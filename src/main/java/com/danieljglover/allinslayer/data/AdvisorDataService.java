package com.danieljglover.allinslayer.data;

import com.danieljglover.allinslayer.model.advisor.SlayerCatalogue;
import com.google.gson.Gson;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class AdvisorDataService
{
    private static final String RESOURCE = "/data/advisor-catalogue.json";
    private final Gson gson;
    @Getter
    private SlayerCatalogue catalogue = new SlayerCatalogue();

    @Inject
    public AdvisorDataService(Gson gson)
    {
        this.gson = gson;
    }

    public void load()
    {
        catalogue = new SlayerCatalogue();
        try (InputStream stream = getClass().getResourceAsStream(RESOURCE))
        {
            if (stream == null)
            {
                throw new IllegalStateException("Missing bundled advisor catalogue: " + RESOURCE);
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8))
            {
                SlayerCatalogue loaded = gson.fromJson(reader, SlayerCatalogue.class);
                if (loaded == null || loaded.getTasks() == null || loaded.getMonsters() == null
                    || loaded.getMethods() == null || loaded.getLocations() == null
                    || loaded.getMasters() == null || loaded.getItems() == null)
                {
                    throw new IllegalStateException("Invalid bundled advisor catalogue");
                }
                catalogue = loaded;
            }
        }
        catch (Exception exception)
        {
            throw new IllegalStateException("Could not load bundled Slayer advisor catalogue", exception);
        }
    }
}
