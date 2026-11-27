package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class TextBox extends EditBox {
    private static final int BORDER = 4;
    private static final int BORDER_COLOR = 0xFFA0A0A0;

    private final Font textFont;

    public TextBox(Font font, int width, int height) {
        super(font, 0, 0, width, height, Component.empty());
        this.textFont = font;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if(!isVisible()) return;
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        graphics.fill(x, y, x + w, y + h, isFocused() ? 0xFFFFFFFF : BORDER_COLOR);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF000000);
        setBordered(false);
        setX(x + BORDER);
        setY(y + h / 2 - textFont.lineHeight / 2);

        this.width = w - 2 * BORDER;
        try {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        } finally {
            this.width = w;
            setX(x);
            setY(y);
            setBordered(true);
        }
    }
}
