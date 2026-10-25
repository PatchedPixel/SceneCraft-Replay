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
 * A mutable 8-bit-per-channel RGBA color. Channel values are always clamped to 0-255.
 */
public final class Color implements ReadableColor {
    private int red;
    private int green;
    private int blue;
    private int alpha = 255;

    public Color() {
    }

    public Color(int r, int g, int b) {
        setColor(r, g, b, 255);
    }

    public Color(int r, int g, int b, int a) {
        setColor(r, g, b, a);
    }

    public Color(ReadableColor color) {
        setColor(color);
    }

    private static int clamp(int value) {
        return value < 0 ? 0 : Math.min(value, 255);
    }

    @Override
    public int getRed() {
        return red;
    }

    @Override
    public int getGreen() {
        return green;
    }

    @Override
    public int getBlue() {
        return blue;
    }

    @Override
    public int getAlpha() {
        return alpha;
    }

    public void setRed(int red) {
        this.red = clamp(red);
    }

    public void setGreen(int green) {
        this.green = clamp(green);
    }

    public void setBlue(int blue) {
        this.blue = clamp(blue);
    }

    public void setAlpha(int alpha) {
        this.alpha = clamp(alpha);
    }

    public void setColor(int r, int g, int b) {
        setColor(r, g, b, 255);
    }

    public void setColor(int r, int g, int b, int a) {
        this.red = clamp(r);
        this.green = clamp(g);
        this.blue = clamp(b);
        this.alpha = clamp(a);
    }

    public void setColor(ReadableColor color) {
        setColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    /**
     * Sets the RGB channels from a hue/saturation/brightness triple (each in the range 0-1).
     * The alpha channel is left untouched.
     */
    public void fromHSB(float hue, float saturation, float brightness) {
        if (saturation <= 0f) {
            int v = Math.round(brightness * 255f);
            setRed(v);
            setGreen(v);
            setBlue(v);
            return;
        }
        float h = (hue - (float) Math.floor(hue)) * 6f;
        float f = h - (float) Math.floor(h);
        float p = brightness * (1f - saturation);
        float q = brightness * (1f - saturation * f);
        float t = brightness * (1f - saturation * (1f - f));
        float r;
        float g;
        float b;
        switch ((int) h) {
            case 0:
                r = brightness;
                g = t;
                b = p;
                break;
            case 1:
                r = q;
                g = brightness;
                b = p;
                break;
            case 2:
                r = p;
                g = brightness;
                b = t;
                break;
            case 3:
                r = p;
                g = q;
                b = brightness;
                break;
            case 4:
                r = t;
                g = p;
                b = brightness;
                break;
            default:
                r = brightness;
                g = p;
                b = q;
                break;
        }
        setRed(Math.round(r * 255f));
        setGreen(Math.round(g * 255f));
        setBlue(Math.round(b * 255f));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReadableColor)) return false;
        ReadableColor that = (ReadableColor) o;
        return red == that.getRed() && green == that.getGreen() && blue == that.getBlue() && alpha == that.getAlpha();
    }

    @Override
    public int hashCode() {
        return toARGB();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[r=" + red + ", g=" + green + ", b=" + blue + ", a=" + alpha + "]";
    }
}
