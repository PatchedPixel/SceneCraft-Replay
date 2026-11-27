package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.flamex.FlameXConstants;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TextListWidget extends AbstractWidget {
    private static final int SLIDER_WIDTH = 6;
    private static final int SLIDER_GAP = 3;
    private static final int SHADOW_BACKGROUND = 0x96000000;
    private static final int PAIR_GAP = 3;

    private final List<Block> blocks = new ArrayList<>();
    private int spacing = 3;
    private int lineSpacing = 3;
    private int padding = 5;
    private int textColor = 0xFFFFFFFF;
    private boolean shadow;
    private boolean slider;
    private boolean centerContent = true;
    private int offset;
    private boolean draggingSlider;

    public TextListWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    public TextListWidget setSpacing(int spacing) {
        this.spacing = spacing;
        return this;
    }

    public TextListWidget setPadding(int padding) {
        this.padding = padding;
        return this;
    }

    public TextListWidget setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    public TextListWidget setShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public TextListWidget setSlider(boolean slider) {
        this.slider = slider;
        return this;
    }

    public TextListWidget setCenterContent(boolean centerContent) {
        this.centerContent = centerContent;
        return this;
    }

    public void addLine(Component text, boolean centered) {
        blocks.add(new Block(centered, List.of(new Line(text, null))));
    }

    public void addLine(Component text) {
        addLine(text, false);
    }

    public void addBlank() {
        addLine(Component.empty(), false);
    }

    public void addBlock(boolean centered, Line... lines) {
        blocks.add(new Block(centered, List.of(lines)));
    }

    public static Line line(Component text) {
        return new Line(text, null);
    }

    public static Line pair(Component label, Component value) {
        return new Line(label, value);
    }

    private int contentWidth() {
        int width = 0;
        for(Block block : blocks) {
            width = Math.max(width, block.width());
        }
        return width;
    }

    private int contentHeight() {
        int height = padding * 2;
        for(int i = 0; i < blocks.size(); i++) {
            height += blocks.get(i).height(lineSpacing) + (i > 0 ? spacing : 0);
        }
        return height;
    }

    private int maxOffset() {
        return Math.max(0, contentHeight() - getHeight());
    }

    private int contentX(int contentWidth) {
        return centerContent ? getX() + getWidth() / 2 - contentWidth / 2 : getX();
    }

    private int sliderX() {
        return getX() + getWidth() / 2 + contentWidth() / 2 + SLIDER_GAP;
    }

    private boolean onSlider(double x, double y) {
        return slider && maxOffset() > 0 && x >= sliderX() && x < sliderX() + SLIDER_WIDTH
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

        if(shadow) graphics.fill(x, y, x + width, y + height, SHADOW_BACKGROUND);
        graphics.enableScissor(x, y, x + width, y + height);
        int contentWidth = contentWidth();
        int cx = contentX(contentWidth);
        int by = y + padding - offset;
        for(Block block : blocks) {
            int blockHeight = block.height(lineSpacing);
            if(by + blockHeight >= y && by <= y + height) {
                int bw = block.width();
                int bx = block.centered ? cx + (contentWidth - bw) / 2 : cx;
                int ly = by;
                for(Line line : block.lines) {
                    graphics.drawString(font, line.text, bx, ly, textColor, false);
                    if(line.value != null) {
                        graphics.drawString(font, line.value, bx + font.width(line.text) + PAIR_GAP, ly, textColor, false);
                    }
                    ly += font.lineHeight + lineSpacing;
                }
            }
            by += blockHeight + spacing;
        }
        if(shadow) {
            graphics.fillGradient(x, y, x + width, y + 4, 0xFF000000, 0x00000000);
            graphics.fillGradient(x, y + height - 4, x + width, y + height, 0x00000000, 0xFF000000);
        }
        graphics.disableScissor();

        if(slider && maxOffset() > 0) {
            int sx = sliderX();
            int barY = y + offset * height / contentHeight();
            int barHeight = height * height / contentHeight();
            graphics.fill(sx, y, sx + SLIDER_WIDTH, y + height, FlameXConstants.SLIDER_TRACK);
            graphics.fill(sx, barY, sx + SLIDER_WIDTH, barY + barHeight, FlameXConstants.SLIDER_THUMB);
            graphics.fill(sx + SLIDER_WIDTH - 1, barY, sx + SLIDER_WIDTH, barY + barHeight, FlameXConstants.SLIDER_SHADOW);
            graphics.fill(sx, barY + barHeight - 1, sx + SLIDER_WIDTH, barY + barHeight, FlameXConstants.SLIDER_SHADOW);
        }
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

    public record Line(Component text, Component value) {
        int width() {
            Font font = MCVer.getFontRenderer();
            return font.width(text) + (value == null ? 0 : PAIR_GAP + font.width(value));
        }
    }

    private record Block(boolean centered, List<Line> lines) {
        int width() {
            int width = 0;
            for(Line line : lines) {
                width = Math.max(width, line.width());
            }
            return width;
        }

        int height(int lineSpacing) {
            return lines.size() * MCVer.getFontRenderer().lineHeight + (lines.size() - 1) * lineSpacing;
        }
    }
}
