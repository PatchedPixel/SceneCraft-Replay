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
import net.patchedpixel.scenecraft.gui.function.*;
import net.patchedpixel.scenecraft.gui.function.*;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.patchedpixel.scenecraft.gui.utils.MouseUtils;
import net.patchedpixel.scenecraft.gui.utils.geometry.Dimension;
import net.patchedpixel.scenecraft.gui.utils.geometry.Point;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadablePoint;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.gui.versions.ScreenExt;
import net.patchedpixel.scenecraft.gui.versions.callbacks.PreTickCallback;
import net.patchedpixel.scenecraft.gui.versions.callbacks.RenderHudCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;

import net.minecraft.client.gui.GuiGraphics;

import static net.patchedpixel.scenecraft.core.versions.MCVer.literalText;

import com.mojang.blaze3d.platform.Window;

public abstract class AbstractGuiOverlay<T extends AbstractGuiOverlay<T>> extends AbstractGuiContainer<T> {
    private final UserInputGuiScreen userInputGuiScreen = new UserInputGuiScreen();
    private final EventHandler eventHandler = new EventHandler();
    private boolean visible;
    private Dimension screenSize;
    private boolean mouseVisible;
    private boolean closeable = true;

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if (this.visible != visible) {
            if (visible) {
                invokeAll(Loadable.class, Loadable::load);
                eventHandler.register();
            } else {
                invokeAll(Closeable.class, Closeable::close);
                eventHandler.unregister();
            }
            updateUserInputGui();
        }
        this.visible = visible;
    }

    public boolean isMouseVisible() {
        return mouseVisible;
    }

    public void setMouseVisible(boolean mouseVisible) {
        this.mouseVisible = mouseVisible;
        updateUserInputGui();
    }

    public boolean isCloseable() {
        return closeable;
    }

    public void setCloseable(boolean closeable) {
        this.closeable = closeable;
    }

    /**
     * @see #setAllowUserInput(boolean)
     */
    public boolean isAllowUserInput() {
        return ((ScreenExt) userInputGuiScreen).doesPassEvents();
    }

    /**
     * Enable/Disable user input for this overlay while the mouse is visible.
     * User input are things like moving the player, attacking/interacting, key bindings but not input into the
     * GUI elements such as text fields.
     * Default for overlays is {@code true} whereas for normal GUI screens it is {@code false}.
     * @param allowUserInput {@code true} to allow user input, {@code false} to disallow it
     */
    public void setAllowUserInput(boolean allowUserInput) {
        ((ScreenExt) userInputGuiScreen).setPassEvents(allowUserInput);
    }

    private void updateUserInputGui() {
        Minecraft mc = getMinecraft();
        if (visible) {
            if (mouseVisible) {
                if (mc.screen == null) {
                    mc.setScreen(userInputGuiScreen);
                }
            } else {
                if (mc.screen == userInputGuiScreen) {
                    mc.setScreen(null);
                }
            }
        }
    }

    @Override
    public void layout(ReadableDimension size, RenderInfo renderInfo) {
        if (size == null) {
            size = screenSize;
        }
        super.layout(size, renderInfo);
        if (mouseVisible && renderInfo.layer == getMaxLayer()) {
            final GuiElement tooltip = forEach(GuiElement.class, e -> e.getTooltip(renderInfo));
            if (tooltip != null) {
                tooltip.layout(tooltip.getMinSize(), renderInfo);
            }
        }
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        super.draw(renderer, size, renderInfo);
        if (mouseVisible && renderInfo.layer == getMaxLayer()) {
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

    private class EventHandler extends EventRegistrations {
        private EventHandler() {}

        { on(RenderHudCallback.EVENT, this::renderOverlay); }
        private void renderOverlay(GuiGraphics stack, float partialTicks) {
            updateUserInputGui();
            updateRenderer();
            int layers = getMaxLayer();
            int mouseX = -1, mouseY = -1;
            if (mouseVisible) {
                Point mouse = MouseUtils.getMousePos();
                mouseX = mouse.getX();
                mouseY = mouse.getY();
            }
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

        { on(PreTickCallback.EVENT, () -> invokeAll(Tickable.class, Tickable::tick)); }

        private void updateRenderer() {
            Minecraft mc = getMinecraft();
            Window
                    res = MCVer.newScaledResolution(mc);
            if (screenSize == null
                    || screenSize.getWidth() != res.getGuiScaledWidth()
                    || screenSize.getHeight() != res.getGuiScaledHeight()) {
                screenSize = new Dimension(res.getGuiScaledWidth(), res.getGuiScaledHeight());
            }
        }
    }

    protected class UserInputGuiScreen extends net.minecraft.client.gui.screens.Screen {

        UserInputGuiScreen() {
            super(literalText(""));
        }

        {
            ((ScreenExt) this).setPassEvents(true);
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            KeyInput keyInput = new KeyInput(keyCode, scanCode, modifiers);
            if (!invokeHandlers(KeyHandler.class, e -> e.handleKey(keyInput))) {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            return true;
        }

        @Override
        public boolean charTyped(char codePoint, int modifiers) {
            CharInput charInput = new CharInput(codePoint, modifiers);
            if (!invokeHandlers(CharHandler.class, e -> e.handleChar(charInput))) {
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
            if (closeable) {
                super.onClose();
            }
        }

        @Override
        public void removed() {
            if (closeable) {
                mouseVisible = false;
            }
        }

        @Override
        public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        }

        public AbstractGuiOverlay<T> getOverlay() {
            return AbstractGuiOverlay.this;
        }
    }
}
