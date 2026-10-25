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

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

import static net.patchedpixel.scenecraft.core.versions.MCVer.*;

public class MinecraftGuiRenderer implements GuiRenderer {
    private final Minecraft mc = getMinecraft();

    private final GuiGraphics context;

    private final PoseStack matrixStack;

    private final int scaledWidth = newScaledResolution(mc).getGuiScaledWidth();
    private final int scaledHeight = newScaledResolution(mc).getGuiScaledHeight();

    public MinecraftGuiRenderer(GuiGraphics context) {
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
    public GuiGraphics getContext() {
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

    private final List<ScissorBounds> scissorStack = new ArrayList<>();

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

    private ResourceLocation boundTexture;
    private int boundTextureId = -1;

    @Override
    public void bindTexture(ResourceLocation location) {
        boundTexture = location;
        boundTextureId = -1;
    }

    @Override
    public void bindTexture(AbstractTexture texture) {
        bindTexture(texture.getId());
    }

    @Override
    public void bindTexture(int textureId) {
        boundTexture = null;
        boundTextureId = textureId;
    }

    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int width, int height) {
        drawTexturedRect(x, y, u, v, width, height, width, height, 256, 256);
    }

    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int width, int height, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        drawTexturedRect(x, x + width, y, y + height, u / (float) textureWidth, (u + uWidth) / (float) textureWidth, v / (float) textureHeight, (v + vHeight) / (float) textureHeight);
    }

    private void drawTexturedRect(int x1, int x2, int y1, int y2, float u1, float u2, float v1, float v2) {
        if (boundTexture != null) {
            RenderSystem.setShaderTexture(0, boundTexture);
        } else {
            RenderSystem.setShaderTexture(0, boundTextureId);
        }
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Matrix4f matrix = matrixStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, x1, y2, 0).uv(u1, v2).endVertex();
        buffer.vertex(matrix, x2, y2, 0).uv(u2, v2).endVertex();
        buffer.vertex(matrix, x2, y1, 0).uv(u2, v1).endVertex();
        buffer.vertex(matrix, x1, y1, 0).uv(u1, v1).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + height, color);
        RenderSystem.setShaderColor(1, 1, 1, 1);
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
            context.drawString(fontRenderer, text, x, y, color | 0xff000000, shadow);
            return x + fontRenderer.width(text);
        } finally {
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    @Override
    public int drawCenteredString(int x, int y, int color, String text, boolean shadow) {
        Font fontRenderer = MCVer.getFontRenderer();
        x -= fontRenderer.width(text) / 2;
        return drawString(x, y, color, text, shadow);
    }

    @Override
    public void invertColors(int right, int bottom, int left, int top) {
        if (left >= right || top >= bottom) return;

        Matrix4f matrix = matrixStack.last().pose();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(0, 0, 1, 1);
        RenderSystem.enableColorLogicOp();
        RenderSystem.logicOp(GlStateManager.LogicOp.OR_REVERSE);
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        buffer.vertex(matrix, left, bottom, 0).endVertex();
        buffer.vertex(matrix, right, bottom, 0).endVertex();
        buffer.vertex(matrix, right, top, 0).endVertex();
        buffer.vertex(matrix, left, top, 0).endVertex();
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.disableColorLogicOp();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }
}
