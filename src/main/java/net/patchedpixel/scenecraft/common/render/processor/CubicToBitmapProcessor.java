package net.patchedpixel.scenecraft.common.render.processor;

import net.patchedpixel.scenecraft.common.render.frame.CubicGpuFrame;
import net.patchedpixel.scenecraft.common.render.frame.BitmapFrame;
import net.patchedpixel.scenecraft.common.render.utils.ByteBufferPool;
import net.patchedpixel.scenecraft.common.flamex.util.geometry.Size;
import net.patchedpixel.scenecraft.common.render.utils.Utils;

import java.nio.ByteBuffer;

public class CubicToBitmapProcessor extends AbstractFrameProcessor<CubicGpuFrame, BitmapFrame> {
    @Override
    public BitmapFrame process(CubicGpuFrame rawFrame) {
        int size = rawFrame.getLeft().getSize().width();
        int bpp = rawFrame.getLeft().getBytesPerPixel();
        int width = size * 4;
        int height = size * 3;

        ByteBuffer result = ByteBufferPool.allocate(width * height * bpp);
        Utils.gpuBytesToBitmap(rawFrame.getLeft(), 0, size, result, width);
        Utils.gpuBytesToBitmap(rawFrame.getFront(), size, size, result, width);
        Utils.gpuBytesToBitmap(rawFrame.getRight(), size * 2, size, result, width);
        Utils.gpuBytesToBitmap(rawFrame.getBack(), size * 3, size, result, width);
        Utils.gpuBytesToBitmap(rawFrame.getTop(), size, 0, result, width);
        Utils.gpuBytesToBitmap(rawFrame.getBottom(), size, size * 2, result, width);

        ByteBufferPool.release(rawFrame.getLeft().getByteBuffer());
        ByteBufferPool.release(rawFrame.getRight().getByteBuffer());
        ByteBufferPool.release(rawFrame.getFront().getByteBuffer());
        ByteBufferPool.release(rawFrame.getBack().getByteBuffer());
        ByteBufferPool.release(rawFrame.getTop().getByteBuffer());
        ByteBufferPool.release(rawFrame.getBottom().getByteBuffer());

        return new BitmapFrame(rawFrame.getFrameId(), new Size(width, height), bpp, result);
    }
}
