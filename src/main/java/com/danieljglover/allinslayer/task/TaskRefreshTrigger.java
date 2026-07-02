package com.danieljglover.allinslayer.task;

import java.util.Locale;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.util.Text;

public final class TaskRefreshTrigger
{
    private TaskRefreshTrigger() {}

    public static boolean isTaskStateChange(VarbitChanged event)
    {
        int varpId = event.getVarpId();
        int varbitId = event.getVarbitId();
        return varpId == SlayerVarbits.SLAYER_TARGET
            || varpId == SlayerVarbits.SLAYER_COUNT
            || varpId == SlayerVarbits.SLAYER_AREA
            || varpId == SlayerVarbits.SLAYER_COUNT_ORIGINAL
            || varbitId == SlayerVarbits.SLAYER_TARGET_BOSSID;
    }

    public static boolean isSlayerTaskMessage(ChatMessageType type, String message)
    {
        if (message == null || !isTaskMessageType(type))
        {
            return false;
        }

        String text = Text.removeTags(message).toLowerCase(Locale.ROOT);
        return text.contains("assigned to kill")
            || text.contains("new task is to kill")
            || text.contains("only ") && text.contains(" more to go")
            || text.contains("completed ") && text.contains(" tasks") && text.contains("slayer master")
            || text.contains("you need something new to hunt")
            || text.contains("return to a slayer master");
    }

    public static boolean isSlayerTaskCheckMenuAction(MenuAction action, String menuOption, int mappedItemId)
    {
        return "Check".equals(menuOption)
            && (action == MenuAction.CC_OP || action == MenuAction.CC_OP_LOW_PRIORITY)
            && (mappedItemId == 4155 || mappedItemId == 11864 || mappedItemId == 11866);
    }

    /**
     * Only a transition into {@link GameState#LOGGED_IN} should rebuild the panel; login/loading
     * blips otherwise churn the render (ADR-0002, audit Finding 9).
     */
    public static boolean isLoggedInGameState(GameStateChanged event)
    {
        return event != null && event.getGameState() == GameState.LOGGED_IN;
    }

    /** A config change belongs to this plugin (so a developer-mode toggle is picked up live). */
    public static boolean isConfigGroupChange(ConfigChanged event, String configGroup)
    {
        return event != null && configGroup != null && configGroup.equals(event.getGroup());
    }

    private static boolean isTaskMessageType(ChatMessageType type)
    {
        return type == ChatMessageType.GAMEMESSAGE
            || type == ChatMessageType.SPAM
            || type == ChatMessageType.DIALOG
            || type == ChatMessageType.MESBOX
            || type == ChatMessageType.NPC_SAY;
    }
}
