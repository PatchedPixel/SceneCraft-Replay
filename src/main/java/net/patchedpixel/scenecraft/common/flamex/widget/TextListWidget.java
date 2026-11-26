package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

public class TextListWidget extends ObjectSelectionList<TextListWidget.Line> {
    public TextListWidget(int width, int height, int y0, int y1) {
        super(MCVer.getMinecraft(), width, height, y0, y1, 12);
        setRenderBackground(true);
        setRenderTopAndBottom(true);
    }

    public void addLine(Component text, boolean centered) {
        addEntry(new Line(text, centered));
    }

    public void addLine(Component text) {
        addLine(text, false);
    }

    public void addBlank() {
        addLine(Component.empty(), false);
    }

    @Override
    public int getRowWidth() {
        return Math.max(50, width - 20);
    }

    @Override
    protected int getScrollbarPosition() {
        return x0 + width - 6;
    }

    @Override
    public void setSelected(Line selected) {}

    public static class Line extends ObjectSelectionList.Entry<Line> {
        private final Component text;
        private final boolean centered;

        Line(Component text, boolean centered) {
            this.text = text;
            this.centered = centered;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            var font = MCVer.getFontRenderer();
            int x = centered ? left + (width - font.width(text)) / 2 : left;
            graphics.drawString(font, text, x, top + 2, 0xFFFFFFFF, false);
        }

        @Override
        public @NotNull Component getNarration() {
            return text;
        }
    }
}
