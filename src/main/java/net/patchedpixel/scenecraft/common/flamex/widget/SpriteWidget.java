package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SpriteWidget extends AbstractWidget {
    private final ResourceLocation texture;
    private final int u, v, spriteW, spriteH, texW, texH;

    public SpriteWidget(ResourceLocation texture, int u, int v, int spriteW, int spriteH, int texW, int texH) {
        super(0, 0, spriteW, spriteH, CommonComponents.EMPTY);
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.spriteW = spriteW;
        this.spriteH = spriteH;
        this.texW = texW;
        this.texH = texH;
        this.active = false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.blit(texture, getX(), getY(), getWidth(), getHeight(), u, v, spriteW, spriteH, texW, texH);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}
}
