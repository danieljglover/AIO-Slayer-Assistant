package com.danieljglover.allinslayer.loadout.advisor;

import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot.Spellbook;
import com.danieljglover.allinslayer.model.advisor.RecommendationRequest;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult.Choice;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Method;
import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue.Supply;
import com.danieljglover.allinslayer.model.advisor.TripPreparationData;
import com.danieljglover.allinslayer.model.advisor.TripPreparationData.Spell;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Value;

/** Resolves bundled spell guidance into preparations without changing client spell or item state. */
public final class SpellPlanner
{
    private static final Pattern SPACE = Pattern.compile("\\s+");
    private static volatile SpellIndex cachedIndex;
    private final TripPreparationData data;
    private final RecommendationRequest request;
    private final RequirementEvaluator requirements;
    private final SpellIndex index;
    private final Map<String, String> requirementFailures = new LinkedHashMap<>();

    public SpellPlanner(TripPreparationData data, RecommendationRequest request)
    {
        this.data = data;
        this.request = request;
        this.requirements = new RequirementEvaluator(request);
        this.index = index(data);
    }

    @Value
    public static class CastingPlan
    {
        boolean managed;
        List<Supply> supplies;
        List<String> blockers;
        List<String> notes;

        private CastingPlan(boolean managed, List<Supply> supplies, List<String> blockers, List<String> notes)
        {
            this.managed = managed;
            this.supplies = Collections.unmodifiableList(new ArrayList<>(supplies));
            this.blockers = Collections.unmodifiableList(new ArrayList<>(blockers));
            this.notes = Collections.unmodifiableList(new ArrayList<>(notes));
        }
    }

