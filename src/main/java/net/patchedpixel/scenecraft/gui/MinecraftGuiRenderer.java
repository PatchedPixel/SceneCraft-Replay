/*
 * This file is part of jGui API, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2016 johni0702 <https://github.com/johni0702>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.patchedpixel.scenecraft.gui;

import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.gui.utils.NonNull;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;

import com.mojang.renderpearl.api.textures.GpuTexture;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;

import static net.patchedpixel.scenecraft.core.versions.MCVer.*;

public class MinecraftGuiRenderer implements GuiRenderer {
    private final Minecraft mc = getMinecraft();

    private final GuiGraphicsExtractor context;

    private final Matrix3x2fStack matrixStack;

    @NonNull
    private final int scaledWidth = newScaledResolution(mc).getGuiScaledWidth();
    private final int scaledHeight = newScaledResolution(mc).getGuiScaledHeight();
    private final double scaleFactor = newScaledResolution(mc).getGuiScale();

    public MinecraftGuiRenderer(GuiGraphicsExtractor context) {
        this.context = context;
        this.matrixStack = context.pose();
    }

    @Override
    public int getGpuOffsetX() {
        return 0;
    }

    @Override
    public int getGpuOffsetY() {
        return 0;
    }

    @Override
    public GuiGraphicsExtractor getContext() {
        return context;
    }

    @Override
    public int getWidth() {
        return scaledWidth;
    }

    @Override
    public int getHeight() {
        return scaledHeight;
    }

    private List<ScissorBounds> scissorStack = new ArrayList<>();

    @Override
    public void pushScissor(int x, int y, int width, int height) {
        if (!scissorStack.isEmpty()) {
            context.disableScissor();
        }
        context.enableScissor(x, y, x + width, y + height);
        scissorStack.add(new ScissorBounds(x, y, width, height));
    }

    @Override
    public void popScissor() {
        scissorStack.remove(scissorStack.size() - 1);
        context.disableScissor();
        if (!scissorStack.isEmpty()) {
            ScissorBounds prev = scissorStack.get(scissorStack.size() - 1);
            context.enableScissor(prev.x, prev.y, prev.x + prev.width, prev.y + prev.height);
        }
    }

    private Identifier boundTexture;
    private GpuTexture boundTextureGpu;

    @Override
    public void bindTexture(Identifier location) {
        boundTexture = location;
        boundTextureGpu = null;
    }

    @Override
    public void bindTexture(AbstractTexture texture) {
        boundTexture = null;
        boundTextureGpu = texture.getTexture();
    }

    public void bindTexture(GpuTexture texture) {
        boundTexture = null;
        boundTextureGpu = texture;
    }

    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int width, int height) {
        drawTexturedRect(x, y, u, v, width, height, width, height, 256, 256);
    }

    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int width, int height, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        color(1, 1, 1);
        drawTexturedRect(x, x + width, y, y + height, u / (float) textureWidth, (u + uWidth) / (float) textureWidth, v / (float) textureHeight, (v + vHeight) / (float) textureHeight);
    }

    private void drawTexturedRect(int x1, int x2, int y1, int y2, float u1, float u2, float v1, float v2) {
        Identifier identifier;
        if (boundTexture != null) {
            identifier = boundTexture;
        } else {
            identifier = Identifier.fromNamespaceAndPath(SceneCraft.MOD_ID, "__tmp_texture__");
            mc.getTextureManager().register(identifier, new AbstractTexture() {
                { this.textureView = RenderSystem.getDevice().createTextureView(boundTextureGpu); }
                @Override public void close() {} // ignore later `destroyTexture` call
            });
        }

        context.blit(identifier, x1, y1, x2, y2, u1, u2, v1, v2);

        if (boundTexture == null) {
            mc.getTextureManager().release(identifier);
        }
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + height, color);
        color(1, 1, 1);
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        context.fillGradient(x, y, x + width, y + height, topLeftColor, bottomLeftColor);
    }

    @Override
    public int drawString(int x, int y, int color, String text) {
        return drawString(x, y, color, text, false);
    }

    @Override
    public int drawCenteredString(int x, int y, int color, String text) {
        return drawCenteredString(x, y, color, text, false);
    }

    @Override
    public int drawString(int x, int y, int color, String text, boolean shadow) {
        Font fontRenderer = MCVer.getFontRenderer();
        try {
            context.text(fontRenderer, text, x, y, color | 0xff000000, shadow);
            return x + fontRenderer.width(text);
        } finally {
            color(1, 1, 1);
        }
    }

    @Override
    public int drawCenteredString(int x, int y, int color, String text, boolean shadow) {
        Font fontRenderer = MCVer.getFontRenderer();
        x-=fontRenderer.width(text) / 2;
        return drawString(x, y, color, text, shadow);
    }

    private void color(float r, float g, float b) {}

    @Override
    public void invertColors(int right, int bottom, int left, int top) {
        if (left >= right || top >= bottom) return;

        context.fill(RenderPipelines.GUI_TEXT_HIGHLIGHT, left, top, right, bottom, 0xff0000ff);
    }
}
