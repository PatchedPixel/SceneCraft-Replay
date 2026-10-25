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

import net.patchedpixel.scenecraft.gui.utils.color.ReadableColor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;

public interface GuiRenderer {
    int getGpuOffsetX();
    int getGpuOffsetY();

    GuiGraphics getContext();

    int getWidth();
    int getHeight();

    void pushScissor(int x, int y, int width, int height);
    void popScissor();

    void bindTexture(ResourceLocation location);
    void bindTexture(AbstractTexture texture);
    void bindTexture(int textureId);

    void drawTexturedRect(int x, int y, int u, int v, int width, int height);

    void drawTexturedRect(int x, int y, int u, int v, int width, int height, int uWidth, int vHeight, int textureWidth, int textureHeight);

    void drawRect(int x, int y, int width, int height, int color);

    void drawRect(int x, int y, int width, int height, int topLeftColor, int topRightColor, int bottomLeftColor, int bottomRightColor);

    int drawString(int x, int y, int color, String text);

    int drawString(int x, int y, int color, String text, boolean shadow);

    int drawCenteredString(int x, int y, int color, String text);

    int drawCenteredString(int x, int y, int color, String text, boolean shadow);

    void invertColors(int right, int bottom, int left, int top);


    default void drawRect(int x, int y, int width, int height, ReadableColor color) {
        drawRect(x, y, width, height, color.toARGB());
    }

    default void drawRect(int x, int y, int width, int height,
                          ReadableColor topLeftColor, ReadableColor topRightColor,
                          ReadableColor bottomLeftColor, ReadableColor bottomRightColor) {
        drawRect(x, y, width, height, topLeftColor.toARGB(), topRightColor.toARGB(),
                bottomLeftColor.toARGB(), bottomRightColor.toARGB());
    }

    default int drawString(int x, int y, ReadableColor color, String text) {
        return drawString(x, y, color.toRGB(), text);
    }

    default int drawString(int x, int y, ReadableColor color, String text, boolean shadow) {
        return drawString(x, y, color.toRGB(), text, shadow);
    }

    default int drawCenteredString(int x, int y, ReadableColor color, String text) {
        return drawCenteredString(x, y, color.toRGB(), text);
    }

    default int drawCenteredString(int x, int y, ReadableColor color, String text, boolean shadow) {
        return drawCenteredString(x, y, color.toRGB(), text, shadow);
    }
}
