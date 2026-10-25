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
package net.patchedpixel.scenecraft.gui.utils.geometry;

/**
 * A mutable 2D integer size.
 */
public final class Dimension implements ReadableDimension, WritableDimension {
    private int width;
    private int height;

    public Dimension() {
    }

    public Dimension(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Dimension(ReadableDimension dimension) {
        setSize(dimension);
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
    public void getSize(WritableDimension dest) {
        dest.setSize(width, height);
    }

    @Override
    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void setSize(ReadableDimension dimension) {
        this.width = dimension.getWidth();
        this.height = dimension.getHeight();
    }

    @Override
    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReadableDimension)) return false;
        ReadableDimension that = (ReadableDimension) o;
        return width == that.getWidth() && height == that.getHeight();
    }

    @Override
    public int hashCode() {
        return 31 * width + height;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[width=" + width + ", height=" + height + "]";
    }
}
