package net.patchedpixel.scenecraft.common.render.processor;

import net.patchedpixel.scenecraft.common.render.frame.BitmapFrame;
import net.patchedpixel.scenecraft.common.render.frame.StereoscopicGpuFrame;
import net.patchedpixel.scenecraft.common.render.utils.ByteBufferPool;
import net.patchedpixel.scenecraft.common.flamex.util.geometry.Size;
import net.patchedpixel.scenecraft.common.render.utils.Utils;

import java.nio.ByteBuffer;

public class StereoscopicToBitmapProcessor extends AbstractFrameProcessor<StereoscopicGpuFrame, BitmapFrame> {
    @Override
    public BitmapFrame process(StereoscopicGpuFrame rawFrame) {
        Size size = rawFrame.getLeft().getSize();
        int width = size.width();
        int bpp = rawFrame.getLeft().getBytesPerPixel();

        ByteBuffer result = ByteBufferPool.allocate(width * 2 * size.height() * bpp);
        Utils.gpuBytesToBitmap(rawFrame.getLeft(), 0, 0, result, width * 2);
        Utils.gpuBytesToBitmap(rawFrame.getRight(), size.width(), 0, result, width * 2);
        ByteBufferPool.release(rawFrame.getLeft().getByteBuffer());
        ByteBufferPool.release(rawFrame.getRight().getByteBuffer());

        return new BitmapFrame(rawFrame.getFrameId(), new Size(width * 2, size.height()), bpp, result);
    }
}
