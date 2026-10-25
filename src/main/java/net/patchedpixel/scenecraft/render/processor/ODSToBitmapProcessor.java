package net.patchedpixel.scenecraft.render.processor;

import net.patchedpixel.scenecraft.render.frame.ODSGpuFrame;
import net.patchedpixel.scenecraft.render.frame.BitmapFrame;
import net.patchedpixel.scenecraft.render.utils.ByteBufferPool;
import net.patchedpixel.scenecraft.gui.utils.geometry.Dimension;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ODSToBitmapProcessor extends AbstractFrameProcessor<ODSGpuFrame, BitmapFrame> {
    private final EquirectangularToBitmapProcessor processor;

    public ODSToBitmapProcessor(int outputWidth, int outputHeight, int sphericalFovX) {
        processor = new EquirectangularToBitmapProcessor(outputWidth, outputHeight / 2, sphericalFovX);
    }

    @Override
    public BitmapFrame process(ODSGpuFrame rawFrame) {
        BitmapFrame leftFrame = processor.process(rawFrame.getLeft());
        BitmapFrame rightFrame = processor.process(rawFrame.getRight());
        ReadableDimension size = new Dimension(leftFrame.getSize().getWidth(), leftFrame.getSize().getHeight() * 2);
        int bpp = rawFrame.getLeft().getLeft().getBytesPerPixel();
        ByteBuffer result = ByteBufferPool.allocate(size.getWidth() * size.getHeight() * bpp);
        result.put(leftFrame.getByteBuffer());
        result.put(rightFrame.getByteBuffer());
        result.rewind();
        ByteBufferPool.release(leftFrame.getByteBuffer());
        ByteBufferPool.release(rightFrame.getByteBuffer());
        return new BitmapFrame(rawFrame.getFrameId(), size, bpp, result);
    }

    @Override
    public void close() throws IOException {
        processor.close();
    }

    public int getFrameSize() {
        return processor.getFrameSize();
    }
}
