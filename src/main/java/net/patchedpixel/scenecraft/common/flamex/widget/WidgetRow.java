package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.components.AbstractWidget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class WidgetRow {
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private final int spacing;
    private int width;
    private int height;

    public WidgetRow(int spacing) {
        this.spacing = spacing;
    }

    public void add(AbstractWidget widget) {
        if(!widgets.contains(widget)) widgets.add(widget);
    }

    public void remove(AbstractWidget widget) {
        widgets.remove(widget);
    }

    public boolean contains(AbstractWidget widget) {
        return widgets.contains(widget);
    }

    public List<AbstractWidget> widgets() {
        return widgets;
    }

    public void forEach(Consumer<AbstractWidget> consumer) {
        for(AbstractWidget w : new ArrayList<>(widgets)) consumer.accept(w);
    }

    public void arrange(int x, int y) {
        int h = 0;
        for(AbstractWidget w : widgets) h = Math.max(h, w.getHeight());
        int cx = x;
        for(AbstractWidget w : widgets) {
            w.setPosition(cx, y + (h - w.getHeight()) / 2);
            cx += w.getWidth() + spacing;
        }

        this.width = widgets.isEmpty() ? 0 : cx - x - spacing;
        this.height = h;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
