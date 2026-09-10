package com.danieljglover.allinslayer.integration.advisor;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/** Reads existing POH routing assumptions without loading or configuring Shortest Path. */
@Singleton
public final class ShortestPathHouseSettings
{
    private final ConfigManager config;

    @Inject
    public ShortestPathHouseSettings(ConfigManager config) { this.config = config; }

    public Map<String, String> capture()
    {
        Map<String, String> requirements = new LinkedHashMap<>();
        if (!"true".equals(config.getConfiguration("shortestpath", "usePoh"))) { return requirements; }
        // Read saved values, not an interface whose defaults include an ornate box.
        // These remain configuration assumptions, never game-observed furniture.
        String tier = config.getConfiguration("shortestpath", "pohJewelleryBoxTier");
        if ("BASIC".equals(tier) || "FANCY".equals(tier) || "ORNATE".equals(tier))
        {
            requirements.put("POH has a jewellery box", "Shortest Path: "
                + tier.toLowerCase(java.util.Locale.ROOT) + " jewellery box configured.");
        }
        if ("ORNATE".equals(tier))
        {
            requirements.put("POH has an Edgeville teleport (mounted glory or ornate jewellery box)",
                "Shortest Path: ornate jewellery box configured.");
        }
        else if ("true".equals(config.getConfiguration("shortestpath", "usePohMountedItems")))
        {
            requirements.put("POH has an Edgeville teleport (mounted glory or ornate jewellery box)",
                "Shortest Path: mounted-item routes enabled, including mounted glory. Confirm this matches your house.");
        }
        return requirements;
    }
}
