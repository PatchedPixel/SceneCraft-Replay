package net.patchedpixel.scenecraft.extras.advancedscreenshots;

import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.platform.Utils;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.extras.SceneCraftExtras;
import net.patchedpixel.scenecraft.render.frame.BitmapFrame;
import net.patchedpixel.scenecraft.render.rendering.Channel;
import net.patchedpixel.scenecraft.render.rendering.FrameConsumer;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;
import net.patchedpixel.scenecraft.gui.versions.Image;
import net.minecraft.CrashReport;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class ScreenshotWriter implements FrameConsumer<BitmapFrame> {
    private final File outputFile;

    public ScreenshotWriter(File outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public void consume(Map<Channel, BitmapFrame> channels) {
        BitmapFrame frame = channels.get(Channel.BRGA);

        // skip the first frame, in which not all chunks are properly loaded
        if (frame.getFrameId() == 0) return;

        final ReadableDimension frameSize = frame.getSize();
        try (Image img = new Image(frameSize.getWidth(), frameSize.getHeight())) {
            for (int y = 0; y < frameSize.getHeight(); y++) {
                for (int x = 0; x < frameSize.getWidth(); x++) {
                    byte b = frame.getByteBuffer().get();
                    byte g = frame.getByteBuffer().get();
                    byte r = frame.getByteBuffer().get();

                    img.setRGBA(x, y, r, g, b, 0xff);
                }
            }

            outputFile.getParentFile().mkdirs();
            img.writePNG(outputFile);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            CrashReport report = CrashReport.forThrowable(e, "Exporting frame");
            MCVer.getMinecraft().delayCrash(report);
        } catch (Throwable t) {
            CrashReport report = CrashReport.forThrowable(t, "Exporting frame");

            SceneCraft.instance.runLater(() -> Utils.error(SceneCraftExtras.LOGGER,
                    SceneCraftReplay.instance.getReplayHandler().getOverlay(),
                    report, null));
        }
    }

    @Override
    public void close() throws IOException {}

    @Override
    public boolean isParallelCapable() {
        return false;
    }
}
