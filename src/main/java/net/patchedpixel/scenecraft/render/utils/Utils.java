package net.patchedpixel.scenecraft.render.utils;

import net.patchedpixel.scenecraft.render.frame.GpuFrame;

import java.nio.ByteBuffer;

public class Utils {
    /**
     * Copies the rgb image (flipped vertically) to the specified position in the target buffer
     * @param source Source image
     * @param xOffset X offset in target image
     * @param yOffset Y offset in target image
     * @param to Target image
     * @param width Target image width
     */
    public static void gpuBytesToBitmap(GpuFrame source, int xOffset, int yOffset, ByteBuffer to, int width) {
        gpuBytesToBitmap(
                source.getByteBuffer(), source.getSize().getWidth(), source.getBytesPerPixel(),
                xOffset, yOffset, to, width);
    }

    /**
     * Copies the rgb image (flipped vertically) to the specified position in the target buffer
     * @param buffer Source image
     * @param bufferWidth  Source image width
     * @param bbp Bytes per pixel
     * @param xOffset X offset in target image
     * @param yOffset Y offset in target image
     * @param to Target image
     * @param width Target image width
     */
    public static void gpuBytesToBitmap(ByteBuffer buffer, int bufferWidth, int bbp, int xOffset, int yOffset, ByteBuffer to, int width) {
        byte[] rowBuf = new byte[bufferWidth * bbp];
        // Copy image flipped vertically to target buffer
        int rows = buffer.remaining() / bbp / bufferWidth;
        for (int i = 0; i < rows; i++) {
            buffer.get(rowBuf);
            to.position(((yOffset + rows - i - 1) * width + xOffset) * bbp);
            to.put(rowBuf);
        }
        to.rewind();
    }
}
