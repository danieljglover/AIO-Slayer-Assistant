package com.danieljglover.allinslayer.task;

import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.events.ConfigChanged;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TaskRefreshTriggerTest
{
    @Test
    public void refreshesForEverySlayerTaskStateVarChange()
    {
        assertTrue(TaskRefreshTrigger.isTaskStateChange(varp(SlayerVarbits.SLAYER_TARGET)));
        assertTrue(TaskRefreshTrigger.isTaskStateChange(varp(SlayerVarbits.SLAYER_COUNT)));
        assertTrue(TaskRefreshTrigger.isTaskStateChange(varp(SlayerVarbits.SLAYER_AREA)));
        assertTrue(TaskRefreshTrigger.isTaskStateChange(varp(SlayerVarbits.SLAYER_COUNT_ORIGINAL)));
        assertTrue(TaskRefreshTrigger.isTaskStateChange(varbit(SlayerVarbits.SLAYER_TARGET_BOSSID)));

        assertFalse(TaskRefreshTrigger.isTaskStateChange(varp(12345)));
        assertFalse(TaskRefreshTrigger.isTaskStateChange(varbit(12345)));
    }

    @Test
    public void refreshesForTaskAssignmentAndProgressGameMessages()
    {
        assertTrue(TaskRefreshTrigger.isSlayerTaskMessage(ChatMessageType.GAMEMESSAGE,
            "You're assigned to kill 152 abyssal demons; only 84 more to go."));
        assertTrue(TaskRefreshTrigger.isSlayerTaskMessage(ChatMessageType.GAMEMESSAGE,
            "You are currently assigned to kill 152 abyssal demons; only 84 more to go."));
        assertTrue(TaskRefreshTrigger.isSlayerTaskMessage(ChatMessageType.GAMEMESSAGE,
            "Your new task is to kill 152 abyssal demons."));
        assertTrue(TaskRefreshTrigger.isSlayerTaskMessage(ChatMessageType.SPAM,
            "<col=ef1020>You're assigned to kill 152 abyssal demons; only 84 more to go.</col>"));
        assertTrue(TaskRefreshTrigger.isSlayerTaskMessage(ChatMessageType.DIALOG,
            "You're assigned to kill 152 abyssal demons; only 84 more to go."));
        assertTrue(TaskRefreshTrigger.isSlayerTaskMessage(ChatMessageType.NPC_SAY,
            "You're assigned to kill 152 abyssal demons; only 84 more to go."));
        assertTrue(TaskRefreshTrigger.isSlayerTaskMessage(ChatMessageType.GAMEMESSAGE,
            "You've completed 341 tasks and received 15 points; return to a Slayer master."));
        assertTrue(TaskRefreshTrigger.isSlayerTaskMessage(ChatMessageType.GAMEMESSAGE,
            "You need something new to hunt. Return to a Slayer master."));
    }

    @Test
    public void ignoresNonSlayerAndNonGameMessages()
    {
        assertFalse(TaskRefreshTrigger.isSlayerTaskMessage(ChatMessageType.PUBLICCHAT,
            "You're assigned to kill 152 abyssal demons; only 84 more to go."));
        assertFalse(TaskRefreshTrigger.isSlayerTaskMessage(ChatMessageType.GAMEMESSAGE,
            "You have 84 charges remaining."));
        assertFalse(TaskRefreshTrigger.isSlayerTaskMessage(ChatMessageType.GAMEMESSAGE, null));
    }

    @Test
    public void refreshesWhenCheckingSlayerTaskItems()
    {
        assertTrue(TaskRefreshTrigger.isSlayerTaskCheckMenuAction(MenuAction.CC_OP, "Check", 4155));
        assertTrue(TaskRefreshTrigger.isSlayerTaskCheckMenuAction(MenuAction.CC_OP, "Check", 11864));
        assertTrue(TaskRefreshTrigger.isSlayerTaskCheckMenuAction(MenuAction.CC_OP_LOW_PRIORITY, "Check", 11866));

        assertFalse(TaskRefreshTrigger.isSlayerTaskCheckMenuAction(MenuAction.CC_OP, "Wield", 11864));
        assertFalse(TaskRefreshTrigger.isSlayerTaskCheckMenuAction(MenuAction.ITEM_FIRST_OPTION, "Check", 11864));
        assertFalse(TaskRefreshTrigger.isSlayerTaskCheckMenuAction(MenuAction.CC_OP, "Check", 12345));
    }

    @Test
    public void rebuildsOnlyOnLoggedInGameState()
    {
        assertTrue(TaskRefreshTrigger.isLoggedInGameState(gameState(GameState.LOGGED_IN)));

        assertFalse(TaskRefreshTrigger.isLoggedInGameState(gameState(GameState.LOGIN_SCREEN)));
        assertFalse(TaskRefreshTrigger.isLoggedInGameState(gameState(GameState.LOADING)));
        assertFalse(TaskRefreshTrigger.isLoggedInGameState(null));
    }

    @Test
    public void recomputesOnlyForOwnConfigGroup()
    {
        assertTrue(TaskRefreshTrigger.isConfigGroupChange(configChanged("allinslayer"), "allinslayer"));

        assertFalse(TaskRefreshTrigger.isConfigGroupChange(configChanged("otherplugin"), "allinslayer"));
        assertFalse(TaskRefreshTrigger.isConfigGroupChange(configChanged(null), "allinslayer"));
        assertFalse(TaskRefreshTrigger.isConfigGroupChange(null, "allinslayer"));
    }

    private static GameStateChanged gameState(GameState state)
    {
        GameStateChanged event = new GameStateChanged();
        event.setGameState(state);
        return event;
    }

    private static ConfigChanged configChanged(String group)
    {
        ConfigChanged event = new ConfigChanged();
        event.setGroup(group);
        return event;
    }

    private static VarbitChanged varp(int id)
    {
        VarbitChanged event = new VarbitChanged();
        event.setVarpId(id);
        return event;
    }

    private static VarbitChanged varbit(int id)
    {
        VarbitChanged event = new VarbitChanged();
        event.setVarbitId(id);
        return event;
    }
}
