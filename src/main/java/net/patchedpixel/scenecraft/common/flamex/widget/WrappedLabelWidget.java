package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WrappedLabelWidget extends AbstractWidget {
    private final int color;

    public WrappedLabelWidget(Component text, int width, int minHeight, int color) {
        super(0, 0, width, minHeight, text);
        this.color = color;
        Font font = MCVer.getFontRenderer();
        this.height = Math.max(minHeight, font.split(text, width).size() * font.lineHeight);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = MCVer.getFontRenderer();
        List<FormattedCharSequence> lines = font.split(getMessage(), getWidth());
        int y = getY();
        for(FormattedCharSequence line : lines) {
            graphics.drawString(font, line, getX(), y, color | 0xFF000000, false);
            y += font.lineHeight;
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}
}
