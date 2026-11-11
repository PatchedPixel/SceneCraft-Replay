package net.patchedpixel.scenecraft.common.render.capturer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.render.frame.GpuFrame;
import net.patchedpixel.scenecraft.common.render.rendering.Frame;
import net.patchedpixel.scenecraft.common.render.rendering.FrameCapturer;
import net.patchedpixel.scenecraft.common.render.utils.ByteBufferPool;
import net.patchedpixel.scenecraft.common.gui.utils.geometry.Dimension;
import net.patchedpixel.scenecraft.common.gui.utils.geometry.ReadableDimension;
import net.patchedpixel.scenecraft.common.gui.utils.geometry.WritableDimension;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;

import java.io.IOException;
import java.nio.ByteBuffer;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.popMatrix;
import static net.patchedpixel.scenecraft.common.core.versions.MCVer.pushMatrix;
import static net.patchedpixel.scenecraft.common.core.versions.MCVer.resizeMainWindow;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import org.joml.Vector4f;

public abstract class GpuFrameCapturer<F extends Frame, D extends CaptureData> implements FrameCapturer<F> {
    protected final WorldRenderer worldRenderer;
    protected final RenderInfo renderInfo;
    protected int framesDone;
    private RenderTarget frameBuffer;

    protected final Minecraft mc = MCVer.getMinecraft();

    public GpuFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        this.worldRenderer = worldRenderer;
        this.renderInfo = renderInfo;
    }

    protected final ReadableDimension frameSize = new ReadableDimension() {
        @Override
        public int getWidth() {
            return getFrameWidth();
        }

        @Override
        public int getHeight() {
            return getFrameHeight();
        }

        @Override
        public void getSize(WritableDimension dest) {
            dest.setSize(getWidth(), getHeight());
        }
    };

    protected int getFrameWidth() {
        return renderInfo.getFrameSize().getWidth();
    }

    protected int getFrameHeight() {
        return renderInfo.getFrameSize().getHeight();
    }

    protected RenderTarget frameBuffer() {
        if(frameBuffer == null) {
            frameBuffer = mc.gameRenderer.mainRenderTarget();
        }
        return frameBuffer;
    }

    @Override
    public boolean isDone() {
        return framesDone >= renderInfo.getTotalFrames();
    }

    protected GpuFrame renderFrame(int frameId, D captureData) {
        resizeMainWindow(mc, getFrameWidth(), getFrameHeight());

        pushMatrix();

        RenderSystem.getDevice()
                .createCommandEncoder()
                .clearColorAndDepthTextures(mc.gameRenderer.mainRenderTarget().getColorTexture(), new Vector4f(), mc.gameRenderer.mainRenderTarget().getDepthTexture(), 0);

        worldRenderer.renderWorld(captureData);

        popMatrix();

        RenderSystem.getDynamicUniforms().reset();
        mc.levelRenderer.endFrame();
        RenderSystem.getDevice().createCommandEncoder().submit();

        return captureFrame(frameId, captureData);
    }

    protected GpuFrame captureFrame(int frameId, D captureData) {
        ByteBuffer buffer = ByteBufferPool.allocate(getFrameWidth() * getFrameHeight() * 4);
        GpuDevice device = RenderSystem.getDevice();
        try (GpuBuffer gpuBuffer = device.createBuffer(null, GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, (long) getFrameWidth() * getFrameHeight() * 4)) {
            device.createCommandEncoder().copyTextureToBuffer(frameBuffer().getColorTexture(), gpuBuffer, 0, () -> {}, 0);
            try (GpuBufferSlice.MappedView view = gpuBuffer.map(true, false)) {
                buffer.put(view.data());
            }
        }
        buffer.rewind();

        return new GpuFrame(frameId, new Dimension(getFrameWidth(), getFrameHeight()), 4, buffer);
    }

    @Override
    public void close() throws IOException {}
}
