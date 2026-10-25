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
package net.patchedpixel.scenecraft.gui.container;

import net.patchedpixel.scenecraft.gui.GuiRenderer;
import net.patchedpixel.scenecraft.gui.MinecraftGuiRenderer;
import net.patchedpixel.scenecraft.gui.OffsetGuiRenderer;
import net.patchedpixel.scenecraft.gui.RenderInfo;
import net.patchedpixel.scenecraft.gui.element.GuiElement;
import net.patchedpixel.scenecraft.gui.element.GuiLabel;
import net.patchedpixel.scenecraft.gui.function.*;
import net.patchedpixel.scenecraft.gui.function.*;
import net.patchedpixel.scenecraft.gui.utils.geometry.Dimension;
import net.patchedpixel.scenecraft.gui.utils.geometry.Point;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadablePoint;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import static net.patchedpixel.scenecraft.core.versions.MCVer.literalText;

public abstract class AbstractGuiScreen<T extends AbstractGuiScreen<T>> extends AbstractGuiContainer<T> {
    private final MinecraftGuiScreen wrapped = new MinecraftGuiScreen();

    private Dimension screenSize;

    private Background background = Background.DEFAULT;

    private boolean enabledRepeatedKeyEvents = true;

    private GuiLabel title;

    protected boolean suppressVanillaKeys;

    public net.minecraft.client.gui.screens.Screen toMinecraft() {
        return wrapped;
    }

    @Override
    public void layout(ReadableDimension size, RenderInfo renderInfo) {
        if (size == null) {
            size = screenSize;
        }
        if (renderInfo.layer == 0) {
            if (title != null) {
                title.layout(title.getMinSize(), renderInfo);
            }
        }
        super.layout(size, renderInfo);
        if (renderInfo.layer == getMaxLayer()) {
            final GuiElement tooltip = forEach(GuiElement.class, e -> e.getTooltip(renderInfo));
            if (tooltip != null) {
                tooltip.layout(tooltip.getMinSize(), renderInfo);
            }
        }
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        if (renderInfo.layer == 0) {
            switch (background) {
                case NONE:
                    break;
                case DEFAULT:
                    // Handled by override in MinecraftGuiScreen
                    break;
                case TRANSPARENT:
                    int top = 0xc0_10_10_10, bottom = 0xd0_10_10_10;
                    renderer.drawRect(0, 0, size.getWidth(), size.getHeight(), top, top, bottom, bottom);
                    break;
                case DIRT:
                    // Handled by override in MinecraftGuiScreen
                    break;
            }
            if (title != null) {
                ReadableDimension titleSize = title.getMinSize();
                int x = screenSize.getWidth() / 2 - titleSize.getWidth() / 2;
                OffsetGuiRenderer eRenderer = new OffsetGuiRenderer(renderer, new Point(x, 10), new Dimension(0, 0));
                title.draw(eRenderer, titleSize, renderInfo);
            }
        }
        super.draw(renderer, size, renderInfo);
        if (renderInfo.layer == getMaxLayer()) {
            final GuiElement tooltip = forEach(GuiElement.class, e -> e.getTooltip(renderInfo));
            if (tooltip != null) {
                final ReadableDimension tooltipSize = tooltip.getMinSize();
                int x, y;
                if (renderInfo.mouseX + 8 + tooltipSize.getWidth() < screenSize.getWidth()) {
                    x = renderInfo.mouseX + 8;
                } else {
                    x = screenSize.getWidth() - tooltipSize.getWidth() - 1;
                }
                if (renderInfo.mouseY + 8 + tooltipSize.getHeight() < screenSize.getHeight()) {
                    y = renderInfo.mouseY + 8;
                } else {
                    y = screenSize.getHeight() - tooltipSize.getHeight() - 1;
                }
                final ReadablePoint position = new Point(x, y);
                try {
                    OffsetGuiRenderer eRenderer = new OffsetGuiRenderer(renderer, position, tooltipSize);
                    tooltip.draw(eRenderer, tooltipSize, renderInfo);
                } catch (Exception ex) {
                    CrashReport crashReport = CrashReport.forThrowable(ex, "Rendering Gui Tooltip");
                    renderInfo.addTo(crashReport);
                    CrashReportCategory category = crashReport.addCategory("Gui container details");
                    MCVer.addDetail(category, "Container", this::toString);
                    MCVer.addDetail(category, "Width", () -> "" + size.getWidth());
                    MCVer.addDetail(category, "Height", () -> "" + size.getHeight());
                    category = crashReport.addCategory("Tooltip details");
                    MCVer.addDetail(category, "Element", tooltip::toString);
                    MCVer.addDetail(category, "Position", position::toString);
                    MCVer.addDetail(category, "Size", tooltipSize::toString);
                    throw new ReportedException(crashReport);
                }
            }
        }
    }

