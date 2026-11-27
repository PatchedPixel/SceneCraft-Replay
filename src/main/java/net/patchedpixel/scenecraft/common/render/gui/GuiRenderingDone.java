package net.patchedpixel.scenecraft.common.render.gui;

import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.patchedpixel.scenecraft.common.core.SettingsRegistry;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.render.RenderSettings;
import net.patchedpixel.scenecraft.common.render.SceneCraftRender;
import net.patchedpixel.scenecraft.common.render.Setting;
import net.patchedpixel.scenecraft.common.flamex.screen.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;

import java.io.File;

public class GuiRenderingDone extends FlameableScreen {
    public final SceneCraftRender mod;
    public final File videoFile;
    public final int videoFrames;
    public final RenderSettings settings;

    private Checkbox neverOpenCheckbox;

    public GuiRenderingDone(SceneCraftRender mod, File videoFile, int videoFrames, RenderSettings settings) {
        super("scenecraft.gui.renderdonetitle", null, 10);
        this.mod = mod;
        this.videoFile = videoFile;
        this.videoFrames = videoFrames;
        this.settings = settings;
        this.background = Background.DIRT;
    }

    @Override
    protected void build(LinearLayout root) {
        LinearLayout info = FlameX.column(4);
        info.addChild(FlameX.label("scenecraft.gui.renderdone1"));
        info.addChild(FlameX.label("scenecraft.gui.renderdone2"));
        root.addChild(info);
        root.addChild(FlameX.button("scenecraft.gui.openfolder", 200, () -> MCVer.openFile(videoFile.getParentFile())));
    }

    @Override
    protected int rootY() {
        return height / 3 - root.getHeight() / 2;
    }

    @Override
    protected void afterLayout() {
        // Close controls anchored in the bottom right corner
        neverOpenCheckbox = addRenderableWidget(FlameX.checkbox("scenecraft.gui.notagain", false, v -> {}));
        var close = addRenderableWidget(FlameX.button("scenecraft.gui.close", 100, () -> {
            if(neverOpenCheckbox.selected()) {
                SettingsRegistry settingsRegistry = mod.getCore().getSettingsRegistry();
                settingsRegistry.set(Setting.SKIP_POST_RENDER_GUI, true);
                settingsRegistry.save();
            }
            MCVer.getMinecraft().setScreen(null);
        }));
        close.setPosition(width - 10 - close.getWidth(), height - 10 - close.getHeight());
        neverOpenCheckbox.setPosition(close.getX() - 5 - neverOpenCheckbox.getWidth(),
                close.getY() + (close.getHeight() - neverOpenCheckbox.getHeight()) / 2);
    }

    @Override
    public void display() {
        if(mod.getCore().getSettingsRegistry().get(Setting.SKIP_POST_RENDER_GUI)) {
            return;
        }
        super.display();
    }
}
