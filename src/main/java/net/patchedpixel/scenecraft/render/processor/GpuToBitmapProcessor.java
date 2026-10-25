package net.patchedpixel.scenecraft.render.processor;

import net.patchedpixel.scenecraft.render.frame.GpuFrame;
import net.patchedpixel.scenecraft.render.frame.BitmapFrame;
import net.patchedpixel.scenecraft.render.utils.ByteBufferPool;
import net.patchedpixel.scenecraft.gui.utils.geometry.Dimension;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;
import net.patchedpixel.scenecraft.render.utils.Utils;

import java.nio.ByteBuffer;

public class GpuToBitmapProcessor extends AbstractFrameProcessor<GpuFrame, BitmapFrame> {
    @Override
    public BitmapFrame process(GpuFrame rawFrame) {
        ReadableDimension size = rawFrame.getSize();
        int width = size.getWidth();
        int height = size.getHeight();
        int bpp = rawFrame.getBytesPerPixel();
        ByteBuffer result = ByteBufferPool.allocate(width * height * bpp);
        Utils.gpuBytesToBitmap(rawFrame, 0, 0, result, width);
        ByteBufferPool.release(rawFrame.getByteBuffer());
        return new BitmapFrame(rawFrame.getFrameId(), new Dimension(width, height), bpp, result);
    }
}
