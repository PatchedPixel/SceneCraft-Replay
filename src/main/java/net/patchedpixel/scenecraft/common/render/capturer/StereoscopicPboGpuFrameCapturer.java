package net.patchedpixel.scenecraft.common.render.capturer;

import net.patchedpixel.scenecraft.common.render.frame.GpuFrame;
import net.patchedpixel.scenecraft.common.render.frame.StereoscopicGpuFrame;

public class StereoscopicPboGpuFrameCapturer extends PboGpuFrameCapturer<StereoscopicGpuFrame, StereoscopicGpuFrameCapturer.Data> {
    public StereoscopicPboGpuFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        super(worldRenderer, renderInfo, StereoscopicGpuFrameCapturer.Data.class,
                renderInfo.getFrameSize().width() / 2 * renderInfo.getFrameSize().height());
    }

    @Override
    protected int getFrameWidth() {
        return super.getFrameWidth() / 2;
    }

    @Override
    protected StereoscopicGpuFrame create(GpuFrame[] from) {
        return new StereoscopicGpuFrame(from[0], from[1]);
    }
}
