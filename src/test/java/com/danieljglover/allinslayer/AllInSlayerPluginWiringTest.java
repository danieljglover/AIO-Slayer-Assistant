package com.danieljglover.allinslayer;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.plugins.itemstats.Effect;
import net.runelite.client.plugins.itemstats.ItemStatChanges;
import net.runelite.client.plugins.itemstats.ItemStatChangesService;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Proves the LFA root-cause fix: RuneLite binds {@code ItemStatChangesService} only inside
 * {@code ItemStatPlugin.configure(Binder)} (that plugin's own child injector), so an external plugin
 * never sees the binding - which is why the old {@code @Inject(optional = true)} field came back null.
 *
 * <p>The fix adapts the <b>public</b> {@code ItemStatChanges} (a {@code @Singleton} with a no-arg
 * constructor that Guice can just-in-time construct in our own injector) to the
 * {@code ItemStatChangesService} functional interface via {@link AllInSlayerPlugin}'s {@code @Provides}
 * method. This test confirms, headlessly, that the live service is genuinely reachable that way.
 */
public class AllInSlayerPluginWiringTest
{
    @Test
    public void guiceCanJitConstructItemStatChangesSoTheAdapterServiceIsBound()
    {
        // Guice JIT-constructs the public ItemStatChanges in a plain injector (no RuneLite bindings).
        Injector injector = Guice.createInjector();
        ItemStatChanges delegate = injector.getInstance(ItemStatChanges.class);
        assertNotNull("ItemStatChanges must be JIT-constructable in our injector", delegate);

        ItemStatChangesService service = new AllInSlayerPlugin().provideItemStatChangesService(delegate);
        assertNotNull("the @Provides adapter must yield a service", service);

        // The live itemstats data knows these potions - so the service path now feeds the selector.
        Effect superCombat = service.getItemStatChanges(ItemID._4DOSE2COMBAT);
        assertNotNull("live service must recognise Super combat", superCombat);
        assertNotNull("live service must recognise Bastion", service.getItemStatChanges(ItemID._4DOSEBASTION));
        assertNotNull("live service must recognise Battlemage", service.getItemStatChanges(ItemID._4DOSEBATTLEMAGE));
    }
}
