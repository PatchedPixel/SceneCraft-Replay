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
    private static final int PADDING = 5;
    private static final int SLIDER_WIDTH = 6;
    private static final int SLIDER_GAP = 3;

    private final LinearLayout content;
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private int scroll;
    private boolean draggingSlider;

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
        content.setPosition(getX() + (getWidth() - content.getWidth()) / 2, getY() + PADDING - scroll);
    }

    private int maxScroll() {
        return Math.max(0, content.getHeight() + PADDING * 2 - getHeight());
    }

    public boolean isDragging() {
        return draggingSlider;
    }

    private int sliderX() {
        return getX() + (getWidth() + content.getWidth()) / 2 + SLIDER_GAP;
    }

    private int contentHeight() {
        return content.getHeight() + PADDING * 2;
    }

    private boolean onSlider(double x, double y) {
        return maxScroll() > 0 && x >= sliderX() && x < sliderX() + SLIDER_WIDTH && y >= getY() && y < getY() + getHeight();
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
        if(maxScroll() > 0) {
            int x = sliderX();
            int height = getHeight();
            int barY = getY() + scroll * height / contentHeight();
            int barHeight = height * height / contentHeight();
            graphics.fill(x, getY(), x + SLIDER_WIDTH, getY() + height, FlameXTheme.SLIDER_TRACK);
            graphics.fill(x, barY, x + SLIDER_WIDTH, barY + barHeight, FlameXTheme.SLIDER_THUMB);
            graphics.fill(x + SLIDER_WIDTH - 1, barY, x + SLIDER_WIDTH, barY + barHeight, FlameXTheme.SLIDER_SHADOW);
            graphics.fill(x, barY + barHeight - 1, x + SLIDER_WIDTH, barY + barHeight, FlameXTheme.SLIDER_SHADOW);
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
        if(button == 0 && onSlider(mouseX, mouseY)) {
            draggingSlider = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if(!draggingSlider) return false;
        scroll = Math.max(0, Math.min(maxScroll(), scroll + (int) Math.round(dy * contentHeight() / getHeight())));
        place();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean was = draggingSlider;
        draggingSlider = false;
        return was;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}
}
