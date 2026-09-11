package com.danieljglover.allinslayer.data.source;

import com.danieljglover.allinslayer.model.UnlockType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SourceTaskUnlock
{
    private String unlockId;
    private String name;
    private Integer pointsCost;
    // WA-3 (ADR-0018 #5): the machine-usable unlock->effect link. Gson defaults an absent (or
    // unrecognised) value to null = unclassified; WA-5 tags the EXTENSION unlock on every task
    // that has an extendedAmount, and WA-7 cross-validates the pairing at build time.
    private UnlockType type;
    private String notes;
}
