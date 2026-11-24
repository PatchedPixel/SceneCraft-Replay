package net.patchedpixel.scenecraft.common.record.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.patchedpixel.scenecraft.common.core.SettingsRegistry;
import net.patchedpixel.scenecraft.common.record.Setting;
import net.patchedpixel.scenecraft.common.flamex.event.EventRegistrations;
import net.patchedpixel.scenecraft.common.flamex.event.callbacks.RenderHudCallback;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import static net.patchedpixel.scenecraft.common.SceneCraft.TEXTURE;
import static net.patchedpixel.scenecraft.common.SceneCraft.TEXTURE_SIZE;

public class GuiRecordingOverlay extends EventRegistrations {
    private final Minecraft mc;
    private final SettingsRegistry settingsRegistry;
    private final GuiRecordingControls guiControls;

    public GuiRecordingOverlay(Minecraft mc, SettingsRegistry settingsRegistry, GuiRecordingControls guiControls) {
        this.mc = mc;
        this.settingsRegistry = settingsRegistry;
        this.guiControls = guiControls;
    }

    { on(RenderHudCallback.EVENT, (stack, partialTicks) -> renderRecordingIndicator(stack)); }

    private void renderRecordingIndicator(GuiGraphics graphics) {
        if(guiControls.isStopped()) return;
        if(settingsRegistry.get(Setting.INDICATOR)) {
            Font fontRenderer = MCVer.getFontRenderer();
            String text = guiControls.isPaused() ? I18n.get("scenecraft.gui.paused") : I18n.get("scenecraft.gui.record");
            graphics.drawString(fontRenderer, text.toUpperCase(), 30, 18 - (fontRenderer.lineHeight / 2), 0xffffffff, false);
            graphics.blit(TEXTURE, 10, 10, 16, 16, 58f, 20f, 16, 16, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }
}
