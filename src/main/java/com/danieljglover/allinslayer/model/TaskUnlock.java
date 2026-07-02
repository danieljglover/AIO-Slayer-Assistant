package com.danieljglover.allinslayer.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A per-task Slayer reward unlock at runtime (WA-1, ADR-0018 #4/#5): the point purchase that
 * affects this assignment (extension, task unlock, superior toggle, ...). Compiled from
 * {@code SourceTaskUnlock} (WA-7). {@code type} is the machine link to its effect - EXTENSION
 * pairs the unlock with {@code TaskData.extendedAmount}; null means unclassified (free-text
 * {@code notes} only), which is every pre-WA-5 authored row (FR-6 back-compat).
 */
@Data
@NoArgsConstructor
public class TaskUnlock
{
    private String unlockId;
    private String name;
    private Integer pointsCost;
    private UnlockType type;
    private String notes;
}
