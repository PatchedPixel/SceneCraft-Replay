package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.patchedpixel.scenecraft.common.SceneCraft;
import org.jetbrains.annotations.NotNull;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.identifier;

public class LogoButton extends Button {
    public static final ResourceLocation ICON = identifier(SceneCraft.MOD_ID, "logo_button.png");

    public LogoButton(int size, Runnable onPress) {
        super(0, 0, size, size, CommonComponents.EMPTY, b -> onPress.run(), DEFAULT_NARRATION);
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        graphics.blit(ICON, getX() + 3, getY() + 3, getWidth() - 6, getHeight() - 6, 0f, 0f, 1, 1, 1, 1);
    }
}
