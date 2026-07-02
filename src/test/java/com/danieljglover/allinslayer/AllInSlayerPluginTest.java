package com.danieljglover.allinslayer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AllInSlayerPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(AllInSlayerPlugin.class);
        RuneLite.main(args);
    }
}
