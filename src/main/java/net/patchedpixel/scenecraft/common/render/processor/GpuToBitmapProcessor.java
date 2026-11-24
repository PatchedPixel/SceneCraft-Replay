package net.patchedpixel.scenecraft.common.render.processor;

import net.patchedpixel.scenecraft.common.render.frame.GpuFrame;
import net.patchedpixel.scenecraft.common.render.frame.BitmapFrame;
import net.patchedpixel.scenecraft.common.render.utils.ByteBufferPool;
import net.patchedpixel.scenecraft.common.flamex.util.geometry.Size;
import net.patchedpixel.scenecraft.common.render.utils.Utils;

import java.nio.ByteBuffer;

public class GpuToBitmapProcessor extends AbstractFrameProcessor<GpuFrame, BitmapFrame> {
    @Override
    public BitmapFrame process(GpuFrame rawFrame) {
        Size size = rawFrame.getSize();
        int width = size.width();
        int height = size.height();
        int bpp = rawFrame.getBytesPerPixel();
        ByteBuffer result = ByteBufferPool.allocate(width * height * bpp);
        Utils.gpuBytesToBitmap(rawFrame, 0, 0, result, width);
        ByteBufferPool.release(rawFrame.getByteBuffer());
        return new BitmapFrame(rawFrame.getFrameId(), new Size(width, height), bpp, result);
    }
}
