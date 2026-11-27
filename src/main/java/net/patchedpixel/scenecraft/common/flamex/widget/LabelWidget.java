package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class LabelWidget extends StringWidget {
    private float alignX = 0.5F;

    public LabelWidget(Component text, Font font) {
        super(text, font);
    }

    @Override
    public @NotNull StringWidget alignLeft() {
        alignX = 0F;
        return super.alignLeft();
    }

    @Override
    public @NotNull StringWidget alignCenter() {
        alignX = 0.5F;
        return super.alignCenter();
    }

    @Override
    public @NotNull StringWidget alignRight() {
        alignX = 1F;
        return super.alignRight();
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = getFont();
        int x = getX() + Math.round(alignX * (getWidth() - font.width(getMessage())));
        graphics.drawString(font, getMessage(), x, getY() + (getHeight() - font.lineHeight) / 2, getColor() | 0xFF000000, false);
    }
}