    @Override
    public ReadableDimension getMinSize() {
        return screenSize;
    }

    @Override
    public ReadableDimension getMaxSize() {
        return screenSize;
    }

    public void setEnabledRepeatedKeyEvents(boolean enableRepeatKeyEvents) {
        // The game always delivers key repeat events, so only the flag is stored here
        this.enabledRepeatedKeyEvents = enableRepeatKeyEvents;
    }

    public void display() {
        getMinecraft().setScreen(toMinecraft());
    }

    protected boolean handleClose() {
        return false;
    }

    public Background getBackground() {
        return this.background;
    }

    public boolean isEnabledRepeatedKeyEvents() {
        return this.enabledRepeatedKeyEvents;
    }

    public GuiLabel getTitle() {
        return this.title;
    }

    public void setBackground(Background background) {
        this.background = background;
    }

    public void setTitle(GuiLabel title) {
        this.title = title;
    }

    protected class MinecraftGuiScreen extends net.minecraft.client.gui.screens.Screen {
        private boolean active;

        protected MinecraftGuiScreen() {
            super(null);
        }

        @Override
        public Component getTitle() {
            GuiLabel title = AbstractGuiScreen.this.title;
            return literalText(title == null ? "" : title.getText());
        }

        @Override
        public void render(GuiGraphics stack, int mouseX, int mouseY, float partialTicks) {
            switch (background) {
                case NONE:
                case TRANSPARENT:
                    break;
                case DEFAULT:
                    super.renderBackground(stack);
                    break;
                case DIRT:
                    super.renderDirtBackground(stack);
                    break;
            }
            int layers = getMaxLayer();
            RenderInfo renderInfo = new RenderInfo(partialTicks, mouseX, mouseY, 0);
            for (int layer = 0; layer <= layers; layer++) {
                layout(screenSize, renderInfo.layer(layer));
            }
            MinecraftGuiRenderer renderer = new MinecraftGuiRenderer(stack);
            for (int layer = 0; layer <= layers; layer++) {
                stack.pose().pushPose();
                stack.pose().translate(0, 0, layer * MinecraftGuiRenderer.LAYER_Z_STEP);
                try {
                    draw(renderer, screenSize, renderInfo.layer(layer));
                } finally {
                    stack.pose().popPose();
                }
            }
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            KeyInput keyInput = new KeyInput(keyCode, scanCode, modifiers);
            if (!invokeHandlers(KeyHandler.class, e -> e.handleKey(keyInput))) {
                if (suppressVanillaKeys) {
                    return false;
                }
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            CharInput charInput = new CharInput(codePoint, modifiers);
            if (!invokeHandlers(CharHandler.class, e -> e.handleChar(charInput))) {
                if (suppressVanillaKeys) {
                    return false;
                }
                return super.charTyped(codePoint, modifiers);
            }
            return true;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            Click click = new Click(mouseX, mouseY, button, MCVer.currentModifiers());
            return invokeHandlers(Clickable.class, e -> e.mouseClick(click));
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            Click click = new Click(mouseX, mouseY, button, MCVer.currentModifiers());
            return invokeHandlers(Draggable.class, e -> e.mouseRelease(click));
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            Click click = new Click(mouseX, mouseY, button, MCVer.currentModifiers());
            return invokeHandlers(Draggable.class, e -> e.mouseDrag(click));
        }

        @Override
        public void tick() {
            invokeAll(Tickable.class, Tickable::tick);
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double dWheel) {
            Point mouse = new Point((int) mouseX, (int) mouseY);
            int wheel = (int) (dWheel * 120);
            return invokeHandlers(Scrollable.class, e -> e.scroll(mouse, wheel));
        }

        @Override
        public void onClose() {
            if (!AbstractGuiScreen.this.handleClose()) {
                super.onClose();
            }
        }

        @Override
        public void removed() {
            invokeAll(Focusable.class, focusable -> {
                if (focusable.isFocused()) {
                    focusable.setFocused(false);
                }
            });
            invokeAll(Closeable.class, Closeable::close);
            active = false;
        }

        @Override
        public void init() {
            active = false;
            screenSize = new Dimension(width, height);
            invokeAll(Loadable.class, Loadable::load);
        }

        public T getWrapper() {
            return AbstractGuiScreen.this.getThis();
        }
    }

    public enum Background {
        NONE, DEFAULT, TRANSPARENT, DIRT;
    }
}
