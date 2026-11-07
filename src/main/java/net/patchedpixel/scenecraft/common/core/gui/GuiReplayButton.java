package net.patchedpixel.scenecraft.common.core.gui;

import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.gui.GuiRenderer;
import net.patchedpixel.scenecraft.common.gui.RenderInfo;
import net.patchedpixel.scenecraft.common.gui.element.GuiButton;
import net.patchedpixel.scenecraft.common.gui.utils.geometry.ReadableDimension;
import net.minecraft.resources.Identifier;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.identifier;

public class GuiReplayButton extends GuiButton {
    public static final Identifier ICON = identifier(SceneCraft.MOD_ID, "logo_button.png");

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        super.draw(renderer, size, renderInfo);

        renderer.bindTexture(ICON);
        renderer.drawTexturedRect(3, 3, 0, 0, size.getWidth() - 6, size.getHeight() - 6, 1, 1, 1, 1);
    }
}
