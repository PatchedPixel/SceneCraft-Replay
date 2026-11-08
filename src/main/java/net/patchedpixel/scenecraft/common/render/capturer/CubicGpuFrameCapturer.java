package net.patchedpixel.scenecraft.common.render.capturer;

import net.patchedpixel.scenecraft.common.render.frame.CubicGpuFrame;
import net.patchedpixel.scenecraft.common.render.rendering.Channel;

import java.util.Collections;
import java.util.Map;

public class CubicGpuFrameCapturer extends GpuFrameCapturer<CubicGpuFrame, CubicGpuFrameCapturer.Data> {
    public enum Data implements CaptureData {
        LEFT, RIGHT, FRONT, BACK, TOP, BOTTOM
    }

    private final int frameSize;
    public CubicGpuFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, int frameSize) {
        super(worldRenderer, renderInfo);
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
    public Map<Channel, CubicGpuFrame> process() {
        renderInfo.updateForNextFrame();
        int frameId = framesDone++;
        CubicGpuFrame frame =  new CubicGpuFrame(renderFrame(frameId, Data.LEFT),
                renderFrame(frameId, Data.RIGHT),
                renderFrame(frameId, Data.FRONT),
                renderFrame(frameId, Data.BACK),
                renderFrame(frameId, Data.TOP),
                renderFrame(frameId, Data.BOTTOM));
        return Collections.singletonMap(Channel.RGBA, frame);
    }
}
