package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class SpriteButton extends Button {
    private final ResourceLocation texture;
    private final int textureSize;
    private final int spriteSize;
    private IntSupplier u = () -> 0;
    private IntSupplier v = () -> 0;
    private Supplier<Component> tooltipText;

    public SpriteButton(int width, int height, ResourceLocation texture, int textureSize, int spriteSize, Runnable onPress) {
        super(0, 0, width, height, CommonComponents.EMPTY, b -> onPress.run(), DEFAULT_NARRATION);
        this.texture = texture;
        this.textureSize = textureSize;
        this.spriteSize = spriteSize;
    }

    public SpriteButton uv(IntSupplier u, IntSupplier v) {
        this.u = u;
        this.v = v;

        return this;
    }

    public SpriteButton uv(int u, int v) {
        return uv(() -> u, () -> v);
    }

    public void pressWithSound() {
        if(!active) {
            return;
        }

        playDownSound(Minecraft.getInstance().getSoundManager());
        onPress();
    }

    public SpriteButton dynamicTooltip(Supplier<Component> text) {
        this.tooltipText = text;
        return this;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if(tooltipText != null && isHoveredOrFocused()) {
            setTooltip(Tooltip.create(tooltipText.get()));
        }
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        graphics.blit(texture, getX(), getY(), getWidth(), getHeight(), u.getAsInt(), v.getAsInt(),
                spriteSize, spriteSize, textureSize, textureSize);
    }
}
