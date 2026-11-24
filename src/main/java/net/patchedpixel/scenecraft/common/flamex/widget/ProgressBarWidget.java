package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.NotNull;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

public class ProgressBarWidget extends AbstractWidget {
    private static final int BORDER = 2;
    private volatile float progress;
    private volatile String label = "%d%%";

    public ProgressBarWidget(int width, int height) {
        super(0, 0, width, height, CommonComponents.EMPTY);
        this.active = false;
    }

    public ProgressBarWidget setProgress(float progress) {
        this.progress = progress;
        return this;
    }

    public ProgressBarWidget setLabel(String label) {
        this.label = label;
        return this;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = MCVer.getFontRenderer();

        int x = getX(), y = getY(), width = getWidth(), height = getHeight();
        int barTotalWidth = width - 2 * BORDER;
        int barDoneWidth = (int) (barTotalWidth * Math.max(0f, Math.min(1f, progress)));

        graphics.fill(x, y, x + width, y + height, 0xFF000000);
        graphics.fill(x + BORDER, y + BORDER, x + BORDER + barTotalWidth, y + height - BORDER, 0xFFFFFFFF);
        graphics.fill(x + BORDER, y + BORDER, x + BORDER + barDoneWidth, y + height - BORDER, 0xFF808080);

        String text = String.format(label, (int) (progress * 100));
        graphics.drawString(font, text, x + (width - font.width(text)) / 2, y + height / 2 - font.lineHeight / 2, 0xFF000000, false);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}
}
