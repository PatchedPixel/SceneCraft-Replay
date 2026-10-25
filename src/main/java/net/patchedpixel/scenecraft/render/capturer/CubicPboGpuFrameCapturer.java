package net.patchedpixel.scenecraft.render.capturer;

import net.patchedpixel.scenecraft.render.frame.CubicGpuFrame;
import net.patchedpixel.scenecraft.render.frame.GpuFrame;

public class CubicPboGpuFrameCapturer extends
        PboGpuFrameCapturer<CubicGpuFrame, CubicGpuFrameCapturer.Data> {

    private final int frameSize;
    public CubicPboGpuFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, int frameSize) {
        super(worldRenderer, renderInfo, CubicGpuFrameCapturer.Data.class, frameSize * frameSize);
        this.frameSize = frameSize;
        worldRenderer.setOmnidirectional(true);
    }

    @Override
    protected int getFrameWidth() {
        return frameSize;
    }

    @Override
    protected int getFrameHeight() {
        return frameSize;
    }

    @Override
    protected CubicGpuFrame create(GpuFrame[] from) {
        return new CubicGpuFrame(from[0], from[1], from[2], from[3], from[4], from[5]);
    }
}
