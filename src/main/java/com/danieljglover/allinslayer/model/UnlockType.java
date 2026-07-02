package com.danieljglover.allinslayer.model;

/**
 * The machine-usable unlock -> effect link (WA-1, ADR-0018 #5). EXTENSION is the load-bearing
 * value: an EXTENSION-typed unlock enables the task's {@code extendedAmount}, cross-validated at
 * build time (WA-7). The other values are a coarse classification for display; Gson defaults an
 * absent/unknown {@code type} key to null, so today's untyped rows keep loading unchanged (FR-6).
 */
public enum UnlockType
{
    EXTENSION,
    TASK_UNLOCK,
    SUPERIOR,
    FINISHING_BLOW,
    COSMETIC,
    OTHER
}
