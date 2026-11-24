package net.patchedpixel.scenecraft.common.render.processor;

import net.patchedpixel.scenecraft.common.render.frame.ODSGpuFrame;
import net.patchedpixel.scenecraft.common.render.frame.BitmapFrame;
import net.patchedpixel.scenecraft.common.render.utils.ByteBufferPool;
import net.patchedpixel.scenecraft.common.flamex.util.geometry.Size;

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
        Size size = new Size(leftFrame.getSize().width(), leftFrame.getSize().height() * 2);
        int bpp = rawFrame.getLeft().getLeft().getBytesPerPixel();
        ByteBuffer result = ByteBufferPool.allocate(size.width() * size.height() * bpp);
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
