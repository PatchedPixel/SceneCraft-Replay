package net.patchedpixel.scenecraft.render.frame;

import net.patchedpixel.scenecraft.render.rendering.Frame;
import org.apache.commons.lang3.Validate;

public class CubicGpuFrame implements Frame {
    private final GpuFrame left, right, front, back, top, bottom;

    public CubicGpuFrame(GpuFrame left, GpuFrame right, GpuFrame front, GpuFrame back, GpuFrame top, GpuFrame bottom) {
        Validate.isTrue(left.getFrameId() == right.getFrameId()
                && right.getFrameId() == front.getFrameId()
                && front.getFrameId() == back.getFrameId()
                && back.getFrameId() == top.getFrameId()
                && top.getFrameId() == bottom.getFrameId(), "Frame ids do not match.");
        Validate.isTrue(left.getByteBuffer().remaining() == right.getByteBuffer().remaining()
                && right.getByteBuffer().remaining() == front.getByteBuffer().remaining()
                && front.getByteBuffer().remaining() == back.getByteBuffer().remaining()
                && back.getByteBuffer().remaining() == top.getByteBuffer().remaining()
                && top.getByteBuffer().remaining() == bottom.getByteBuffer().remaining(), "Buffer size does not match.");
        this.left = left;
        this.right = right;
        this.front = front;
        this.back = back;
        this.top = top;
        this.bottom = bottom;
    }

    @Override
    public int getFrameId() {
        return left.getFrameId();
    }

    public GpuFrame getLeft() {
        return this.left;
    }

    public GpuFrame getRight() {
        return this.right;
    }

    public GpuFrame getFront() {
        return this.front;
    }

    public GpuFrame getBack() {
        return this.back;
    }

    public GpuFrame getTop() {
        return this.top;
    }

    public GpuFrame getBottom() {
        return this.bottom;
    }
}
