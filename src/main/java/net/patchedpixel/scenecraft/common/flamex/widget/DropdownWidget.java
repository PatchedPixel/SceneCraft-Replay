package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class DropdownWidget<T> extends AbstractWidget {
    private static final int OUTLINE = 0xFFA0A0A0;
    private static final int BLACK = 0xFF000000;
    private static final int WHITE = 0xFFFFFFFF;

    private final List<T> values;
    private final Function<T, String> toString;
    private final Consumer<T> onSelect;
    private int selected;
    private boolean opened;
    private boolean keepOpenOnRefocus;
    private Function<T, String> entryTooltip;

    public DropdownWidget(List<T> values, T initial, Function<T, String> toString, int width, int height, Consumer<T> onSelect) {
        super(0, 0, width, height, Component.empty());
        this.values = values;
        this.toString = toString;
        this.onSelect = onSelect;
        int index = values.indexOf(initial);
        this.selected = Math.max(0, index);
    }

    public void setEntryTooltip(Function<T, String> entryTooltip) {
        this.entryTooltip = entryTooltip;
    }

    public T getSelectedValue() {
        return values.get(selected);
    }

    public void setSelectedValue(T value) {
        int index = values.indexOf(value);
        if(index >= 0) {
            selected = index;
        }
    }

    private int rowHeight() {
        return MCVer.getFontRenderer().lineHeight + 5;
    }

    private boolean overList(double x, double y) {
        return opened && x >= getX() && x < getX() + getWidth()
                && y >= getY() + getHeight() && y < getY() + getHeight() + rowHeight() * values.size();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return super.isMouseOver(mouseX, mouseY) || overList(mouseX, mouseY);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = MCVer.getFontRenderer();
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();

        graphics.fill(x, y, x + width, y + height, OUTLINE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, BLACK);
        graphics.fill(x + width - height, y, x + width - height + 1, y + height, OUTLINE);

        int base = height - 6;
        int triangle = base / 2;
        int tx = x + width - 3 - base / 2;
        int ty = y + height / 2 - 2;
        for(int layer = triangle; layer > 0; layer--) {
            graphics.fill(tx - layer, ty + (triangle - layer), tx + layer - 1, ty + (triangle - layer) + 1, OUTLINE);
        }

        graphics.enableScissor(x + 1, y + 1, x + width - height, y + height - 1);
        graphics.drawString(font, toString.apply(getSelectedValue()), x + 3, y + height / 2 - font.lineHeight / 2, WHITE, false);
        graphics.disableScissor();

        if(opened) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 300);
            int row = rowHeight();
            for(int i = 0; i < values.size(); i++) {
                int ry = y + height + i * row;
                graphics.fill(x, ry, x + width, ry + row, OUTLINE);
                graphics.fill(x + 1, ry, x + width - 1, ry + row - 1, BLACK);
                graphics.enableScissor(x + 1, ry, x + width - 1, ry + row - 1);
                graphics.drawString(font, toString.apply(values.get(i)), x + 3, ry + 2, WHITE, false);
                graphics.disableScissor();
            }
            graphics.pose().popPose();
            if(entryTooltip != null && overList(mouseX, mouseY)) {
                int hovered = (mouseY - (y + height)) / row;
                if(hovered >= 0 && hovered < values.size()) {
                    graphics.renderTooltip(font, Component.literal(entryTooltip.apply(values.get(hovered))), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!active || !visible || button != 0) return false;
        if(overList(mouseX, mouseY)) {
            int index = (int) ((mouseY - (getY() + getHeight())) / rowHeight());
            if(index >= 0 && index < values.size()) {
                selected = index;
                onSelect.accept(values.get(index));
            }
            opened = false;
            return true;
        }
        if(super.isMouseOver(mouseX, mouseY)) {
            opened = !opened;
            keepOpenOnRefocus = opened;
            return true;
        }
        opened = false;
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if(focused) {
            keepOpenOnRefocus = false;
        } else if(keepOpenOnRefocus) {
            keepOpenOnRefocus = false;
        } else {
            opened = false;
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}
}
