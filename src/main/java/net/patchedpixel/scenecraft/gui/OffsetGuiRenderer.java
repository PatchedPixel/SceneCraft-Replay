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

import net.patchedpixel.scenecraft.gui.utils.NonNull;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadablePoint;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;

public class OffsetGuiRenderer implements GuiRenderer {
    @NonNull
    private final GuiRenderer renderer;

    private final int positionX;
    private final int positionY;

    private final int width;
    private final int height;

    private final boolean strict;

    /**
     * Creates a new strict offset jgui renderer with the same settings as the supplied one.
     *
     * @param renderer The renderer to copy from
     */
    public OffsetGuiRenderer(OffsetGuiRenderer renderer) {
        this(renderer.renderer, renderer.positionX, renderer.positionY, renderer.width, renderer.height, true);
    }

    /**
     * Creates a new offset GUI renderer.
     *
     * @param renderer The parent renderer
     * @param positionX The X position of this renderer within the parent
     * @param positionY The Y position of this renderer within the parent
     * @param width The width of the drawable area
     * @param height The height of the drawable area
     */
    public OffsetGuiRenderer(GuiRenderer renderer, int positionX, int positionY, int width, int height) {
        this(renderer, positionX, positionY, width, height, true);
    }

    /**
     * Creates a new offset GUI renderer.
     *
     * @param renderer The parent renderer
     * @param positionX The X position of this renderer within the parent
     * @param positionY The Y position of this renderer within the parent
     * @param width The width of the drawable area
     * @param height The height of the drawable area
     * @param strict Whether drawing should be forced to be within the drawable area
     */
    public OffsetGuiRenderer(GuiRenderer renderer, int positionX, int positionY, int width, int height, boolean strict) {
        this.renderer = renderer;
        this.positionX = positionX;
        this.positionY = positionY;
        this.width = width;
        this.height = height;
        this.strict = strict;
    }

    /**
     * Creates a new strict offset GUI renderer.
     *
     * @param renderer The parent renderer
     * @param position The position of this renderer within the parent
     * @param size The size of the drawable area
     */
    public OffsetGuiRenderer(GuiRenderer renderer, ReadablePoint position, ReadableDimension size) {
        this(renderer, position, size, true);
    }

    /**
     * Creates a new offset GUI renderer.
     *
     * @param renderer The parent renderer
     * @param position The position of this renderer within the parent
     * @param size The size of the drawable area
     * @param strict Whether drawing should be forced to be within the drawable area
     */
    public OffsetGuiRenderer(GuiRenderer renderer, ReadablePoint position, ReadableDimension size, boolean strict) {
        this(renderer, position.getX(), position.getY(), size.getWidth(), size.getHeight(), strict);
    }

    @Override
    public int getGpuOffsetX() {
        return renderer.getGpuOffsetX() + positionX;
    }

    @Override
    public int getGpuOffsetY() {
        return renderer.getGpuOffsetY() + positionY;
    }

    @Override
    public GuiGraphics getContext() {
        return renderer.getContext();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void pushScissor(int x, int y, int width, int height) {
        if (!strict) {
            renderer.pushScissor(
                    x + positionX, y + positionY, width, height
            );

            return;
        }

        int left = Math.max(0, x);
        int top = Math.max(0, y);
        int right = Math.min(x + width, this.width);
        int bottom = Math.min(y + height, this.height);

        left += positionX;
        top += positionY;
        right += positionX;
        bottom += positionY;

        right = Math.max(left, right);
        bottom = Math.max(top, bottom);

        renderer.pushScissor(
                left, top, right - left, bottom - top
        );
    }

    @Override
    public void popScissor() {
        renderer.popScissor();
    }

    public void startUsing() {
        pushScissor(0, 0, width, height);
    }

    public void stopUsing() {
        popScissor();
    }

    @Override
    public void bindTexture(ResourceLocation location) {
        renderer.bindTexture(location);
    }

    @Override
    public void bindTexture(AbstractTexture texture) {
        renderer.bindTexture(texture);
    }

    @Override
    public void bindTexture(int textureId) {
        renderer.bindTexture(textureId);
    }

    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int width, int height) {
        renderer.drawTexturedRect(x + positionX, y + positionY, u, v, width, height);
    }

    @Override
    public void drawTexturedRect(int x, int y, int u, int v, int width, int height, int uWidth, int vHeight, int textureWidth, int textureHeight) {
        renderer.drawTexturedRect(x + positionX, y + positionY, u, v, width, height, uWidth, vHeight, textureWidth, textureHeight);
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int color) {
        renderer.drawRect(x + positionX, y + positionY, width, height, color);
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor) {
        renderer.drawRect(x + positionX, y + positionY, width, height, topLeftColor, topRightColor, bottomLeftColor, bottomRightColor);
    }

    @Override
    public int drawString(int x, int y, int color, String text) {
        return renderer.drawString(x + positionX, y + positionY, color, text) - positionX;
    }

    @Override
    public int drawCenteredString(int x, int y, int color, String text) {
        return renderer.drawCenteredString(x + positionX, y + positionY, color, text) - positionX;
    }

    @Override
    public int drawString(int x, int y, int color, String text, boolean shadow) {
        return renderer.drawString(x + positionX, y + positionY, color, text, shadow) - positionX;
    }

    @Override
    public int drawCenteredString(int x, int y, int color, String text, boolean shadow) {
        return renderer.drawCenteredString(x + positionX, y + positionY, color, text, shadow) - positionX;
    }

    @Override
    public void invertColors(int right, int bottom, int left, int top) {
        renderer.invertColors(right + positionX, bottom + positionY, left + positionX, top + positionY);
    }
}
