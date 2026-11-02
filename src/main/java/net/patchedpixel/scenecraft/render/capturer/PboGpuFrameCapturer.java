package net.patchedpixel.scenecraft.render.capturer;

import net.patchedpixel.scenecraft.render.frame.GpuFrame;
import net.patchedpixel.scenecraft.render.rendering.Channel;
import net.patchedpixel.scenecraft.render.rendering.Frame;
import net.patchedpixel.scenecraft.render.utils.ByteBufferPool;

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
        int bufferSize = framePixels * (4 /* bgra */ + (withDepth ? 4 /* float */ : 0)) * data.length;
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

    private F readFromPbo(ByteBuffer pboBuffer, int bytesPerPixel, boolean swapRB) {
        GpuFrame[] frames = new GpuFrame[data.length];
        int frameBufferSize = getFrameWidth() * getFrameHeight() * bytesPerPixel;
        for(int i = 0; i < frames.length; i++) {
            ByteBuffer frameBuffer = ByteBufferPool.allocate(frameBufferSize);
            pboBuffer.limit(pboBuffer.position() + frameBufferSize);
            if(swapRB) {
                for(int j = 0; j < frameBufferSize; j += 4) {
                    byte r = pboBuffer.get();
                    byte g = pboBuffer.get();
                    byte b = pboBuffer.get();
                    byte a = pboBuffer.get();
                    frameBuffer.put(b);
                    frameBuffer.put(g);
                    frameBuffer.put(r);
                    frameBuffer.put(a);
                }
            } else {
                frameBuffer.put(pboBuffer);
            }
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
                channels.put(Channel.BRGA, readFromPbo(view.data(), 4, true));
                if(withDepth) {
                    channels.put(Channel.DEPTH, readFromPbo(view.data(), 4, false));
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
