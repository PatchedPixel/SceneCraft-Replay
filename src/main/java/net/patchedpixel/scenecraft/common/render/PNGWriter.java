package net.patchedpixel.scenecraft.common.render;

import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.scenestudio.util.IOConsumer;
import net.patchedpixel.scenecraft.common.render.frame.BitmapFrame;
import net.patchedpixel.scenecraft.common.render.rendering.Channel;
import net.patchedpixel.scenecraft.common.render.rendering.FrameConsumer;
import net.patchedpixel.scenecraft.common.render.utils.ByteBufferPool;
import net.patchedpixel.scenecraft.common.gui.utils.geometry.ReadableDimension;
import net.patchedpixel.scenecraft.common.gui.versions.Image;
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
        BitmapFrame rgbaFrame = channels.get(Channel.RGBA);
        BitmapFrame depthFrame = channels.get(Channel.DEPTH);
        try {
            if(rgbaFrame != null) {
                withImage(rgbaFrame, true, image ->
                        image.writePNG(outputFolder.resolve(rgbaFrame.getFrameId() + ".png").toFile()));
            }
            if(depthFrame != null) {
                withImage(depthFrame, false, image ->
                        image.writePNG(outputFolder.resolve(depthFrame.getFrameId() + ".depth.png").toFile()));
            }
        } catch(Throwable t) {
            MCVer.getMinecraft().delayCrash(CrashReport.forThrowable(t, "Exporting EXR frame"));
        } finally {
            channels.values().forEach(it -> ByteBufferPool.release(it.getByteBuffer()));
        }
    }

    private void withImage(BitmapFrame frame, boolean rgba, IOConsumer<Image> consumer) throws IOException {
        byte alphaMask = (byte) (keepAlpha ? 0 : 0xff);
        ByteBuffer buffer = frame.getByteBuffer();
        ReadableDimension size = frame.getSize();
        int width = size.getWidth();
        int height = size.getHeight();
        try (Image image = new Image(width, height)) {
            for(int y = 0; y < height; y++) {
                for(int x = 0; x < width; x++) {
                    byte r, g, b, a;
                    if(rgba) {
                        r = buffer.get();
                        g = buffer.get();
                        b = buffer.get();
                        a = buffer.get();
                    } else {
                        // Depth: raw float bytes, keep the layout the depth PNGs always had
                        b = buffer.get();
                        g = buffer.get();
                        r = buffer.get();
                        a = buffer.get();
                    }
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
