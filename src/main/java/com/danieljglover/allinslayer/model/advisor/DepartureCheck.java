package com.danieljglover.allinslayer.model.advisor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

/** Outstanding preparation actions and facts that could not be verified. */
@Getter
public final class DepartureCheck
{
    private final List<Entry> entries;

    public DepartureCheck(List<Entry> entries)
    {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public boolean isReady() { return entries.isEmpty(); }

    public String summary(boolean preview)
    {
        if (isReady()) { return preview ? "Packed for preview" : "Ready to leave"; }
        return entries.size() + (entries.size() == 1 ? " thing left" : " things left");
    }

    @Getter
    public static final class Entry
    {
        private final String title;
        private final String detail;
        private final boolean verification;

        public Entry(String title, String detail, boolean verification)
        {
            this.title = title;
            this.detail = detail;
            this.verification = verification;
        }
    }
}