    public CastingPlan plan(Method method, Map<String, Choice> equipment, boolean allowBlighted)
    {
        List<Supply> supplies = new ArrayList<>();
        List<String> blockers = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        Set<Spell> selected = new LinkedHashSet<>();
        String title = normal(method.getId() + " " + method.getName());
        String guidance = normal(method.getSummary() + " " + String.join(" ", method.getGuidance()));
        String documented = title + " " + guidance + " " + normal(String.join(" ", method.getRequirements()));
        boolean magic = "MAGIC".equalsIgnoreCase(method.getStyle());
        boolean freezeRequired = method.getRequirements().stream().anyMatch(SpellPlanner::freezeRequirement);
        List<Spell> named = namedSpells(title).stream().filter(Spell::isOffensive).collect(Collectors.toList());
        Spell primary = named.isEmpty() ? null : named.get(0);
        if (primary == null)
        {
            // Keep the authored baseline unless the method explicitly offers area-spell alternatives.
            for (String line : method.getGuidance())
            {
                if (line.startsWith("Spell preparation baseline:"))
                {
                    List<Spell> baseline = namedSpells(normal(line.split(";", 2)[0]));
                    if (!baseline.isEmpty()) { primary = baseline.get(0); break; }
                }
            }
        }
        Choice weapon = equipment.get("WEAPON");
        String powered = weapon == null ? null : data.getPoweredWeapons().get(weapon.getItemId());
        if (primary == null)
        {
            for (String line : method.getRequirements())
            {
                String text = normal(line);
                if (optional(text)) { continue; }
                List<Spell> fixed = namedSpells(text).stream().filter(Spell::isOffensive)
                    .filter(s -> explicitCastingRequirement(text, s)).collect(Collectors.toList());
                if (!fixed.isEmpty())
                {
                    primary = fixed.size() > 1 && (text.contains(" or ") || text.contains(" / "))
                        ? fixed.stream().filter(s -> problems(s, equipment).isEmpty()).findFirst().orElse(fixed.get(0))
                        : fixed.get(0);
                    break;
                }
            }
        }
        Spell areaAlternative = fundedAreaAlternative(method, title, named, primary, equipment, allowBlighted);
        if (areaAlternative != null)
        {
            primary = areaAlternative;
            notes.add("Selected " + primary.getName() + " from the guide's burst/barrage alternatives: "
                + "the highest Magic level currently usable and funded for " + data.getCombatCasts() + " casts.");
        }
        if (primary != null) { selected.add(primary); }
        else if (magic && powered != null)
        {
            checkCharges(blockers, weapon, powered);
            notes.add("Use " + weapon.getName() + "'s built-in attack. Confirm at least "
                + data.getCombatCasts() + " loaded charges; loose runes are not loaded charges.");
        }
        else if (magic)
        {
            primary = compatibleDefault(method, equipment, documented, allowBlighted);
            if (primary == null)
            {
                blockers.add("No verified offensive spell is usable with the current Magic level, spellbook and required weapon.");
            }
            else
            {
                selected.add(primary);
                notes.add("Spell planning assumption: " + primary.getName()
                    + ". The guide does not fix a single spell; this is a compatible preparation, not a measured damage ranking.");
            }
        }

        // Exact requirements are stronger than incidental mentions of optional guide alternatives.
        for (String line : method.getRequirements())
        {
            String text = normal(line);
            if (optional(text)) { continue; }
            List<Spell> required = namedSpells(text).stream()
                .filter(s -> !s.isOffensive() || powered == null || explicitCastingRequirement(text, s))
                .collect(Collectors.toList());
            if (required.size() > 1 && (text.contains(" or ") || text.contains(" / ")))
            {
                selected.add(required.stream().filter(s -> problems(s, equipment).isEmpty())
                    .findFirst().orElse(required.get(0)));
            }
            else { selected.addAll(required); }
        }
        for (String line : method.getGuidance())
        {
            String text = normal(line);
            if (text.startsWith("spell preparation baseline") || optional(text)) { continue; }
            for (Spell spell : namedSpells(text))
            {
                if (!spell.isOffensive() && !"PVP_ONLY".equals(spell.getFamily())
                    && (text.contains("required") || text.contains("must ") || text.contains("cast ")
                        || text.contains("use ") || "Mark of Darkness".equals(spell.getName())))
                {
                    selected.add(spell);
                }
            }
        }
        // A powered/ranged freeze method still needs its documented binding spell, independently
        // of the damage weapon. An explicit named spell above retains its exact requirements.
        boolean hasFreeze = selected.stream().anyMatch(s -> "ICE".equals(s.getFamily()) || "BIND".equals(s.getFamily()));
        if (!hasFreeze && (freezeRequired || primary == null && (documented.contains("freez") || documented.contains("entangle"))))
        {
            List<Spell> freezes = namedSpells(documented).stream()
                .filter(s -> "ICE".equals(s.getFamily()) || "BIND".equals(s.getFamily()))
                .sorted(Comparator.comparingInt(Spell::getMagicLevel).reversed()).collect(Collectors.toList());
            Spell freeze = freezes.stream().filter(s -> problems(s, equipment).isEmpty()).findFirst().orElse(null);
            if (freeze == null && freezeRequired)
            {
                freeze = data.getSpells().values().stream()
                    .filter(s -> "ICE".equals(s.getFamily()) || "BIND".equals(s.getFamily()))
                    .filter(s -> problems(s, equipment).isEmpty())
                    .max(Comparator.comparingInt(Spell::getMagicLevel)).orElse(null);
            }
            if (freeze != null) { selected.add(freeze); }
            else if (!freezes.isEmpty()) { selected.add(freezes.get(0)); }
            else if (freezeRequired) { blockers.add("No verified freezing or binding spell is usable with the current Magic level and spellbook."); }
        }
        boolean managed = magic || freezeRequired || !selected.isEmpty();
        Map<Integer, Integer> remaining = new LinkedHashMap<>(request.getPlayer().getOwned());
        for (Spell spell : selected)
        {
            List<String> issues = problems(spell, equipment);
            if (!issues.isEmpty()) { blockers.addAll(issues); continue; }
            if ("IBAN".equals(spell.getFamily()) && weapon != null)
            {
                if (weapon.getItemId() == 1409 && data.getCombatCasts() > 120)
                {
                    blockers.add("Iban's regular staff holds 120 NPC casts; the " + data.getCombatCasts()
                        + "-cast target requires an upgraded staff or a smaller target.");
                    continue;
                }
                String chargeCheck = "Iban's staff charges prepared for this trip";
                checkCharges(blockers, weapon, chargeCheck);
            }
            if ("SLAYER_DART".equals(spell.getFamily()) && weapon != null && weapon.getItemId() == 21255)
            {
                String chargeCheck = "Slayer's staff enchantment charges prepared for this trip";
                checkCharges(blockers, weapon, chargeCheck);
            }
            List<Supply> next = suppliesFor(spell, data.getCombatCasts(), equipment, allowBlighted, remaining);
            supplies.addAll(next);
            for (Supply supply : next)
            {
                remaining.computeIfPresent(supply.getItemIds().get(0), (id, quantity) -> Math.max(0, quantity - supply.getQuantity()));
            }
            notes.add("Pack up to " + data.getCombatCasts() + " casts of " + spell.getName()
                + (spell.isOffensive() ? "." : " as a conservative support-spell allowance.")
                + " Rune-saving chance is not assumed.");
        }
        if (managed)
        {
            notes.add("Spellbook and requirements use current account state. Rune pouch packing, loading charges and choosing autocast/manual casting are preparation steps; this plugin does not perform them.");
        }
        return new CastingPlan(managed, aggregate(supplies), distinct(blockers), notes);
    }

