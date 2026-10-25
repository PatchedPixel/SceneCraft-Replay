package net.patchedpixel.scenecraft.render.utils;

import com.mojang.blaze3d.platform.GlStateManager;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;

import java.nio.ByteBuffer;

public class PixelBufferObject implements AutoCloseable {
    private final int handle;

    public PixelBufferObject(long size) {
        this.handle = GlStateManager._glGenBuffers();
        bind();
        GlStateManager._glBufferData(GL21.GL_PIXEL_PACK_BUFFER, size, GL15.GL_STREAM_READ);
        unbind();
    }

    public void bind() {
        GlStateManager._glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, handle);
    }

    public void unbind() {
        GlStateManager._glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);
    }

    public ByteBuffer map() {
        ByteBuffer buffer = GlStateManager._glMapBuffer(GL21.GL_PIXEL_PACK_BUFFER, GL15.GL_READ_ONLY);
        if (buffer == null) {
            throw new IllegalStateException("Failed to map pixel buffer object");
        }
        return buffer;
    }

    public void unmap() {
        GlStateManager._glUnmapBuffer(GL21.GL_PIXEL_PACK_BUFFER);
    }

    @Override
    public void close() {
        GlStateManager._glDeleteBuffers(handle);
    }
}
