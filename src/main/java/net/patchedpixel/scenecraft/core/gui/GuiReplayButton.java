package net.patchedpixel.scenecraft.core.gui;

import net.patchedpixel.scenecraft.core.SceneCraft;
import net.patchedpixel.scenecraft.gui.GuiRenderer;
import net.patchedpixel.scenecraft.gui.RenderInfo;
import net.patchedpixel.scenecraft.gui.element.GuiButton;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;
import net.minecraft.resources.ResourceLocation;

import static net.patchedpixel.scenecraft.core.versions.MCVer.identifier;

public class GuiReplayButton extends GuiButton {
    public static final ResourceLocation ICON = identifier(SceneCraft.MOD_ID, "logo_button.png");

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        super.draw(renderer, size, renderInfo);

        renderer.bindTexture(ICON);
        renderer.drawTexturedRect(3, 3, 0, 0, size.getWidth() - 6, size.getHeight() - 6, 1, 1, 1, 1);
    }
}
