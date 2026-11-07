package net.patchedpixel.scenecraft.common.render.rendering;

import net.patchedpixel.scenecraft.common.render.RenderSettings;
import net.patchedpixel.scenecraft.common.render.capturer.CubicPboGpuFrameCapturer;
import net.patchedpixel.scenecraft.common.render.capturer.RenderInfo;
import net.patchedpixel.scenecraft.common.render.capturer.SimplePboGpuFrameCapturer;
import net.patchedpixel.scenecraft.common.render.capturer.StereoscopicPboGpuFrameCapturer;
import net.patchedpixel.scenecraft.common.render.capturer.WorldRenderer;
import net.patchedpixel.scenecraft.common.render.frame.CubicGpuFrame;
import net.patchedpixel.scenecraft.common.render.frame.ODSGpuFrame;
import net.patchedpixel.scenecraft.common.render.frame.GpuFrame;
import net.patchedpixel.scenecraft.common.render.frame.BitmapFrame;
import net.patchedpixel.scenecraft.common.render.frame.StereoscopicGpuFrame;
import net.patchedpixel.scenecraft.common.render.hooks.EntityRendererHandler;
import net.patchedpixel.scenecraft.common.render.processor.CubicToBitmapProcessor;
import net.patchedpixel.scenecraft.common.render.processor.EquirectangularToBitmapProcessor;
import net.patchedpixel.scenecraft.common.render.processor.ODSToBitmapProcessor;
import net.patchedpixel.scenecraft.common.render.processor.GpuToBitmapProcessor;
import net.patchedpixel.scenecraft.common.render.processor.StereoscopicToBitmapProcessor;

public class Pipelines {
    public static Pipeline newPipeline(RenderSettings.RenderMethod method, RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        switch(method) {
            case DEFAULT:
                return newDefaultPipeline(renderInfo, consumer);
            case STEREOSCOPIC:
                return newStereoscopicPipeline(renderInfo, consumer);
            case CUBIC:
                return newCubicPipeline(renderInfo, consumer);
            case EQUIRECTANGULAR:
                return newEquirectangularPipeline(renderInfo, consumer);
            case ODS:
                return newODSPipeline(renderInfo, consumer);
        }
        throw new UnsupportedOperationException("Unknown method: " + method);
    }

    public static Pipeline<GpuFrame, BitmapFrame> newDefaultPipeline(RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);
        FrameCapturer<GpuFrame> capturer;
        capturer = new SimplePboGpuFrameCapturer(worldRenderer, renderInfo);
        return new Pipeline<>(worldRenderer, capturer, new GpuToBitmapProcessor(), consumer);
    }

    public static Pipeline<StereoscopicGpuFrame, BitmapFrame> newStereoscopicPipeline(RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);
        FrameCapturer<StereoscopicGpuFrame> capturer;
        capturer = new StereoscopicPboGpuFrameCapturer(worldRenderer, renderInfo);
        return new Pipeline<>(worldRenderer, capturer, new StereoscopicToBitmapProcessor(), consumer);
    }

    public static Pipeline<CubicGpuFrame, BitmapFrame> newCubicPipeline(RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);
        FrameCapturer<CubicGpuFrame> capturer;
        capturer = new CubicPboGpuFrameCapturer(worldRenderer, renderInfo, settings.getVideoWidth() / 4);
        return new Pipeline<>(worldRenderer, capturer, new CubicToBitmapProcessor(), consumer);
    }

    public static Pipeline<CubicGpuFrame, BitmapFrame> newEquirectangularPipeline(RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);

        EquirectangularToBitmapProcessor processor = new EquirectangularToBitmapProcessor(settings.getVideoWidth(),
                settings.getVideoHeight(), settings.getSphericalFovX());

        FrameCapturer<CubicGpuFrame> capturer;
        capturer = new CubicPboGpuFrameCapturer(worldRenderer, renderInfo, processor.getFrameSize());
        return new Pipeline<>(worldRenderer, capturer, processor, consumer);
    }

    public static Pipeline<ODSGpuFrame, BitmapFrame> newODSPipeline(RenderInfo renderInfo, FrameConsumer<BitmapFrame> consumer) {
        RenderSettings settings = renderInfo.getRenderSettings();
        WorldRenderer worldRenderer = new EntityRendererHandler(settings, renderInfo);

        ODSToBitmapProcessor processor = new ODSToBitmapProcessor(settings.getVideoWidth(),
                settings.getVideoHeight(), settings.getSphericalFovX());

        if(!RenderSettings.RenderMethod.ODS.isSupported()) {
            throw new UnsupportedOperationException("ODS rendering requires Iris on the OpenGL backend");
        }
        FrameCapturer<ODSGpuFrame> capturer =
                new net.patchedpixel.scenecraft.common.render.capturer.IrisODSFrameCapturer(worldRenderer, renderInfo, processor.getFrameSize());
        return new Pipeline<>(worldRenderer, capturer, processor, consumer);
    }
}
