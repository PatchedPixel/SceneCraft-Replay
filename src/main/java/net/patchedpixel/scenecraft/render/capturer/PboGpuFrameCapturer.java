package net.patchedpixel.scenecraft.render.capturer;

import net.patchedpixel.scenecraft.render.frame.GpuFrame;
import net.patchedpixel.scenecraft.render.rendering.Channel;
import net.patchedpixel.scenecraft.render.rendering.Frame;
import net.patchedpixel.scenecraft.render.utils.ByteBufferPool;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;


import net.patchedpixel.scenecraft.render.utils.PixelBufferObject;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public abstract class PboGpuFrameCapturer<F extends Frame, D extends Enum<D> & CaptureData> extends GpuFrameCapturer<F, D> {
    private final boolean withDepth;
    private final D[] data;
    private PixelBufferObject pbo, otherPBO;

    public PboGpuFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo, Class<D> type, int framePixels) {
        super(worldRenderer, renderInfo);

        withDepth = renderInfo.getRenderSettings().isDepthMap();
        data = type.getEnumConstants();
        int bufferSize = framePixels * (4 /* bgra */ + (withDepth ? 4 /* float */ : 0)) * data.length;
        pbo = new PixelBufferObject(bufferSize);
        otherPBO = new PixelBufferObject(bufferSize);
    }

    protected abstract F create(GpuFrame[] from);

    private void swapPBOs() {
        PixelBufferObject old = pbo;
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
        for (int i = 0; i < frames.length; i++) {
            ByteBuffer frameBuffer = ByteBufferPool.allocate(frameBufferSize);
            pboBuffer.limit(pboBuffer.position() + frameBufferSize);
            if (swapRB) {
                for (int j = 0; j < frameBufferSize; j += 4) {
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

        if (framesDone > 1) {
            // Read pbo to memory
            pbo.bind();
            ByteBuffer pboBuffer = pbo.map();
            channels = new HashMap<>();
            channels.put(Channel.BRGA, readFromPbo(pboBuffer, 4, true));
            if (withDepth) {
                channels.put(Channel.DEPTH, readFromPbo(pboBuffer, 4, false));
            }
            pbo.unmap();
            pbo.unbind();
        }

        if (framesDone < renderInfo.getTotalFrames()) {
            renderInfo.updateForNextFrame();
            // Then fill it again
            for (D data : this.data) {
                renderFrame(framesDone, data);
            }
        }

        framesDone++;
        swapPBOs();
        return channels;
    }

    @Override
    protected GpuFrame captureFrame(int frameId, D captureData) {
        int offset = captureData.ordinal() * getFrameWidth() * getFrameHeight() * 4;
        pbo.bind();
        bindFrameBufferForRead();
        GlStateManager._pixelStore(GL11.GL_PACK_ALIGNMENT, 1);
        GlStateManager._readPixels(0, 0, getFrameWidth(), getFrameHeight(), GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, (long) offset);
        if (withDepth) {
            offset += data.length * getFrameWidth() * getFrameHeight() * 4;
            GlStateManager._readPixels(0, 0, getFrameWidth(), getFrameHeight(), GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (long) offset);
        }
        unbindFrameBufferForRead();
        pbo.unbind();
        return null;
    }

    @Override
    public void close() throws IOException {
        super.close();
        pbo.close();
        otherPBO.close();
    }
}