    public List<String> problems(Spell spell, Map<String, Choice> equipment)
    {
        List<String> result = new ArrayList<>();
        if (spell == null) { return Collections.singletonList("Spell has no reviewed preparation data."); }
        String magic = "Magic " + spell.getMagicLevel();
        addFailure(result, magic);
        try
        {
            Spellbook required = Spellbook.valueOf(spell.getSpellbook().toUpperCase(Locale.ROOT));
            addFailure(result, required.getDisplayName() + " spellbook selected");
        }
        catch (IllegalArgumentException ex) { result.add("Spellbook is unverified for " + spell.getName() + "."); }
        for (String requirement : spell.getRequirements())
        {
            addFailure(result, requirement);
        }
        if (!spell.getWeaponIds().isEmpty())
        {
            Choice weapon = equipment.get("WEAPON");
            if (weapon == null || weapon.getMissing() > 0 || !spell.getWeaponIds().contains(weapon.getItemId()))
            {
                result.add(spell.getName() + " requires a compatible equipped casting weapon.");
            }
        }
        return distinct(result);
    }

    public List<Supply> suppliesFor(Spell spell, int casts, Map<String, Choice> equipment, boolean allowBlighted)
    {
        return suppliesFor(spell, casts, equipment, allowBlighted, request.getPlayer().getOwned());
    }

