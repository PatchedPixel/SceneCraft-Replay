package net.patchedpixel.scenecraft.render;

import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.scenestudio.util.IOConsumer;
import net.patchedpixel.scenecraft.render.frame.BitmapFrame;
import net.patchedpixel.scenecraft.render.rendering.Channel;
import net.patchedpixel.scenecraft.render.rendering.FrameConsumer;
import net.patchedpixel.scenecraft.render.utils.ByteBufferPool;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;
import net.patchedpixel.scenecraft.gui.versions.Image;
import net.minecraft.CrashReport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class PNGWriter implements FrameConsumer<BitmapFrame> {
    private final Path outputFolder;
    private final boolean keepAlpha;

    public PNGWriter(Path outputFolder, boolean keepAlpha) throws IOException {
        this.outputFolder = outputFolder;
        this.keepAlpha = keepAlpha;

        Files.createDirectories(outputFolder);
    }

    @Override
    public void consume(Map<Channel, BitmapFrame> channels) {
        BitmapFrame bgraFrame = channels.get(Channel.BRGA);
        BitmapFrame depthFrame = channels.get(Channel.DEPTH);
        try {
            if (bgraFrame != null) {
                withImage(bgraFrame, image ->
                        image.writePNG(outputFolder.resolve(bgraFrame.getFrameId() + ".png").toFile()));
            }
            if (depthFrame != null) {
                withImage(depthFrame, image ->
                        image.writePNG(outputFolder.resolve(depthFrame.getFrameId() + ".depth.png").toFile()));
            }
        } catch (Throwable t) {
            MCVer.getMinecraft().delayCrash(CrashReport.forThrowable(t, "Exporting EXR frame"));
        } finally {
            channels.values().forEach(it -> ByteBufferPool.release(it.getByteBuffer()));
        }
    }

    private void withImage(BitmapFrame frame, IOConsumer<Image> consumer) throws IOException {
        byte alphaMask = (byte) (keepAlpha ? 0 : 0xff);
        ByteBuffer buffer = frame.getByteBuffer();
        ReadableDimension size = frame.getSize();
        int width = size.getWidth();
        int height = size.getHeight();
        try (Image image = new Image(width, height)) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    byte b = buffer.get();
                    byte g = buffer.get();
                    byte r = buffer.get();
                    byte a = buffer.get();
                    image.setRGBA(x, y, r, g, b, a | alphaMask);
                }
            }
            consumer.consume(image);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isParallelCapable() {
        return true;
    }
}
