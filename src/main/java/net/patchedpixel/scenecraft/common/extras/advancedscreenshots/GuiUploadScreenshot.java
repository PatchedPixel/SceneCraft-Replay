package net.patchedpixel.scenecraft.common.extras.advancedscreenshots;

import net.minecraft.client.gui.layouts.LinearLayout;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.core.SettingsRegistry;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.extras.Setting;
import net.patchedpixel.scenecraft.common.render.RenderSettings;
import net.patchedpixel.scenecraft.common.flamex.components.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;

import java.net.URI;

public class GuiUploadScreenshot extends FlameableScreen {
    public final SceneCraft mod;
    public final RenderSettings renderSettings;
    private boolean neverOpen;

    public GuiUploadScreenshot(SceneCraft mod, RenderSettings renderSettings) {
        super("scenecraft.gui.advancedscreenshots.finished.title", null, 6);
        this.panel = true;
        this.mod = mod;
        this.renderSettings = renderSettings;
        this.background = Background.NONE;
    }

    @Override
    protected void build(LinearLayout root) {
        boolean veer = renderSettings.getRenderMethod() == RenderSettings.RenderMethod.EQUIRECTANGULAR;

        root.addChild(FlameX.label(veer
                ? "scenecraft.gui.advancedscreenshots.finished.description.360"
                : "scenecraft.gui.advancedscreenshots.finished.description"));
        if(veer) {
            root.addChild(FlameX.label("scenecraft.gui.advancedscreenshots.finished.description.veer"));
            root.addChild(FlameX.button("scenecraft.gui.advancedscreenshots.finished.upload.veer", 150,
                    () -> MCVer.openURL(URI.create("https://veer.tv/upload"))));
        }
        root.addChild(FlameX.button("scenecraft.gui.advancedscreenshots.finished.showfile", 150,
                () -> MCVer.openFile(renderSettings.getOutputFile().getParentFile())));
        root.addChild(FlameX.checkbox("scenecraft.gui.notagain", neverOpen, v -> neverOpen = v));
        root.addChild(FlameX.button("scenecraft.gui.close", 150, this::close));
    }

    public void open() {
        if(mod.getSettingsRegistry().get(Setting.SKIP_POST_SCREENSHOT_GUI)) {
            return;
        }
        display();
    }

    @Override
    public void close() {
        if(neverOpen) {
            SettingsRegistry settingsRegistry = mod.getSettingsRegistry();
            settingsRegistry.set(Setting.SKIP_POST_SCREENSHOT_GUI, true);
            settingsRegistry.save();
        }
        MCVer.getMinecraft().setScreen(null);
    }
}
