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
package net.patchedpixel.scenecraft.common.gui.versions;

import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.function.BooleanSupplier;

/**
 * Presents the focus state of a jGui text element to vanilla as a {@link GuiEventListener}.
 * Vanilla only needs it as an identity token for text input / preedit handling, so one
 * instance must be kept per element for its whole lifetime.
 */
public class TextInputAdapter implements GuiEventListener {
    private final BooleanSupplier focused;

    public TextInputAdapter(BooleanSupplier focused) {
        this.focused = focused;
    }

    @Override
    public void setFocused(boolean focused) {
        // Focus is owned by the jGui element; vanilla never drives it for this adapter
    }

    @Override
    public boolean isFocused() {
        return focused.getAsBoolean();
    }
}
