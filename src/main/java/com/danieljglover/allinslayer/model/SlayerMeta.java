package com.danieljglover.allinslayer.model;

import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The root of the SECOND generated resource (WA-8, ADR-0018 #1):
 * {@code slayer-meta.json = {masters, rewards}}. Kept separate from {@code slayer-data.json}
 * (still a bare {@code List<TaskData>} array) so the proven task artifact and its
 * determinism/equivalence tests stay byte-shape-untouched.
 */
@Data
@NoArgsConstructor
public class SlayerMeta
{
    private List<MasterData> masters;
    private List<RewardData> rewards;
}
