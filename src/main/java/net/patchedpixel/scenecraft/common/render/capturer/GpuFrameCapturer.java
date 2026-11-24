package net.patchedpixel.scenecraft.common.render.capturer;

import com.mojang.blaze3d.systems.RenderSystem;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.render.frame.GpuFrame;
import net.patchedpixel.scenecraft.common.render.rendering.Frame;
import net.patchedpixel.scenecraft.common.render.rendering.FrameCapturer;
import net.patchedpixel.scenecraft.common.render.utils.ByteBufferPool;
import net.patchedpixel.scenecraft.common.flamex.util.geometry.Size;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.nio.ByteBuffer;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.popMatrix;
import static net.patchedpixel.scenecraft.common.core.versions.MCVer.pushMatrix;
import static net.patchedpixel.scenecraft.common.core.versions.MCVer.resizeMainWindow;


public abstract class GpuFrameCapturer<F extends Frame, D extends CaptureData> implements FrameCapturer<F> {
    protected final WorldRenderer worldRenderer;
    protected final RenderInfo renderInfo;
    protected int framesDone;

    protected final Minecraft mc = MCVer.getMinecraft();

    public GpuFrameCapturer(WorldRenderer worldRenderer, RenderInfo renderInfo) {
        this.worldRenderer = worldRenderer;
        this.renderInfo = renderInfo;
    }

    protected Size frameSize() {
        return new Size(getFrameWidth(), getFrameHeight());
    }

    protected int getFrameWidth() {
        return renderInfo.getFrameSize().width();
    }

    protected int getFrameHeight() {
        return renderInfo.getFrameSize().height();
    }

    protected RenderTarget frameBuffer() {
        return mc.getMainRenderTarget();
    }

    protected void bindFrameBufferForRead() {
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, frameBuffer().frameBufferId);
    }

    protected void unbindFrameBufferForRead() {
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
    }

    @Override
    public boolean isDone() {
        return framesDone >= renderInfo.getTotalFrames();
    }

    protected GpuFrame renderFrame(int frameId, D captureData) {
        resizeMainWindow(mc, getFrameWidth(), getFrameHeight());

        pushMatrix();
        frameBuffer().bindWrite(true);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        worldRenderer.renderWorld(captureData);

        frameBuffer().unbindWrite();
        popMatrix();

        return captureFrame(frameId, captureData);
    }

    protected GpuFrame captureFrame(int frameId, D captureData) {
        ByteBuffer buffer = ByteBufferPool.allocate(getFrameWidth() * getFrameHeight() * 4);

        bindFrameBufferForRead();
        GlStateManager._pixelStore(GL11.GL_PACK_ALIGNMENT, 1);
        GlStateManager._readPixels(0, 0, getFrameWidth(), getFrameHeight(), GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buffer);
        unbindFrameBufferForRead();

        buffer.rewind();

        return new GpuFrame(frameId, new Size(getFrameWidth(), getFrameHeight()), 4, buffer);
    }

    @Override
    public void close() throws IOException {}
}
