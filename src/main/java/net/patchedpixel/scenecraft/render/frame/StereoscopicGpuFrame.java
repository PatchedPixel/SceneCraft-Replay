package net.patchedpixel.scenecraft.render.frame;

import net.patchedpixel.scenecraft.render.rendering.Frame;
import org.apache.commons.lang3.Validate;

public class StereoscopicGpuFrame implements Frame {
    private final GpuFrame left, right;

    public StereoscopicGpuFrame(GpuFrame left, GpuFrame right) {
        Validate.isTrue(left.getFrameId() == right.getFrameId(), "Frame ids do not match.");
        Validate.isTrue(left.getByteBuffer().remaining() == right.getByteBuffer().remaining(), "Buffer size does not match.");
        this.left = left;
        this.right = right;
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
}
