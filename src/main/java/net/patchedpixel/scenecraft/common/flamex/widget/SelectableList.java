package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.flamex.FlameXTheme;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class SelectableList<T> extends AbstractWidget {
    private static final int PADDING = 5;
    private static final int SLIDER_WIDTH = 6;
    private static final int SLIDER_GAP = 3;

    private final List<T> items = new ArrayList<>();
    private final Function<T, String> label;
    private final Set<T> selected;
    private final BiConsumer<T, Boolean> onClick;
    private final int entryWidth;
    private final int rowHeight;
    private int rowSpacing;
    private int offset;
    private boolean draggingSlider;

    public SelectableList(int width, int height, int entryWidth, int rowHeight, Set<T> selected,
                          Function<T, String> label, BiConsumer<T, Boolean> onClick) {
        super(0, 0, width, height, Component.empty());
        this.entryWidth = entryWidth;
        this.rowHeight = rowHeight;
        this.selected = selected;
        this.label = label;
        this.onClick = onClick;
    }

    public SelectableList<T> setRowSpacing(int spacing) {
        this.rowSpacing = spacing;
        return this;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public List<T> items() {
        return items;
    }

    public void add(T item) {
        items.add(item);
    }

    private int contentHeight() {
        int count = items.size();
        return count * (rowHeight + rowSpacing) - (count > 0 ? rowSpacing : 0) + PADDING * 2;
    }

    private int maxOffset() {
        return Math.max(0, contentHeight() - getHeight());
    }

    private int entryX() {
        return getX() + getWidth() / 2 - entryWidth / 2;
    }

    private int sliderX() {
        return getX() + getWidth() / 2 + entryWidth / 2 + SLIDER_GAP;
    }

    private boolean onSlider(double x, double y) {
        return maxOffset() > 0 && x >= sliderX() && x < sliderX() + SLIDER_WIDTH
                && y >= getY() && y < getY() + getHeight();
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = MCVer.getFontRenderer();
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        offset = Math.max(0, Math.min(maxOffset(), offset));

        graphics.fill(x, y, x + width, y + height, 0x96000000);
        graphics.enableScissor(x, y, x + width, y + height);
        int ex = entryX();
        for(int i = 0; i < items.size(); i++) {
            int ry = y + PADDING + i * (rowHeight + rowSpacing) - offset;
            if(ry + rowHeight < y || ry > y + height) continue;
            T item = items.get(i);
            if(selected.contains(item)) {
                graphics.fill(ex, ry, ex + entryWidth, ry + rowHeight, 0xFF000000);
                graphics.fill(ex, ry, ex + 2, ry + rowHeight, 0xFFFFFFFF);
            }
            graphics.drawString(font, label.apply(item), ex + 5, ry + rowHeight / 2 - font.lineHeight / 2, 0xFFFFFFFF, false);
        }
        graphics.fillGradient(x, y, x + width, y + 4, 0xFF000000, 0x00000000);
        graphics.fillGradient(x, y + height - 4, x + width, y + height, 0x00000000, 0xFF000000);
        graphics.disableScissor();

        if(maxOffset() > 0) {
            int sx = sliderX();
            int barY = y + offset * height / contentHeight();
            int barHeight = height * height / contentHeight();
            graphics.fill(sx, y, sx + SLIDER_WIDTH, y + height, FlameXTheme.Slider.TRACK);
            graphics.fill(sx, barY, sx + SLIDER_WIDTH, barY + barHeight, FlameXTheme.Slider.THUMB);
            graphics.fill(sx + SLIDER_WIDTH - 1, barY, sx + SLIDER_WIDTH, barY + barHeight, FlameXTheme.Slider.SHADOW);
            graphics.fill(sx, barY + barHeight - 1, sx + SLIDER_WIDTH, barY + barHeight, FlameXTheme.Slider.SHADOW);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!active || !visible || button != 0 || !isMouseOver(mouseX, mouseY) && !onSlider(mouseX, mouseY)) return false;
        if(onSlider(mouseX, mouseY)) {
            draggingSlider = true;
            return true;
        }
        int ex = entryX();
        if(mouseX < ex || mouseX >= ex + entryWidth) return false;
        double relative = mouseY - getY() - PADDING + offset;
        int index = (int) (relative / (rowHeight + rowSpacing));
        if(relative < 0 || index >= items.size() || relative - index * (rowHeight + rowSpacing) >= rowHeight) return false;
        onClick.accept(items.get(index), net.minecraft.client.gui.screens.Screen.hasControlDown());
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if(!draggingSlider) return false;
        offset = Math.max(0, Math.min(maxOffset(), offset + (int) Math.round(dy * contentHeight() / getHeight())));
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean was = draggingSlider;
        draggingSlider = false;
        return was;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if(!isMouseOver(mouseX, mouseY) || maxOffset() == 0) return false;
        offset = Math.max(0, Math.min(maxOffset(), offset - (int) Math.signum(delta) * 30));
        return true;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}
}
