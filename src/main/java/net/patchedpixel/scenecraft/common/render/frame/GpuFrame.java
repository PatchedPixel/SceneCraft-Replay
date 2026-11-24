package net.patchedpixel.scenecraft.common.render.frame;

import net.patchedpixel.scenecraft.common.render.rendering.Frame;
import net.patchedpixel.scenecraft.common.flamex.util.geometry.Size;

import java.nio.ByteBuffer;

public class GpuFrame implements Frame {
    private final int frameId;
    private final Size size;
    private final int bytesPerPixel;
    private final ByteBuffer byteBuffer;

    public GpuFrame(int frameId, Size size, int bytesPerPixel, ByteBuffer byteBuffer) {
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

    public int getBytesPerPixel() {
        return bytesPerPixel;
    }

    public ByteBuffer getByteBuffer() {
        return this.byteBuffer;
    }
}
