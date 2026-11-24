package net.patchedpixel.scenecraft.common.flamex.popup;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.flamex.FlameXTheme;
import net.patchedpixel.scenecraft.common.flamex.FlameableScreen;
import org.jetbrains.annotations.NotNull;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import javax.annotation.Nullable;

public abstract class PopupScreen extends FlameableScreen {
    protected PopupScreen(@Nullable Screen parent, int spacing) {
        super(Component.empty(), parent, spacing);
        this.panel = true;
    }

    public static AbstractWidget label(Component text) {
        Minecraft mc = MCVer.getMinecraft();
        return new AbstractWidget(0, 0, MCVer.getFontRenderer().width(text), MCVer.getFontRenderer().lineHeight, text) {
            @Override
            protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                graphics.drawString(MCVer.getFontRenderer(), getMessage(), getX(), getY(), FlameXTheme.TEXT_ON_PANEL, false);
            }

            @Override
            protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}
        };
    }
}
