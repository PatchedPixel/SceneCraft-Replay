package net.patchedpixel.scenecraft.common.extras.advancedscreenshots;

import net.minecraft.CrashReport;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.render.RenderSettings;
import net.patchedpixel.scenecraft.common.render.gui.GuiRenderSettings;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static net.patchedpixel.scenecraft.common.render.SceneCraftRender.LOGGER;
import static net.patchedpixel.scenecraft.common.utils.Utils.error;

public class GuiCreateScreenshot extends GuiRenderSettings {
    private final SceneCraft mod;

    public GuiCreateScreenshot(SceneCraft mod) {
        super(null, null, null);

        this.mod = mod;
        this.background = Background.NONE;

        SceneCraftReplay.instance.getReplayHandler().getReplaySender().setReplaySpeed(0);

        setRenderAction(ctrl -> {
            close();

            mod.runLater(() -> {
                try {
                    RenderSettings settings = save(false, ctrl);

                    boolean success = new ScreenshotRenderer(settings).renderScreenshot();
                    if(success) {
                        new GuiUploadScreenshot(mod, settings).open();
                    }
                } catch(Throwable t) {
                    error(LOGGER, CrashReport.forThrowable(t, "Rendering video"), () -> {});
                    display();
                }
            });
        });
    }

    @Override
    protected boolean isScreenshotMode() {
        return true;
    }

    @Override
    public void close() {
        saveSettings();
        getMinecraft().setScreen(null);
    }

    @Override
    protected File generateOutputFile(RenderSettings.EncodingPreset encodingPreset) {
        DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
        File screenshotFolder = new File(getMinecraft().gameDirectory, "screenshots");
        screenshotFolder.mkdirs();
        String baseName = DATE_FORMAT.format(new Date());
        for(int i = 1; ; i++) {
            File screenshotFile = new File(screenshotFolder, baseName + (i == 1 ? "" : "_" + i) + ".png");
            if(!screenshotFile.exists()) {
                return screenshotFile;
            }
        }
    }

    @Override
    public void load(RenderSettings settings) {
        super.load(settings.withEncodingPreset(RenderSettings.EncodingPreset.PNG));
    }

    @Override
    protected Path getSettingsPath() {
        return getMinecraft().gameDirectory.toPath().resolve("config/scenecraft-screenshotsettings.json");
    }
}
