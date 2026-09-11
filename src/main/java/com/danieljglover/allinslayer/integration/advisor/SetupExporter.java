package com.danieljglover.allinslayer.integration.advisor;

import com.danieljglover.allinslayer.model.advisor.PlayerSnapshot;
import com.danieljglover.allinslayer.model.advisor.RecommendationResult;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

/** Inventory Setups' portable JSON format; copying/importing is always an explicit user action. */
public final class SetupExporter
{
    private static final List<String> EQUIPMENT = Arrays.asList("HEAD", "CAPE", "AMULET", "WEAPON",
        "BODY", "SHIELD", "ARMS", "LEGS", "HAIR", "HANDS", "FEET", "JAW", "RING", "AMMO");
    private final Gson gson;

    @Inject
    public SetupExporter(Gson gson)
    {
        this.gson = gson;
    }

    public String export(RecommendationResult.Setup recommendation, PlayerSnapshot player)
    {
        return export(recommendation, player, false);
    }

    public String export(RecommendationResult.Setup recommendation, PlayerSnapshot player, boolean taskPreview)
    {
        JsonObject setup = new JsonObject();
        String title = "Slayer: " + recommendation.getTitle();
        setup.addProperty("name", title.substring(0, Math.min(50, title.length())));
        String notes = recommendation.getSummary();
        if (taskPreview)
        {
            notes = "ON-TASK CATALOGUE PREVIEW: assumes the selected Slayer assignment for bonuses and task-only access. "
                + "Your detected assignment is unchanged.\n" + notes;
        }
        if (recommendation.getWildernessRisk() != null && recommendation.getWildernessRisk().getPlannedEtherCharges() > 0)
        {
            int charges = recommendation.getWildernessRisk().getPlannedEtherCharges();
            notes += "\nPlanned loss assumes " + charges + " usable charges (" + (charges + 1000)
                + " total ether) in each Wilderness weapon. This exported target does not verify loaded charges; more ether increases actual loss.";
        }
        RecommendationResult.Choice head = recommendation.getEquipment().get("HEAD");
        if (head != null && !head.getExplanation().isEmpty()) notes += "\n" + head.getExplanation();
        List<RecommendationResult.Choice> pouch = recommendation.getInventory().stream()
            .filter(SetupExporter::isPouchContent).collect(Collectors.toList());
        if (!pouch.isEmpty())
        {
            notes += "\nRune pouch configuration (exact amounts):";
            for (RecommendationResult.Choice rune : pouch)
            {
                notes += "\n" + rune.getName() + " x" + rune.getQuantity();
            }
            notes += "\nLoad these exact amounts; unload extra runes. Pouch contents are separate from the loose inventory below.";
        }
        List<String> preparation = recommendation.getExplanations().stream()
            .filter(note -> note.startsWith("Return:") || note.startsWith("Escape:")
                || note.startsWith("Looting bag:") || note.startsWith("Rune pouch:") || note.startsWith("Casting:")
                || note.startsWith("Boosts:") || note.startsWith("Food:"))
            .distinct().collect(Collectors.toList());
        for (RecommendationResult.Choice choice : recommendation.getInventory())
        {
            String explanation = choice.getExplanation();
            if (!explanation.isEmpty() && !preparation.contains(explanation)) { preparation.add(explanation); }
        }
        if (!preparation.isEmpty()) { notes += "\nTrip preparation:\n" + String.join("\n", preparation); }
        setup.addProperty("notes", notes + "\n" + String.join("\n", recommendation.getGuidance()));
        setup.addProperty("sb", 4); // No spellbook specified: do not falsely export Standard magic.
        JsonArray equipment = new JsonArray();
        for (String slot : EQUIPMENT)
        {
            RecommendationResult.Choice choice = recommendation.getEquipment().get(slot);
            equipment.add(choice == null || choice.getItemId() <= 0 ? JsonNull.INSTANCE
                : item(choice.getItemId(), choice.getQuantity()));
        }
        setup.add("eq", equipment);
        JsonArray inventory = new JsonArray();
        for (RecommendationResult.Choice choice : recommendation.getInventory())
        {
            if (isPouchContent(choice) || choice.getItemId() <= 0 || choice.getQuantity() <= 0)
            {
                continue;
            }
            PlayerSnapshot.ItemStats stats = player.getItems().get(choice.getItemId());
            boolean stackable = stats != null && stats.isStackable();
            int copies = stackable ? 1 : choice.getQuantity();
            for (int i = 0; i < copies && inventory.size() < 28; i++)
            {
                inventory.add(item(choice.getItemId(), stackable ? choice.getQuantity() : 1));
            }
        }
        while (inventory.size() < 28)
        {
            inventory.add(JsonNull.INSTANCE);
        }
        setup.add("inv", inventory);
        JsonObject portable = new JsonObject();
        portable.add("setup", setup);
        portable.add("layout", new JsonArray());
        return gson.toJson(portable);
    }

    private static boolean isPouchContent(RecommendationResult.Choice choice)
    {
        return "RUNE POUCH".equals(choice.getSlot());
    }

    private JsonObject item(int id, int quantity)
    {
        JsonObject value = new JsonObject();
        value.addProperty("id", id);
        if (quantity != 1)
        {
            value.addProperty("q", quantity);
        }
        return value;
    }
}
