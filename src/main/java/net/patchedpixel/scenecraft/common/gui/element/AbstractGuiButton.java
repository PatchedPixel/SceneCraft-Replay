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
package net.patchedpixel.scenecraft.common.gui.element;

import net.patchedpixel.scenecraft.common.gui.GuiRenderer;
import net.patchedpixel.scenecraft.common.gui.RenderInfo;
import net.patchedpixel.scenecraft.common.gui.container.GuiContainer;
import net.patchedpixel.scenecraft.common.gui.function.Click;
import net.patchedpixel.scenecraft.common.gui.function.Clickable;
import net.patchedpixel.scenecraft.common.gui.utils.geometry.Dimension;
import net.patchedpixel.scenecraft.common.gui.utils.geometry.Point;
import net.patchedpixel.scenecraft.common.gui.utils.geometry.ReadableDimension;
import net.patchedpixel.scenecraft.common.gui.utils.geometry.ReadablePoint;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;

import net.minecraft.client.renderer.RenderPipelines;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.*;

public abstract class AbstractGuiButton<T extends AbstractGuiButton<T>> extends AbstractGuiClickable<T> implements Clickable, IGuiButton<T> {
    protected static final Identifier BUTTON_SOUND = identifier("gui.button.press");
    protected static final Identifier[] BUTTON_TEXTURES = new Identifier[] {
            identifier("widget/button_disabled"),
            identifier("widget/button"),
            identifier("widget/button_highlighted"),
    };

    private SoundEvent sound = SoundEvents.UI_BUTTON_CLICK.value();

    private int labelColor = 0xe0e0e0;
    private String label;

    private Identifier texture;
    private ReadableDimension textureSize;
    private ReadablePoint spriteUV;
    private ReadableDimension spriteSize;

    public AbstractGuiButton() {
    }

    public AbstractGuiButton(GuiContainer container) {
        super(container);
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        super.draw(renderer, size, renderInfo);

        byte texture = 1;
        int color = labelColor;
        if(!isEnabled()) {
            texture = 0;
            color = 0xa0a0a0;
        } else if(isMouseHovering(new Point(renderInfo.mouseX, renderInfo.mouseY))) {
            texture = 2;
        }

        int halfWidth = size.getWidth() / 2;

        int offsetX = renderer.getGpuOffsetX();
        int offsetY = renderer.getGpuOffsetY();
        renderer.getContext().blitSprite(RenderPipelines.GUI_TEXTURED, BUTTON_TEXTURES[texture], offsetX, offsetY, size.getWidth(), size.getHeight());

        if(this.texture != null) {
            renderer.bindTexture(this.texture);
            if(spriteUV != null && textureSize != null) {
                ReadableDimension spriteSize = this.spriteSize != null ? this.spriteSize : getMinSize();
                renderer.drawTexturedRect(0, 0, spriteUV.getX(), spriteUV.getY(), size.getWidth(), size.getHeight(),
                        spriteSize.getWidth(), spriteSize.getHeight(),
                        textureSize.getWidth(), textureSize.getHeight());
            } else {
                renderer.drawTexturedRect(0, 0, 0, 0, size.getWidth(), size.getHeight());
            }
        }

        if(label != null) {
            renderer.drawCenteredString(halfWidth, (size.getHeight() - 8) / 2, color, label, true);
        }
    }

    @Override
    public ReadableDimension calcMinSize() {
        if(label != null) {
            Font fontRenderer = MCVer.getFontRenderer();
            return new Dimension(fontRenderer.width(label), 20);
        } else {
            return new Dimension(0, 0);
        }
    }

    @Override
    public void onClick(Click click) {
        playClickSound(getMinecraft());
        super.onClick(click);
    }

    public static void playClickSound(Minecraft mc) {
        playClickSound(mc, SoundEvents.UI_BUTTON_CLICK.value());
    }
    public static void playClickSound(Minecraft mc, SoundEvent sound) {
        mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1.0F));
    }

    @Override
    public T setLabel(String label) {
        this.label = label;
        return getThis();
    }

    @Override
    public T setSound(SoundEvent sound) {
        this.sound = sound;
        return getThis();
    }

    public SoundEvent getSound() {
        return this.sound;
    }

    @Override
    public T setI18nLabel(String label, Object... args) {
        return setLabel(I18n.get(label, args));
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabelColor(int labelColor) {
        this.labelColor = labelColor;
    }

    @Override
    public Identifier getTexture() {
        return texture;
    }

    @Override
    public T setTexture(Identifier texture) {
        this.texture = texture;
        return getThis();
    }

    @Override
    public ReadableDimension getTextureSize() {
        return textureSize;
    }

    @Override
    public T setTextureSize(ReadableDimension textureSize) {
        this.textureSize = textureSize;
        return getThis();
    }

    @Override
    public ReadablePoint getSpriteUV() {
        return spriteUV;
    }

    @Override
    public T setSpriteUV(ReadablePoint spriteUV) {
        this.spriteUV = spriteUV;
        return getThis();
    }

    @Override
    public ReadableDimension getSpriteSize() {
        return spriteSize;
    }

    @Override
    public T setSpriteSize(ReadableDimension spriteSize) {
        this.spriteSize = spriteSize;
        return getThis();
    }
}
