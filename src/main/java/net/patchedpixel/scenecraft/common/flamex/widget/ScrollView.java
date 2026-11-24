package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.flamex.FlameXTheme;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ScrollView extends AbstractWidget {
    private static final int STEP = 20;

    private final LinearLayout content;
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private int scroll;

    public ScrollView(LinearLayout content, int width, int height) {
        super(0, 0, width, height, Component.empty());
        this.content = content;

        content.visitWidgets(widgets::add);
    }

    public List<AbstractWidget> widgets() {
        return widgets;
    }

    public void place() {
        content.arrangeElements();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        content.setPosition(getX() + (getWidth() - content.getWidth()) / 2, getY() - scroll);
    }

    private int maxScroll() {
        return Math.max(0, content.getHeight() - getHeight());
    }

    public boolean contains(double x, double y) {
        return x >= getX() && x < getX() + getWidth() && y >= getY() && y < getY() + getHeight();
    }

    public void setInputEnabled(boolean enabled) {
        for(AbstractWidget w : widgets) w.visible = enabled;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean inside = contains(mouseX, mouseY);
        graphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
        for(AbstractWidget widget : widgets) {
            if(widget.getY() + widget.getHeight() < getY() || widget.getY() > getY() + getHeight()) continue;
            boolean wasVisible = widget.visible;
            widget.visible = true;
            widget.render(graphics, inside ? mouseX : -1, inside ? mouseY : -1, partialTick);
            widget.visible = wasVisible;
        }
        graphics.disableScissor();
        int max = maxScroll();
        if(max > 0) {
            int barHeight = Math.max(16, getHeight() * getHeight() / content.getHeight());
            int barY = getY() + (getHeight() - barHeight) * scroll / max;
            graphics.fill(getX() + getWidth() - 3, barY, getX() + getWidth(), barY + barHeight, FlameXTheme.SCROLL_THUMB);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if(!contains(mouseX, mouseY) || maxScroll() == 0) return false;
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta) * STEP));

        place();
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}
}
