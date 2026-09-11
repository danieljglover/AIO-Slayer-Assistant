package com.danieljglover.allinslayer.data.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SlayerDataValidationException extends RuntimeException
{
    private final List<String> errors;

    public SlayerDataValidationException(List<String> errors)
    {
        super(String.join("\n", errors));
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public List<String> getErrors()
    {
        return errors;
    }
}
