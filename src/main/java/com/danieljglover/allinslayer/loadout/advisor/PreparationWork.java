package com.danieljglover.allinslayer.loadout.advisor;

import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** A shared worker budget prevents large task families multiplying the gear search without limit. */
final class PreparationWork
{
    private int remaining = 3000;
    private final BooleanSupplier cancelled;

    PreparationWork(BooleanSupplier cancelled)
    {
        this.cancelled = cancelled;
    }

    void checkCancelled()
    {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted())
        {
            throw new CancellationException("Preparation superseded by newer account or panel state");
        }
    }

    boolean spend()
    {
        checkCancelled();
        if (remaining == 0) { return false; }
        remaining--;
        return true;
    }

    boolean exhausted() { return remaining == 0; }
}
