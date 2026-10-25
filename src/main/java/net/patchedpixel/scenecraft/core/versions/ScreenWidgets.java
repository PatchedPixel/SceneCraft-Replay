package net.patchedpixel.scenecraft.core.versions;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * Replacement for Fabric's {@code Screens.getWidgets}: a live list view of all top-level
 * {@link AbstractWidget}s of a screen. Adding a widget registers it as child and renderable.
 */
public final class ScreenWidgets {
    private ScreenWidgets() {}

    @SuppressWarnings("unchecked")
    public static List<AbstractWidget> getWidgets(Screen screen) {
        return new AbstractList<>() {
            private List<GuiEventListener> children() {
                return (List<GuiEventListener>) screen.children();
            }

            private List<AbstractWidget> snapshot() {
                List<AbstractWidget> out = new ArrayList<>();
                for (GuiEventListener l : children()) {
                    if (l instanceof AbstractWidget w) out.add(w);
                }
                return out;
            }

            @Override
            public AbstractWidget get(int index) {
                return snapshot().get(index);
            }

            @Override
            public int size() {
                return snapshot().size();
            }

            @Override
            public boolean add(AbstractWidget widget) {
                register(widget);
                return true;
            }

            @Override
            public void add(int index, AbstractWidget widget) {
                List<AbstractWidget> before = snapshot();
                AbstractWidget anchor = index < before.size() ? before.get(index) : null;
                register(widget);
                List<GuiEventListener> children = children();
                if (anchor != null) {
                    children.remove(widget);
                    children.add(children.indexOf(anchor), widget);
                }
            }

            private void register(AbstractWidget widget) {
                screen.addRenderableWidget(widget);
            }
        };
    }
}
