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
package net.patchedpixel.scenecraft.common.gui.function;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;

public class KeyInput implements InputWithModifiers {
    /** Physical key position (SDL scancode). Independent of the keyboard layout. */
    public int key;
    /** Logical key (SDL keycode). Follows the active keyboard layout. */
    public int keycode;
    public int modifiers;

    public KeyInput(int key, int keycode, int modifiers) {
        this.key = key;
        this.keycode = keycode;
        this.modifiers = modifiers;
    }

    public KeyInput(KeyEvent mc) {
        this(mc.key(), mc.keycode(), mc.modifiers());
    }
    public KeyEvent toMC() {
        return new KeyEvent(key, keycode, modifiers);
    }

    @Override
    public int modifiers() {
        return modifiers;
    }

    public boolean isEscape() {
        return key == InputConstants.KEY_ESCAPE;
    }
}
