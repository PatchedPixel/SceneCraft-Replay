/*
 * This file is part of jGui API, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2025 johni0702 <https://github.com/johni0702>
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
package net.patchedpixel.scenecraft.gui.utils.color;

/**
 * A read-only view of an 8-bit-per-channel RGBA color.
 */
public interface ReadableColor {
    ReadableColor RED = new Color(255, 0, 0);
    ReadableColor GREEN = new Color(0, 255, 0);
    ReadableColor WHITE = new Color(255, 255, 255);
    ReadableColor BLACK = new Color(0, 0, 0);
    ReadableColor LTGREY = new Color(192, 192, 192);
    ReadableColor GREY = new Color(128, 128, 128);
    ReadableColor DKGREY = new Color(64, 64, 64);

    /** Red channel, 0-255. */
    int getRed();

    /** Green channel, 0-255. */
    int getGreen();

    /** Blue channel, 0-255. */
    int getBlue();

    /** Alpha channel, 0-255 (255 is fully opaque). */
    int getAlpha();

    /**
     * Packs this color as 0xAARRGGBB, which is the format used by vanilla GUI drawing.
     */
    default int toARGB() {
        return (getAlpha() & 0xFF) << 24 | (getRed() & 0xFF) << 16 | (getGreen() & 0xFF) << 8 | (getBlue() & 0xFF);
    }

    /**
     * Packs this color as 0x00RRGGBB, dropping the alpha channel.
     */
    default int toRGB() {
        return toARGB() & 0x00FFFFFF;
    }
}
