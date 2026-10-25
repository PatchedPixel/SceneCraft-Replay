package net.patchedpixel.scenecraft.render.processor;

import net.patchedpixel.scenecraft.render.frame.BitmapFrame;
import net.patchedpixel.scenecraft.render.frame.StereoscopicGpuFrame;
import net.patchedpixel.scenecraft.render.utils.ByteBufferPool;
import net.patchedpixel.scenecraft.gui.utils.geometry.Dimension;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;
import net.patchedpixel.scenecraft.render.utils.Utils;

import java.nio.ByteBuffer;

public class StereoscopicToBitmapProcessor extends AbstractFrameProcessor<StereoscopicGpuFrame, BitmapFrame> {
    @Override
    public BitmapFrame process(StereoscopicGpuFrame rawFrame) {
        ReadableDimension size = rawFrame.getLeft().getSize();
        int width = size.getWidth();
        int bpp = rawFrame.getLeft().getBytesPerPixel();
        ByteBuffer result = ByteBufferPool.allocate(width * 2 * size.getHeight() * bpp);
        Utils.gpuBytesToBitmap(rawFrame.getLeft(), 0, 0, result, width * 2);
        Utils.gpuBytesToBitmap(rawFrame.getRight(), size.getWidth(), 0, result, width * 2);
        ByteBufferPool.release(rawFrame.getLeft().getByteBuffer());
        ByteBufferPool.release(rawFrame.getRight().getByteBuffer());
        return new BitmapFrame(rawFrame.getFrameId(), new Dimension(width * 2, size.getHeight()), bpp, result);
    }
}
