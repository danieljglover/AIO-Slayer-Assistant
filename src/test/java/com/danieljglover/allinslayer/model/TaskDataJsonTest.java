package com.danieljglover.allinslayer.model;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TaskDataJsonTest
{
    @Test
    public void roundTripsAllFields()
    {
        TaskData t = new TaskData();
        t.setTask("Nechryael");
        t.setSlayerLevel(80);
        t.setQuestReqs(Collections.singletonList("Priest in Peril"));
        t.setAssignedBy(Arrays.asList("duradel", "nieve"));
        t.setMonsters(Arrays.asList("Nechryael", "Greater nechryael"));
        t.setWeakness(new Weakness(CombatStyle.MAGIC, "air"));
        t.setMonsterDefence(new MonsterDefence(115, 0, 0, 0, 0, 0));
        t.setSlayerHelmApplies(true);
        t.setLocations(Collections.singletonList(
            new SlayerLocation("Catacombs of Kourend", true, false, true, true)));
        t.setRecommendedMethod("Ice Barrage in Catacombs");

        Gson gson = new Gson();
        String json = gson.toJson(t);
        TaskData back = gson.fromJson(json, TaskData.class);

        assertEquals("Nechryael", back.getTask());
        assertEquals(80, back.getSlayerLevel());
        assertEquals(CombatStyle.MAGIC, back.getWeakness().getStyle());
        assertEquals("air", back.getWeakness().getElement());
        assertEquals(115, back.getMonsterDefence().getDefenceLevel());
        assertTrue(back.getLocations().get(0).isBurst());
        assertEquals("Ice Barrage in Catacombs", back.getRecommendedMethod());
    }

    @Test
    public void undeadFlagRoundTripsAndDefaultsFalseWhenAbsent()
    {
        // LFB-1: the new per-task undead flag deserialises when present and defaults to false (Gson
        // boolean default) so the 40 existing entries without the key keep loading as not-undead.
        TaskData undead = new Gson().fromJson("{\"task\":\"Ankou\",\"undead\":true}", TaskData.class);
        assertTrue("explicit undead:true deserialises", undead.isUndead());

        TaskData absent = new Gson().fromJson("{\"task\":\"Bloodveld\"}", TaskData.class);
        assertFalse("undead defaults to false when the key is absent", absent.isUndead());
    }

    @Test
    public void dragonFlagRoundTripsAndDefaultsFalseWhenAbsent()
    {
        // WDB-7: the per-task dragon flag deserialises when present and defaults to false when absent.
        TaskData dragon = new Gson().fromJson("{\"task\":\"Metal dragons\",\"dragon\":true}", TaskData.class);
        assertTrue("explicit dragon:true deserialises", dragon.isDragon());

        TaskData absent = new Gson().fromJson("{\"task\":\"Bloodveld\"}", TaskData.class);
        assertFalse("dragon defaults to false when the key is absent", absent.isDragon());
    }

    @Test
    public void demonAndKalphiteFlagsRoundTripAndDefaultFalse()
    {
        // WDB-9: the per-task demon/kalphite category flags deserialise and default to false.
        TaskData both = new Gson().fromJson(
            "{\"task\":\"Greater demons\",\"demon\":true}", TaskData.class);
        assertTrue("demon:true deserialises", both.isDemon());
        assertFalse("kalphite absent -> false", both.isKalphite());

        TaskData kalphite = new Gson().fromJson(
            "{\"task\":\"Kalphite\",\"kalphite\":true}", TaskData.class);
        assertTrue(kalphite.isKalphite());

        TaskData absent = new Gson().fromJson("{\"task\":\"Bloodveld\"}", TaskData.class);
        assertFalse(absent.isDemon());
        assertFalse(absent.isKalphite());
    }

    @Test
    public void variantsBlockRoundTripsAndDefaultsNullWhenAbsent()
    {
        // MV-B1: a task gains an optional variants array. A task with no variants key loads with
        // variants == null (Gson default) so the existing 42 tasks are byte-identical (FR-6 anchor).
        TaskData absent = new Gson().fromJson("{\"task\":\"Bloodveld\"}", TaskData.class);
        assertNull("variants defaults to null when the key is absent", absent.getVariants());

        String json = "{\"task\":\"Greater demons\",\"variants\":["
            + "{\"name\":\"Greater demon\",\"isDefault\":true,\"demon\":true,\"combatLevel\":92,"
            + "\"weakness\":{\"style\":\"MELEE\",\"element\":null},"
            + "\"monsterDefence\":{\"defenceLevel\":70,\"stab\":0,\"slash\":0,\"crush\":0,\"magic\":0,\"range\":0}},"
            + "{\"name\":\"K'ril Tsutsaroth\",\"isBoss\":true,\"demon\":true,\"npcIds\":[3129],\"bossId\":7,"
            + "\"weakness\":{\"style\":\"MELEE\",\"element\":null},"
            + "\"monsterDefence\":{\"defenceLevel\":240,\"stab\":0,\"slash\":0,\"crush\":0,\"magic\":0,\"range\":0},"
            + "\"location\":\"God Wars Dungeon\",\"requirement\":\"boss - separate trip\"}]}";

        TaskData t = new Gson().fromJson(json, TaskData.class);
        assertEquals(2, t.getVariants().size());

        MonsterVariant base = t.getVariants().get(0);
        assertEquals("Greater demon", base.getName());
        assertTrue("base is the default", base.isDefault());
        assertFalse("base is not a boss", base.isBoss());
        assertTrue("category flag follows the variant", base.isDemon());
        assertEquals(Integer.valueOf(92), base.getCombatLevel());
        assertEquals(CombatStyle.MELEE, base.getWeakness().getStyle());
        assertEquals(70, base.getMonsterDefence().getDefenceLevel());

        MonsterVariant boss = t.getVariants().get(1);
        assertEquals("K'ril Tsutsaroth", boss.getName());
        assertTrue("boss flag round-trips", boss.isBoss());
        assertFalse("boss is not the default", boss.isDefault());
        assertEquals(Integer.valueOf(3129), boss.getNpcIds().get(0));
        assertEquals(Integer.valueOf(7), boss.getBossId());
        assertEquals("God Wars Dungeon", boss.getLocation());
        assertEquals("boss - separate trip", boss.getRequirement());

        // A variant with no weakness/defence loads them as null (UNKNOWN-weakness inherit, ADR-0012.3).
        TaskData inherit = new Gson().fromJson(
            "{\"task\":\"Vampyres\",\"variants\":[{\"name\":\"Vampyre\",\"isDefault\":true}]}",
            TaskData.class);
        assertNull(inherit.getVariants().get(0).getWeakness());
        assertNull(inherit.getVariants().get(0).getMonsterDefence());
    }

    @Test
    public void strategyBlockRoundTripsAndDefaultsNullWhenAbsent()
    {
        // MV-S1 (ADR-0015): a variant gains an optional nested strategy. A variant with no strategy
        // key loads with strategy == null (Gson default) so today's variants are byte-identical
        // (FR-6 / FR-S4). When present, the priority-ordered primary + styled secondary weapons (with
        // authored raw item ids) round-trip.
        TaskData absent = new Gson().fromJson(
            "{\"task\":\"Bloodveld\",\"variants\":[{\"name\":\"Bloodveld\",\"isDefault\":true}]}",
            TaskData.class);
        assertNull("strategy defaults to null when the key is absent",
            absent.getVariants().get(0).getStrategy());

        String json = "{\"task\":\"Demonic\",\"variants\":[{"
            + "\"name\":\"Tormented Demon\",\"isBoss\":true,\"demon\":true,"
            + "\"weakness\":{\"style\":\"MELEE\",\"element\":null},"
            + "\"strategy\":{"
            + "\"primaryStyle\":\"MELEE\","
            + "\"primaryWeapons\":[{\"name\":\"Emberlight\",\"itemId\":28583,\"style\":null},"
            + "{\"name\":\"Arclight\",\"itemId\":19675,\"style\":null}],"
            + "\"secondaryWeapons\":[{\"name\":\"Scorching bow\",\"itemId\":30327,\"style\":\"RANGED\"}],"
            + "\"note\":\"Switch to Scorching bow for the shield-down ranged window\","
            + "\"sourceUrl\":\"https://oldschool.runescape.wiki/w/Tormented_Demon/Strategies\"}}]}";

        TaskData t = new Gson().fromJson(json, TaskData.class);
        MonsterStrategy s = t.getVariants().get(0).getStrategy();
        assertNotNull("strategy block deserialises", s);
        assertEquals(CombatStyle.MELEE, s.getPrimaryStyle());
        assertEquals(2, s.getPrimaryWeapons().size());
        assertEquals("Emberlight", s.getPrimaryWeapons().get(0).getName());
        assertEquals(Integer.valueOf(28583), s.getPrimaryWeapons().get(0).getItemId());
        assertNull("primary weapon style null -> uses primaryStyle",
            s.getPrimaryWeapons().get(0).getStyle());
        assertEquals(1, s.getSecondaryWeapons().size());
        assertEquals(CombatStyle.RANGED, s.getSecondaryWeapons().get(0).getStyle());
        assertEquals(Integer.valueOf(30327), s.getSecondaryWeapons().get(0).getItemId());
        assertTrue(s.getNote().contains("Scorching bow"));
        assertTrue(s.getSourceUrl().contains("/Strategies"));
    }

    @Test
    public void variantLocationNamesRoundTripAndDefaultNullWhenAbsent()
    {
        // DT-B3 (ADR-0017): a variant gains an optional locationNames - a NAMED SUBSET of the task's
        // locations. Gson default null (== empty) is the fallback sentinel meaning "all task locations
        // apply", which keeps today's ~209 variants byte-identical (FR-6). Present -> the named subset
        // round-trips in order.
        TaskData absent = new Gson().fromJson(
            "{\"task\":\"Bloodveld\",\"variants\":[{\"name\":\"Bloodveld\",\"isDefault\":true}]}",
            TaskData.class);
        assertNull("locationNames defaults null when absent (all-task fallback sentinel)",
            absent.getVariants().get(0).getLocationNames());

        String json = "{\"task\":\"Abyssal demons\",\"variants\":[{"
            + "\"name\":\"Abyssal demon\",\"isDefault\":true,"
            + "\"locationNames\":[\"Catacombs of Kourend\",\"Slayer Tower\"]}]}";
        TaskData t = new Gson().fromJson(json, TaskData.class);
        java.util.List<String> names = t.getVariants().get(0).getLocationNames();
        assertEquals(2, names.size());
        assertEquals("Catacombs of Kourend", names.get(0));
        assertEquals("Slayer Tower", names.get(1));
    }

    @Test
    public void slayerLocationRoundTripsSafeSpotAndAccessNoteAndKeepsBackCompatConstructors()
    {
        // DT-B2 (ADR-0017): SlayerLocation gains safeSpot (default false) + accessNote (default null),
        // added additively so the existing 5-arg and 6-arg constructors still compile and the runtime
        // location enrichment (DT-B4) can reach the panel. Gson back-compat: absent keys -> false/null
        // -> today's behaviour.
        Gson gson = new Gson();

        // Explicit values round-trip via the new all-args constructor.
        SlayerLocation full = new SlayerLocation("Catacombs of Kourend", true, false, true, true,
            false, true, "Kourend favour + teleport");
        SlayerLocation fullBack = gson.fromJson(gson.toJson(full), SlayerLocation.class);
        assertTrue("safeSpot round-trips", fullBack.isSafeSpot());
        assertEquals("Kourend favour + teleport", fullBack.getAccessNote());

        // Absent keys default to false/null so older serialised data keeps loading unchanged.
        SlayerLocation absent = gson.fromJson("{\"name\":\"Slayer Tower\",\"multi\":false}",
            SlayerLocation.class);
        assertFalse("safeSpot defaults false when the key is absent", absent.isSafeSpot());
        assertNull("accessNote defaults null when the key is absent", absent.getAccessNote());

        // The pre-existing 6-arg (wilderness) and 5-arg constructors still compile and default the
        // two new fields (the FR-6 back-compat anchor for the ~110 existing call sites).
        SlayerLocation sixArg = new SlayerLocation("Ankou area", true, true, true, true, true);
        assertFalse(sixArg.isSafeSpot());
        assertNull(sixArg.getAccessNote());
        SlayerLocation fiveArg = new SlayerLocation("Stronghold Slayer Cave", true, true, true, true);
        assertFalse(fiveArg.isSafeSpot());
        assertNull(fiveArg.getAccessNote());
    }

    @Test
    public void unlocksExtendedAmountAndWeightByMasterRoundTripAndDefaultNullWhenAbsent()
    {
        // WA-1 (ADR-0018 #3/#4/#5): TaskData gains unlocks / extendedAmount / weightByMaster
        // additively. Absent keys -> null (the FR-6 sentinel: a task without them is byte-identical
        // to today), so the existing 43 tasks keep loading unchanged.
        TaskData absent = new Gson().fromJson("{\"task\":\"Bloodveld\"}", TaskData.class);
        assertNull("unlocks defaults null when absent", absent.getUnlocks());
        assertNull("extendedAmount defaults null when absent", absent.getExtendedAmount());
        assertNull("weightByMaster defaults null when absent", absent.getWeightByMaster());

        String json = "{\"task\":\"Abyssal demons\","
            + "\"extendedAmount\":{\"duradel\":[200,250]},"
            + "\"weightByMaster\":{\"duradel\":12,\"konar\":9},"
            + "\"unlocks\":[{\"unlockId\":\"augment-my-abbies\",\"name\":\"Augment my abbies\","
            + "\"pointsCost\":100,\"type\":\"EXTENSION\",\"notes\":\"Extends the task to 200-250.\"}]}";
        TaskData t = new Gson().fromJson(json, TaskData.class);
        assertArrayEquals(new int[]{200, 250}, t.getExtendedAmount().get("duradel"));
        assertEquals(Integer.valueOf(12), t.getWeightByMaster().get("duradel"));
        assertEquals(Integer.valueOf(9), t.getWeightByMaster().get("konar"));
        TaskUnlock unlock = t.getUnlocks().get(0);
        assertEquals("augment-my-abbies", unlock.getUnlockId());
        assertEquals("Augment my abbies", unlock.getName());
        assertEquals(Integer.valueOf(100), unlock.getPointsCost());
        assertEquals(UnlockType.EXTENSION, unlock.getType());
        assertTrue(unlock.getNotes().contains("200-250"));

        // An untyped unlock (today's authored rows carry no type key) loads with type == null.
        TaskData untyped = new Gson().fromJson(
            "{\"task\":\"Gargoyles\",\"unlocks\":[{\"unlockId\":\"gargoyle-smasher\",\"pointsCost\":120}]}",
            TaskData.class);
        assertNull("type defaults null when absent", untyped.getUnlocks().get(0).getType());

        // Full serialise -> deserialise round-trip preserves all three blocks.
        Gson gson = new Gson();
        TaskData back = gson.fromJson(gson.toJson(t), TaskData.class);
        assertEquals(UnlockType.EXTENSION, back.getUnlocks().get(0).getType());
        assertArrayEquals(new int[]{200, 250}, back.getExtendedAmount().get("duradel"));
        assertEquals(Integer.valueOf(12), back.getWeightByMaster().get("duradel"));
    }

    @Test
    public void ignoresLegacyLoadoutsBlockInJson()
    {
        // Legacy data may still carry a "loadouts" block; with the field removed (LD14) Gson must
        // ignore it rather than fail, so older serialised data keeps loading.
        String legacy = "{\"task\":\"Bloodveld\",\"weakness\":{\"style\":\"MELEE\",\"element\":null},"
            + "\"loadouts\":{\"MELEE\":{\"style\":\"MELEE\",\"slotOptions\":{\"WEAPON\":[4151]},"
            + "\"inventory\":[385],\"spellMaxHit\":null}}}";

        TaskData back = new Gson().fromJson(legacy, TaskData.class);

        assertEquals("Bloodveld", back.getTask());
        assertEquals(CombatStyle.MELEE, back.getWeakness().getStyle());
        assertNull(back.getWeakness().getElement());
    }
}