    public List<Supply> suppliesFor(Spell spell, int casts, Map<String, Choice> equipment, boolean allowBlighted,
        Map<Integer, Integer> available)
    {
        if (casts <= 0 || !problems(spell, equipment).isEmpty()) { return Collections.emptyList(); }
        if (allowBlighted && spell.getSackItemId() > 0
            && available.getOrDefault(spell.getSackItemId(), 0) >= casts)
        {
            return Collections.singletonList(supply(spell.getSackItemId(), sackName(spell.getSackItemId()), casts));
        }
        Map<String, Integer> perCast = new LinkedHashMap<>(spell.getRunes());
        for (Choice choice : equipment.values())
        {
            if (choice.getMissing() == 0 && choice.getQuantity() > 0)
            {
                for (String rune : data.getInfiniteRunes().getOrDefault(choice.getItemId(), Collections.emptyList()))
                {
                    perCast.remove(rune);
                }
            }
        }
        List<Supply> supplies = new ArrayList<>();
        // A combination rune can supply both components of the SAME cast. Work per cast, then
        // multiply, so Blood/Ice or offence/teleport demand cannot spend the same unit twice.
        while (true)
        {
            Integer selected = null;
            int selectedQuantity = 0;
            for (Map.Entry<Integer, List<String>> combo : data.getCombinationRunes().entrySet())
            {
                if (combo.getValue().size() != 2 || !perCast.keySet().containsAll(combo.getValue())) { continue; }
                int quantity = multiply(Math.max(perCast.get(combo.getValue().get(0)), perCast.get(combo.getValue().get(1))), casts);
                if (available.getOrDefault(combo.getKey(), 0) < quantity) { continue; }
                if (selected == null || quantity < selectedQuantity)
                {
                    selected = combo.getKey(); selectedQuantity = quantity;
                }
            }
            if (selected == null) { break; }
            supplies.add(supply(selected, runeName(selected), selectedQuantity));
            data.getCombinationRunes().get(selected).forEach(perCast::remove);
        }
        perCast.forEach((name, quantity) ->
        {
            Integer itemId = data.getRuneIds().get(name);
            int total = multiply(quantity, casts);
            if (itemId != null)
            {
                // A one-component substitution does not save a slot, but can fund a spell
                // when the owned stock contains combination runes instead of the basic rune.
                if (available.getOrDefault(itemId, 0) < total)
                {
                    for (Map.Entry<Integer, List<String>> combo : data.getCombinationRunes().entrySet())
                    {
                        int reserved = supplies.stream().filter(s -> s.getItemIds().contains(combo.getKey()))
                            .mapToInt(Supply::getQuantity).sum();
                        if (combo.getValue().contains(name)
                            && (long) available.getOrDefault(combo.getKey(), 0) - reserved >= total)
                        {
                            supplies.add(supply(combo.getKey(), runeName(combo.getKey()), total));
                            return;
                        }
                    }
                }
                supplies.add(supply(itemId, name, total));
            }
        });
        return aggregate(supplies);
    }

    private Spell compatibleDefault(Method method, Map<String, Choice> equipment, String documented, boolean allowBlighted)
    {
        Set<String> families = new LinkedHashSet<>();
        for (String family : new String[] {"WIND", "WATER", "EARTH", "FIRE", "ICE", "BLOOD", "SMOKE", "SHADOW", "DEMONBANE", "GRASP"})
        {
            if (Pattern.compile("\\b" + family.toLowerCase(Locale.ROOT) + "\\s+(?:spells?|magic|barrage|burst|surge|wave|blitz|rush)\\b").matcher(documented).find()
                || family.equals("DEMONBANE") && documented.contains("demonbane")) { families.add(family); }
        }
        Spellbook book = request.getPlayer().getSpellbook();
        if (families.isEmpty())
        {
            if (book == Spellbook.STANDARD) { families.addAll(java.util.Arrays.asList("WIND", "WATER", "EARTH", "FIRE")); }
            else if (book == Spellbook.ANCIENT) { families.add("ICE"); }
            else if (book == Spellbook.ARCEUUS) { families.add("GRASP"); }
        }
        List<Spell> candidates = data.getSpells().values().stream().filter(Spell::isOffensive)
            .filter(s -> families.contains(s.getFamily()))
            .filter(s -> !"ANCIENT".equals(s.getSpellbook()) || method.isBarrage()
                || !s.getName().endsWith("Barrage") && !s.getName().endsWith("Burst"))
            .filter(s -> problems(s, equipment).isEmpty())
            .sorted(Comparator.comparingInt(Spell::getMagicLevel).reversed()).collect(Collectors.toList());
        return candidates.stream().filter(s -> suppliesFor(s, data.getCombatCasts(), equipment, allowBlighted).stream()
                .allMatch(supply -> request.getPlayer().getOwned().getOrDefault(supply.getItemIds().get(0), 0) >= supply.getQuantity()))
            .findFirst().orElse(candidates.isEmpty() ? null : candidates.get(0));
    }

