package com.danieljglover.allinslayer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Manual-testing launcher for {@code ./gradlew run}: boots RuneLite with the plugin loaded.
 * Lives in the dev source set - this project has no automated tests by design; all verification
 * is manual through this launcher.
 */
public class AllInSlayerPluginLauncher
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(AllInSlayerPlugin.class);
        RuneLite.main(args);
    }
}
