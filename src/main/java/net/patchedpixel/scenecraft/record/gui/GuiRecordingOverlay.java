package net.patchedpixel.scenecraft.record.gui;

import net.patchedpixel.scenecraft.core.SettingsRegistry;
import net.patchedpixel.scenecraft.record.Setting;
import net.patchedpixel.scenecraft.gui.MinecraftGuiRenderer;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.patchedpixel.scenecraft.gui.versions.callbacks.RenderHudCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;

import static net.patchedpixel.scenecraft.core.SceneCraft.TEXTURE;
import static net.patchedpixel.scenecraft.core.SceneCraft.TEXTURE_SIZE;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Renders overlay during record.
 */
public class GuiRecordingOverlay extends EventRegistrations {
    private final Minecraft mc;
    private final SettingsRegistry settingsRegistry;
    private final GuiRecordingControls guiControls;

    public GuiRecordingOverlay(Minecraft mc, SettingsRegistry settingsRegistry, GuiRecordingControls guiControls) {
        this.mc = mc;
        this.settingsRegistry = settingsRegistry;
        this.guiControls = guiControls;
    }

    /**
     * Render the record icon and text in the top left corner of the screen.
     */
    { on(RenderHudCallback.EVENT, (stack, partialTicks) -> renderRecordingIndicator(stack)); }
    private void renderRecordingIndicator(GuiGraphics stack) {
        if (guiControls.isStopped()) return;
        if (settingsRegistry.get(Setting.INDICATOR)) {
            Font fontRenderer = mc.font;
            String text = guiControls.isPaused() ? I18n.get("scenecraft.gui.paused") : I18n.get("scenecraft.gui.record");
            MinecraftGuiRenderer renderer = new MinecraftGuiRenderer(stack);
            renderer.drawString(30, 18 - (fontRenderer.lineHeight / 2), 0xffffffff, text.toUpperCase());
            renderer.bindTexture(TEXTURE);
            renderer.drawTexturedRect(10, 10, 58, 20, 16, 16, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }
}