    private Spell fundedAreaAlternative(Method method, String title, List<Spell> named, Spell baseline,
        Map<String, Choice> equipment, boolean allowBlighted)
    {
        if (!"MAGIC".equalsIgnoreCase(method.getStyle()) || baseline == null
            || !title.contains("burst") || !title.contains("barrage") || named.size() == 1)
        {
            return null;
        }
        // A fixed spell requirement remains binding, even if another section mentions alternatives.
        if (method.getRequirements().stream().filter(line -> !optional(normal(line)))
            .anyMatch(line -> namedSpells(normal(line)).stream().anyMatch(Spell::isOffensive)))
        {
            return null;
        }
        return data.getSpells().values().stream()
            .filter(Spell::isOffensive)
            .filter(spell -> "ANCIENT".equals(spell.getSpellbook()))
            .filter(spell -> spell.getName().endsWith("Burst") || spell.getName().endsWith("Barrage"))
            .filter(spell -> named.isEmpty() ? baseline.getFamily().equals(spell.getFamily()) : named.contains(spell))
            .filter(spell -> problems(spell, equipment).isEmpty())
            .sorted(Comparator.comparingInt(Spell::getMagicLevel).reversed())
            .filter(spell -> suppliesFor(spell, data.getCombatCasts(), equipment, allowBlighted).stream()
                .allMatch(supply -> request.getPlayer().getOwned().getOrDefault(supply.getItemIds().get(0), 0)
                    >= supply.getQuantity()))
            .findFirst().orElse(null);
    }

    private List<Spell> namedSpells(String text)
    {
        List<Spell> result = new ArrayList<>();
        for (IndexedSpell indexed : index.spells)
        {
            Spell spell = indexed.spell;
            String name = indexed.name;
            if (!text.contains(name)) { continue; }
            if ("charge".equals(name) && !text.contains("charge spell") && !text.equals("charge")) { continue; }
            Matcher matcher = indexed.pattern.matcher(text);
            if (matcher.find()) { result.add(spell); }
        }
        result.sort(Comparator.comparingInt(s -> text.indexOf(normal(s.getName()))));
        return result;
    }

    private static String normal(String value)
    {
        return value == null ? "" : SPACE.matcher(value.toLowerCase(Locale.ROOT).replace('-', ' ').replace('_', ' ')).replaceAll(" ").trim();
    }

    private static boolean optional(String text)
    {
        return text.contains("optional") || text.contains("consider ") || text.contains("may ")
            || text.contains("can ") || text.contains("alternatively") || text.contains("for example");
    }

    private static boolean explicitCastingRequirement(String text, Spell spell)
    {
        String name = normal(spell.getName());
        return text.equals(name) || text.startsWith(name + " selected") || text.startsWith(name + " required")
            || text.startsWith("cast " + name) || text.startsWith("casting " + name)
            || text.equals("arceuus spellbook with dark demonbane and mark of darkness selected");
    }

    private void checkCharges(List<String> blockers, Choice weapon, String requirement)
    {
        com.danieljglover.allinslayer.model.advisor.ChargeObservation observed = request.getPlayer()
            .getPreparation().getCharges().get(weapon.getItemId());
        if (observed == null || !observed.isObserved())
        {
            addFailure(blockers, requirement + " (at least " + data.getCombatCasts() + " charges)");
        }
        else if (observed.getCharges() < data.getCombatCasts())
        {
            blockers.add(weapon.getName() + " has " + observed.getCharges() + " observed charges; needs at least "
                + data.getCombatCasts() + " for this trip.");
        }
    }

    private void addFailure(List<String> target, String requirement)
    {
        if (!requirementFailures.containsKey(requirement))
        {
            requirementFailures.put(requirement, requirements.failure(requirement));
        }
        String failure = requirementFailures.get(requirement);
        if (failure != null) { target.add(failure); }
    }

