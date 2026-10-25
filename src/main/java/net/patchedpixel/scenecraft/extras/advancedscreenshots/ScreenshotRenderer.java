package net.patchedpixel.scenecraft.extras.advancedscreenshots;

import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.render.RenderSettings;
import net.patchedpixel.scenecraft.render.capturer.RenderInfo;
import net.patchedpixel.scenecraft.render.hooks.ForceChunkLoadingHook;
import net.patchedpixel.scenecraft.render.rendering.Pipelines;
import net.patchedpixel.scenecraft.gui.utils.geometry.Dimension;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.CrashReport;

import static net.patchedpixel.scenecraft.core.versions.MCVer.resizeMainWindow;

public class ScreenshotRenderer implements RenderInfo {
    private final Minecraft mc = MCVer.getMinecraft();

    private final RenderSettings settings;

    private int framesDone;

    public ScreenshotRenderer(RenderSettings settings) {
        this.settings = settings;
    }

    public boolean renderScreenshot() {
        try {
            Window window = mc.getWindow();
            int widthBefore = window.getWidth();
            int heightBefore = window.getHeight();

            ForceChunkLoadingHook clrg = new ForceChunkLoadingHook(mc.levelRenderer);

            Pipelines.newPipeline(settings.getRenderMethod(), this,
                    new ScreenshotWriter(settings.getOutputFile())).run();

            clrg.uninstall();

            resizeMainWindow(mc, widthBefore, heightBefore);
            return true;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            CrashReport report = CrashReport.forThrowable(e, "Creating Equirectangular Screenshot");
            MCVer.getMinecraft().delayCrash(report);
        }
        return false;
    }

    @Override
    public ReadableDimension getFrameSize() {
        return new Dimension(settings.getVideoWidth(), settings.getVideoHeight());
    }

    @Override
    public int getFramesDone() {
        return framesDone;
    }

    @Override
    public int getTotalFrames() {
        // render 2 frames, because only the second contains all frames fully loaded
        return 2;
    }

    @Override
    public float updateForNextFrame() {
        framesDone++;
        return mc.getFrameTime();
    }

    @Override
    public RenderSettings getRenderSettings() {
        return settings;
    }
}
