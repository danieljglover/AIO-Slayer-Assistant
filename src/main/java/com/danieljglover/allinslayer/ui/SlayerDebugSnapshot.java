package com.danieljglover.allinslayer.ui;

import lombok.Value;
import net.runelite.api.MenuAction;

@Value
public class SlayerDebugSnapshot
{
    String menuOption;
    MenuAction menuAction;
    int rawItemId;
    int mappedItemId;
    boolean matchedTaskCheck;
    int slayerTargetVarp;
    int slayerCountVarp;
    int slayerAreaVarp;
    int bossTargetVarbit;
    String detectorResult;
    int unsupportedTargetId;

    public static SlayerDebugSnapshot empty()
    {
        return new SlayerDebugSnapshot("", null, -1, -1, false, -1, -1, -1, -1, "", -1);
    }

    public SlayerDebugSnapshot withMenu(String menuOption, MenuAction menuAction, int rawItemId,
        int mappedItemId, boolean matchedTaskCheck)
    {
        return new SlayerDebugSnapshot(
            menuOption == null ? "" : menuOption,
            menuAction,
            rawItemId,
            mappedItemId,
            matchedTaskCheck,
            slayerTargetVarp,
            slayerCountVarp,
            slayerAreaVarp,
            bossTargetVarbit,
            detectorResult,
            unsupportedTargetId);
    }

    public SlayerDebugSnapshot withSlayerState(int slayerTargetVarp, int slayerCountVarp,
        int slayerAreaVarp, int bossTargetVarbit, String detectorResult, int unsupportedTargetId)
    {
        return new SlayerDebugSnapshot(
            menuOption,
            menuAction,
            rawItemId,
            mappedItemId,
            matchedTaskCheck,
            slayerTargetVarp,
            slayerCountVarp,
            slayerAreaVarp,
            bossTargetVarbit,
            detectorResult == null ? "" : detectorResult,
            unsupportedTargetId);
    }
}