    /** These exact combined preparations are checked component-by-component by this planner. */
    public boolean managesRequirement(String requirement)
    {
        return freezeRequirement(requirement)
            || "Arceuus spellbook with Dark Demonbane and Mark of Darkness selected".equals(requirement)
            || "Magic Dart selected with sufficient mind and death runes".equals(requirement)
            || "Magic setup prepared: compatible spellbook and sufficient runes, or a charged powered staff".equals(requirement);
    }

    private static boolean freezeRequirement(String requirement)
    {
        return "Freeze spell and sufficient runes prepared for Callisto".equals(requirement)
            || "Freeze spell and sufficient runes prepared for Artio".equals(requirement)
            || "Guardian freeze spell and sufficient runes prepared for Scorpia".equals(requirement);
    }

    public boolean hasDocumentedBarrageSpell(Method method)
    {
        String source = normal(method.getId() + " " + method.getName());
        for (String line : method.getGuidance())
        {
            if (line.startsWith("Spell preparation baseline:")) { source += " " + normal(line.split(";", 2)[0]); }
        }
        return namedSpells(source).stream().anyMatch(s -> "ANCIENT".equals(s.getSpellbook())
            && (s.getName().endsWith("Barrage") || s.getName().endsWith("Burst")) && !s.getRunes().isEmpty());
    }

    private static SpellIndex index(TripPreparationData data)
    {
        SpellIndex existing = cachedIndex;
        if (existing != null && existing.data == data) { return existing; }
        synchronized (SpellPlanner.class)
        {
            if (cachedIndex == null || cachedIndex.data != data) { cachedIndex = new SpellIndex(data); }
            return cachedIndex;
        }
    }

    private static final class SpellIndex
    {
        private final TripPreparationData data;
        private final List<IndexedSpell> spells = new ArrayList<>();

        private SpellIndex(TripPreparationData data)
        {
            this.data = data;
            data.getSpells().values().forEach(spell -> spells.add(new IndexedSpell(spell)));
        }
    }

    private static final class IndexedSpell
    {
        private final Spell spell;
        private final String name;
        private final Pattern pattern;

        private IndexedSpell(Spell spell)
        {
            this.spell = spell;
            this.name = normal(spell.getName());
            this.pattern = Pattern.compile("(?<![a-z])" + Pattern.quote(name) + "(?![a-z])");
        }
    }

    private static int multiply(int quantity, int casts)
    {
        return (int) Math.min(Integer.MAX_VALUE, (long) quantity * casts);
    }

    private static Supply supply(int id, String name, int quantity)
    {
        Supply result = new Supply();
        result.setName(name); result.setItemIds(Collections.singletonList(id));
        result.setQuantity(quantity); result.setRequired(true); result.setStackable(true);
        return result;
    }

    private String runeName(int id)
    {
        return data.getRuneIds().entrySet().stream().filter(e -> e.getValue() == id)
            .map(Map.Entry::getKey).findFirst().orElse("Rune " + id);
    }

    private static String sackName(int id)
    {
        if (id == 24607) { return "Blighted ancient ice sack"; }
        if (id == 26705) { return "Blighted surge sack"; }
        if (id == 24613) { return "Blighted entangle sack"; }
        if (id == 24621) { return "Blighted vengeance sack"; }
        return "Spell sack " + id;
    }

    private static List<Supply> aggregate(List<Supply> input)
    {
        Map<Integer, Supply> result = new LinkedHashMap<>();
        for (Supply supply : input)
        {
            int id = supply.getItemIds().get(0);
            Supply previous = result.get(id);
            if (previous == null) { result.put(id, supply); }
            else { previous.setQuantity((int) Math.min(Integer.MAX_VALUE, (long) previous.getQuantity() + supply.getQuantity())); }
        }
        return new ArrayList<>(result.values());
    }

    private static List<String> distinct(List<String> input)
    {
        return new ArrayList<>(new LinkedHashSet<>(input));
    }
}
