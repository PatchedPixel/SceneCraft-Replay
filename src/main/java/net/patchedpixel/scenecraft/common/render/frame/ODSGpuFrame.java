package net.patchedpixel.scenecraft.common.render.frame;

import net.patchedpixel.scenecraft.common.render.rendering.Frame;
import org.apache.commons.lang3.Validate;

public class ODSGpuFrame implements Frame {
    private final CubicGpuFrame left, right;

    public ODSGpuFrame(CubicGpuFrame left, CubicGpuFrame right) {
        Validate.isTrue(left.getFrameId() == right.getFrameId(), "Frame ids do not match.");
        this.left = left;
        this.right = right;
    }

    @Override
    public int getFrameId() {
        return left.getFrameId();
    }

    public CubicGpuFrame getLeft() {
        return this.left;
    }

    public CubicGpuFrame getRight() {
        return this.right;
    }
}
