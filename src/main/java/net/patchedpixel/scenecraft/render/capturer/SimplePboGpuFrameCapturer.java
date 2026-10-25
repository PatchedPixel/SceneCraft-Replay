package net.patchedpixel.scenecraft.render.capturer;

import net.patchedpixel.scenecraft.render.frame.GpuFrame;

public class SimplePboGpuFrameCapturer extends PboGpuFrameCapturer<GpuFrame, SimplePboGpuFrameCapturer.SinglePass> {
    public SimplePboGpuFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        super(worldRenderer, renderInfo, SinglePass.class,
                renderInfo.getFrameSize().getWidth() * renderInfo.getFrameSize().getHeight());
    }

    @Override
    protected GpuFrame create(GpuFrame[] from) {
        return from[0];
    }

    public enum SinglePass implements CaptureData {
        SINGLE_PASS
    }
}
