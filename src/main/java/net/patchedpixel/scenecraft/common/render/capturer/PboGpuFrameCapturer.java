package net.patchedpixel.scenecraft.common.render.capturer;

import net.patchedpixel.scenecraft.common.render.frame.GpuFrame;
import net.patchedpixel.scenecraft.common.render.rendering.Channel;
import net.patchedpixel.scenecraft.common.render.rendering.Frame;
import net.patchedpixel.scenecraft.common.render.utils.ByteBufferPool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;

public abstract class PboGpuFrameCapturer<F extends Frame, D extends Enum<D> & CaptureData> extends GpuFrameCapturer<F, D> {
    private final boolean withDepth;
    private final D[] data;
    private GpuBuffer pbo, otherPBO;

    public PboGpuFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, Class<D> type, int framePixels) {
        super(worldRenderer, renderInfo);

        withDepth = renderInfo.getRenderSettings().isDepthMap();
        data = type.getEnumConstants();
        int bufferSize = framePixels * (4 /* rgba */ + (withDepth ? 4 /* float */ : 0)) * data.length;
        pbo = RenderSystem.getDevice().createBuffer(null, GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, bufferSize);
        otherPBO = RenderSystem.getDevice().createBuffer(null, GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, bufferSize);
    }

    protected abstract F create(GpuFrame[] from);

    private void swapPBOs() {
        GpuBuffer old = pbo;
        pbo = otherPBO;
        otherPBO = old;
    }

    @Override
    public boolean isDone() {
        return framesDone >= renderInfo.getTotalFrames() + 2;
    }

    private F readFromPbo(ByteBuffer pboBuffer, int bytesPerPixel) {
        GpuFrame[] frames = new GpuFrame[data.length];
        int frameBufferSize = getFrameWidth() * getFrameHeight() * bytesPerPixel;
        for(int i = 0; i < frames.length; i++) {
            ByteBuffer frameBuffer = ByteBufferPool.allocate(frameBufferSize);
            pboBuffer.limit(pboBuffer.position() + frameBufferSize);
            // The pixels stay in the render target's native RGBA order, ffmpeg is told to expect exactly that.
            frameBuffer.put(pboBuffer);
            frameBuffer.rewind();
            frames[i] = new GpuFrame(framesDone - 2, frameSize, bytesPerPixel, frameBuffer);
        }

        return create(frames);
    }

    @Override
    public Map<Channel, F> process() {
        Map<Channel, F> channels = null;

        if(framesDone > 1) {
            // Read pbo to memory
            try (GpuBufferSlice.MappedView view = pbo.map(true, false)) {
                channels = new HashMap<>();
                channels.put(Channel.RGBA, readFromPbo(view.data(), 4));
                if(withDepth) {
                    channels.put(Channel.DEPTH, readFromPbo(view.data(), 4));
                }
            }
        }

        if(framesDone < renderInfo.getTotalFrames()) {
            renderInfo.updateForNextFrame();
            // Then fill it again
            for(D data : this.data) {
                renderFrame(framesDone, data);
            }
        } else {
            RenderSystem.getDevice().createCommandEncoder().submit();
        }

        framesDone++;
        swapPBOs();

        return channels;
    }

    @Override
    protected GpuFrame captureFrame(int frameId, D captureData) {
        int offset = captureData.ordinal() * getFrameWidth() * getFrameHeight() * 4;
        CommandEncoder cmd = RenderSystem.getDevice().createCommandEncoder();
        cmd.copyTextureToBuffer(frameBuffer().getColorTexture(), pbo, offset, () -> {}, 0);
        if(withDepth) {
            offset += data.length * getFrameWidth() * getFrameHeight() * 4;
            cmd.copyTextureToBuffer(frameBuffer().getDepthTexture(), pbo, offset, () -> {}, 0);
        }

        return null;
    }

    @Override
    public void close() throws IOException {
        super.close();
        pbo.close();
        otherPBO.close();
    }
}
