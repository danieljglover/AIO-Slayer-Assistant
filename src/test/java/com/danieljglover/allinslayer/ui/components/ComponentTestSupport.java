package com.danieljglover.allinslayer.ui.components;

import com.danieljglover.allinslayer.ui.ItemIconRenderer;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

/**
 * Shared headless-Swing test helpers for the leaf component library, mirroring the conventions of
 * {@code SlayerPanelTest} (construct/mutate on the EDT, walk the tree by name/type). Kept in one
 * place so the seven component tests do not each re-declare {@code runOnEdt} / tree walkers.
 */
final class ComponentTestSupport
{
    private ComponentTestSupport()
    {
    }

    static void runOnEdt(Runnable runnable)
    {
        try
        {
            if (SwingUtilities.isEventDispatchThread())
            {
                runnable.run();
            }
            else
            {
                SwingUtilities.invokeAndWait(runnable);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    static <T> T buildOnEdt(Supplier<T> supplier)
    {
        AtomicReference<T> ref = new AtomicReference<>();
        runOnEdt(() -> ref.set(supplier.get()));
        return ref.get();
    }

    static List<Component> allComponents(Container container)
    {
        List<Component> components = new ArrayList<>();
        collectComponents(container, components);
        return components;
    }

    private static void collectComponents(Component component, List<Component> components)
    {
        components.add(component);
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents())
            {
                collectComponents(child, components);
            }
        }
    }

    static <T extends Component> T componentByName(Container container, String name, Class<T> type)
    {
        for (Component component : allComponents(container))
        {
            if (type.isInstance(component) && name.equals(component.getName()))
            {
                return type.cast(component);
            }
        }
        throw new AssertionError("Missing component named: " + name);
    }

    static List<String> labelTexts(Container container)
    {
        List<String> texts = new ArrayList<>();
        for (Component component : allComponents(container))
        {
            if (component instanceof JLabel)
            {
                texts.add(((JLabel) component).getText());
            }
        }
        return texts;
    }

    /**
     * A call-recording {@link ItemIconRenderer} fake (ADR-0001 test seam). Records the
     * {@code (label, id, qty, stackable, dim)} of every render request so item-component tests can
     * assert what was asked of the renderer without ever touching a live {@code ItemManager}.
     */
    static final class RecordingIconRenderer implements ItemIconRenderer
    {
        static final class Call
        {
            final JLabel label;
            final int itemId;
            final int quantity;
            final boolean stackable;
            final boolean dim;

            Call(JLabel label, int itemId, int quantity, boolean stackable, boolean dim)
            {
                this.label = label;
                this.itemId = itemId;
                this.quantity = quantity;
                this.stackable = stackable;
                this.dim = dim;
            }
        }

        final List<Call> calls = new ArrayList<>();

        @Override
        public void render(JLabel label, int itemId, int quantity, boolean stackable, boolean dim)
        {
            calls.add(new Call(label, itemId, quantity, stackable, dim));
        }

        /** The recorded call whose target label is {@code label}, or {@code null} if none. */
        Call callFor(JLabel label)
        {
            for (Call call : calls)
            {
                if (call.label == label)
                {
                    return call;
                }
            }
            return null;
        }
    }
}
