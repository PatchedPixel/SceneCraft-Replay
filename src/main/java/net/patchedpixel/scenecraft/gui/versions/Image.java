package net.patchedpixel.scenecraft.gui.versions;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Cleaner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * AWT must never be initialized in the game process, otherwise windowing can break on macOS.
 * **Any** usage of BufferedImage will initialize AWT (static initializer).
 */
public class Image implements AutoCloseable {
    private NativeImage inner;
    private final Cleaner.Cleanable cleanable;
    private static final Cleaner CLEANER = Cleaner.create();

    public Image(int width, int height) {
        this(
                new NativeImage(NativeImage.Format.RGBA, width, height, true)
        );
    }

    public Image(NativeImage inner) {
        this.inner = inner;
        this.cleanable = CLEANER.register(this, new NativeImageCloser(inner));
    }

    @Override
    public void close() {
        if (inner != null) {
            inner = null;
            cleanable.clean();
        }
    }

    public int getWidth() {
        return inner.getWidth();
    }

    public int getHeight() {
        return inner.getHeight();
    }

    public void setRGBA(int x, int y, int r, int g, int b, int a) {
        inner.setPixelRGBA(x, y, ((a & 0xff) << 24) | ((b & 0xff) << 16) | ((g & 0xff) << 8) | (r & 0xff));
    }

    public static Image read(Path path) throws IOException {
        return read(Files.newInputStream(path));
    }

    public static Image read(InputStream in) throws IOException {
        byte[] bytes;
        try (InputStream ignored = in) {
            bytes = in.readAllBytes();
        }
        in = new ByteArrayInputStream(bytes);

        return new Image(NativeImage.read(in));
    }

    public static Image read(ReplayFile file, InputStream in, boolean[] converted) throws IOException {
        byte[] bytes;
        try(InputStream ignored = in) {
            bytes = in.readAllBytes();
        }

        boolean png = bytes.length >= 8 && Arrays.equals(
                Arrays.copyOf(bytes, 8),
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );

        Image image = read(new ByteArrayInputStream(bytes));
        converted[0] = false;

        if(!png) {
            try(ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                image.writePNG(out);
                file.writeThumbBytes(out.toByteArray());
                converted[0] = true;
            } catch (IOException e) {
                image.close();
                throw e;
            }
        }

        return image;
    }

    public void writePNG(File file) throws IOException {
        inner.writeToFile(file);
    }

    public void writePNG(OutputStream outputStream) throws IOException {
        Path tmp = Files.createTempFile("tmp", ".png");
        try {
            inner.writeToFile(tmp);
            Files.copy(tmp, outputStream);
        } finally {
            Files.delete(tmp);
        }
    }

    public Image scaledSubRect(int x, int y, int width, int height, int scaledWidth, int scaledHeight) {
        NativeImage dst = new NativeImage(inner.format(), scaledWidth, scaledHeight, false);
        inner.resizeSubRectTo(x, y, width, height, dst);
        return new Image(dst);
    }

    @Deprecated // BufferedImage should not be used, see class docs
    public BufferedImage toBufferedImage() {
        // Not very efficient but certainly the easiest solution.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            writePNG(out);
            return ImageIO.read(new ByteArrayInputStream(out.toByteArray()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public DynamicTexture toTexture() {
        return new DynamicTexture(inner);
    }

    private static final class NativeImageCloser implements Runnable {
        private final NativeImage image;

        private NativeImageCloser(NativeImage image) {
            this.image = image;
        }

        @Override
        public void run() {
            image.close();
        }
    }
}
