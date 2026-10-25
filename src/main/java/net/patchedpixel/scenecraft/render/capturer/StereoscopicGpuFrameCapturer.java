package net.patchedpixel.scenecraft.render.capturer;

import net.patchedpixel.scenecraft.render.frame.GpuFrame;
import net.patchedpixel.scenecraft.render.frame.StereoscopicGpuFrame;
import net.patchedpixel.scenecraft.render.rendering.Channel;

import java.util.Collections;
import java.util.Map;

public class StereoscopicGpuFrameCapturer extends GpuFrameCapturer<StereoscopicGpuFrame, StereoscopicGpuFrameCapturer.Data> {
    public enum Data implements CaptureData {
        LEFT_EYE, RIGHT_EYE
    }

    public StereoscopicGpuFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        super(worldRenderer, renderInfo);
    }

    @Override
    protected int getFrameWidth() {
        return super.getFrameWidth() / 2;
    }

    @Override
    public Map<Channel, StereoscopicGpuFrame> process() {
        renderInfo.updateForNextFrame();
        int frameId = framesDone++;
        GpuFrame left = renderFrame(frameId, Data.LEFT_EYE);
        GpuFrame right = renderFrame(frameId, Data.RIGHT_EYE);
        StereoscopicGpuFrame frame = new StereoscopicGpuFrame(left, right);
        return Collections.singletonMap(Channel.BRGA, frame);
    }
}
