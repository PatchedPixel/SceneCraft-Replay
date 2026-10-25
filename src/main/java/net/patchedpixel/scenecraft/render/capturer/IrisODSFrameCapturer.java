package net.patchedpixel.scenecraft.render.capturer;

import net.patchedpixel.scenecraft.render.RenderSettings;
import net.patchedpixel.scenecraft.render.frame.CubicGpuFrame;
import net.patchedpixel.scenecraft.render.frame.ODSGpuFrame;
import net.patchedpixel.scenecraft.render.frame.GpuFrame;
import net.patchedpixel.scenecraft.render.rendering.Channel;
import net.patchedpixel.scenecraft.render.rendering.FrameCapturer;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.config.IrisConfig;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IrisODSFrameCapturer implements FrameCapturer<ODSGpuFrame> {
    public static final String SHADER_PACK_NAME = "assets/scenecraft/iris/ods";
    public static IrisODSFrameCapturer INSTANCE;
    private final CubicPboGpuFrameCapturer left, right;
    private final String prevShaderPack;
    private final boolean prevShadersEnabled;
    private int direction;
    private boolean isLeftEye;

    public IrisODSFrameCapturer(WorldRenderer worldRenderer, final RenderInfo renderInfo, int frameSize) {
        RenderInfo fakeInfo = new RenderInfo() {
            private int call;
            private float partialTicks;
            @Override
            public ReadableDimension getFrameSize() {
                return renderInfo.getFrameSize();
            }

            @Override
            public int getFramesDone() {
                return renderInfo.getFramesDone();
            }

            @Override
            public int getTotalFrames() {
                return renderInfo.getTotalFrames();
            }

            @Override
            public float updateForNextFrame() {
                if (call++ % 2 == 0) {
                    partialTicks = renderInfo.updateForNextFrame();
                }
                return partialTicks;
            }

            @Override
            public RenderSettings getRenderSettings() {
                return renderInfo.getRenderSettings();
            }
        };
        left = new CubicStereoFrameCapturer(worldRenderer, fakeInfo, frameSize);
        right = new CubicStereoFrameCapturer(worldRenderer, fakeInfo, frameSize);

        INSTANCE = this;
        IrisConfig irisConfig = Iris.getIrisConfig();
        prevShaderPack = irisConfig.getShaderPackName().orElse(null);
        prevShadersEnabled = irisConfig.areShadersEnabled();
        setShaderPack(SHADER_PACK_NAME, true);
    }

    private static void setShaderPack(String name, boolean enabled) {
        IrisConfig irisConfig = Iris.getIrisConfig();
        irisConfig.setShaderPackName(name);
        irisConfig.setShadersEnabled(enabled);
        try {
            irisConfig.save();
            Iris.reload();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getDirection() {
        return direction;
    }

    public boolean isLeftEye() {
        return isLeftEye;
    }

    @Override
    public boolean isDone() {
        return left.isDone() && right.isDone();
    }

    @Override
    public Map<Channel, ODSGpuFrame> process() {
        isLeftEye = true;
        Map<Channel, CubicGpuFrame> leftChannels = left.process();
        isLeftEye = false;
        Map<Channel, CubicGpuFrame> rightChannels = right.process();

        if (leftChannels != null && rightChannels != null) {
            Map<Channel, ODSGpuFrame> result = new HashMap<>();
            for (Channel channel : Channel.values()) {
                CubicGpuFrame leftFrame = leftChannels.get(channel);
                CubicGpuFrame rightFrame = rightChannels.get(channel);
                if (leftFrame != null && rightFrame != null) {
                    result.put(channel, new ODSGpuFrame(leftFrame, rightFrame));
                }
            }

            return result;
        }

        return null;
    }

    @Override
    public void close() throws IOException {
        left.close();
        right.close();
        INSTANCE = null;
        setShaderPack(prevShaderPack, prevShadersEnabled);
    }

    private class CubicStereoFrameCapturer extends CubicPboGpuFrameCapturer {
        public CubicStereoFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, int frameSize) {
            super(worldRenderer, renderInfo, frameSize);
        }

        @Override
        protected GpuFrame renderFrame(int frameId, CubicGpuFrameCapturer.Data captureData) {
            direction = captureData.ordinal();
            return super.renderFrame(frameId, captureData);
        }
    }
}
