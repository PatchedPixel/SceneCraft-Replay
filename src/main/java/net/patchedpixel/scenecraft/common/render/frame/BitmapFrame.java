package net.patchedpixel.scenecraft.common.render.frame;

import net.patchedpixel.scenecraft.common.render.rendering.Frame;
import net.patchedpixel.scenecraft.common.flamex.util.geometry.Size;
import org.apache.commons.lang3.Validate;

import java.nio.ByteBuffer;

public class BitmapFrame implements Frame {
    private final int frameId;
    private final Size size;
    private final int bytesPerPixel;
    private final ByteBuffer byteBuffer;

    public BitmapFrame(int frameId, Size size, int bytesPerPixel, ByteBuffer byteBuffer) {
        Validate.isTrue(size.width() * size.height() * bytesPerPixel == byteBuffer.remaining(),
                "Buffer size is %d (cap: %d) but should be %d",
                byteBuffer.remaining(), byteBuffer.capacity(), size.width() * size.height() * bytesPerPixel);
        this.frameId = frameId;
        this.size = size;
        this.bytesPerPixel = bytesPerPixel;
        this.byteBuffer = byteBuffer;
    }

    public int getFrameId() {
        return this.frameId;
    }

    public Size getSize() {
        return this.size;
    }

    public ByteBuffer getByteBuffer() {
        return this.byteBuffer;
    }
}
