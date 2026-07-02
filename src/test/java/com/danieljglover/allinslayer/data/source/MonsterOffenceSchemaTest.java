package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.AttackStyle;
import com.danieljglover.allinslayer.model.MonsterOffence;
import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * WD-1 (ADR-0020 #1): the additive {@code offence} domain deserialises on both the monster variant
 * and the task (task-level fallback), and is honestly absent -> null when the JSON omits it (FR-6).
 */
public class MonsterOffenceSchemaTest
{
    private static final Gson GSON = new Gson();

    @Test
    public void variantWithOffenceParsesEveryField()
    {
        String json = "{\"variantId\":\"kril\",\"name\":\"K'ril Tsutsaroth\",\"offence\":{"
            + "\"hitpoints\":255,\"maxHit\":31,\"attackStyles\":[\"MELEE\",\"MAGIC\"],"
            + "\"attackSpeedTicks\":4,\"magicLevel\":200,\"venomous\":true}}";
        SourceMonsterVariant v = GSON.fromJson(json, SourceMonsterVariant.class);
        MonsterOffence o = v.getOffence();
        assertEquals(Integer.valueOf(255), o.getHitpoints());
        assertEquals(Integer.valueOf(31), o.getMaxHit());
        assertEquals(2, o.getAttackStyles().size());
        assertEquals(AttackStyle.MELEE, o.getAttackStyles().get(0));
        assertEquals(AttackStyle.MAGIC, o.getAttackStyles().get(1));
        assertEquals(Integer.valueOf(4), o.getAttackSpeedTicks());
        assertEquals(Integer.valueOf(200), o.getMagicLevel());
        assertFalse(o.isPoisonous());
        assertTrue(o.isVenomous());
    }

    @Test
    public void variantWithoutOffenceIsNull()
    {
        SourceMonsterVariant v = GSON.fromJson(
            "{\"variantId\":\"x\",\"name\":\"X\"}", SourceMonsterVariant.class);
        assertNull("absent offence -> null (FR-6)", v.getOffence());
    }

    @Test
    public void taskCarriesOptionalOffenceFallback()
    {
        SourceTask withOffence = GSON.fromJson(
            "{\"taskId\":\"t\",\"name\":\"T\",\"offence\":{\"maxHit\":12,\"attackStyles\":[\"RANGED\"]}}",
            SourceTask.class);
        assertEquals(Integer.valueOf(12), withOffence.getOffence().getMaxHit());
        assertEquals(AttackStyle.RANGED, withOffence.getOffence().getAttackStyles().get(0));

        SourceTask without = GSON.fromJson("{\"taskId\":\"t\",\"name\":\"T\"}", SourceTask.class);
        assertNull("absent task offence -> null (FR-6)", without.getOffence());
    }
}
