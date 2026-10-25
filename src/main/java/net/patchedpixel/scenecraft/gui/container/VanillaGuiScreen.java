package net.patchedpixel.scenecraft.gui.container;

import net.patchedpixel.scenecraft.gui.function.CharHandler;
import net.patchedpixel.scenecraft.gui.function.CharInput;
import net.patchedpixel.scenecraft.gui.function.Click;
import net.patchedpixel.scenecraft.gui.function.Draggable;
import net.patchedpixel.scenecraft.gui.function.KeyHandler;
import net.patchedpixel.scenecraft.gui.function.KeyInput;
import net.patchedpixel.scenecraft.gui.function.Scrollable;
import net.patchedpixel.scenecraft.gui.function.Tickable;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.patchedpixel.scenecraft.gui.utils.MouseUtils;
import net.patchedpixel.scenecraft.gui.utils.geometry.Point;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadablePoint;
import net.patchedpixel.scenecraft.gui.versions.callbacks.InitScreenCallback;
import net.patchedpixel.scenecraft.gui.versions.callbacks.OpenGuiScreenCallback;
import net.patchedpixel.scenecraft.gui.versions.callbacks.PostRenderScreenCallback;
import net.patchedpixel.scenecraft.gui.versions.callbacks.PreTickCallback;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.minecraft.client.gui.GuiGraphics;
import net.patchedpixel.scenecraft.gui.versions.callbacks.KeyboardCallback;
import net.patchedpixel.scenecraft.gui.versions.callbacks.MouseCallback;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public class VanillaGuiScreen extends GuiScreen implements Draggable, KeyHandler, CharHandler, Scrollable, Tickable {
    private static final Map<net.minecraft.client.gui.screens.Screen, VanillaGuiScreen> WRAPPERS =
            Collections.synchronizedMap(new WeakHashMap<>());

    public static VanillaGuiScreen wrap(net.minecraft.client.gui.screens.Screen originalGuiScreen) {
        VanillaGuiScreen gui = WRAPPERS.get(originalGuiScreen);
        if (gui == null) {
            WRAPPERS.put(originalGuiScreen, gui = new VanillaGuiScreen(originalGuiScreen));
            gui.register();
        }
        return gui;
    }

    // Use wrap instead and make sure to preserve the existing layout.
    // (or if you really want your own, inline this code)
    @Deprecated
    public static VanillaGuiScreen setup(net.minecraft.client.gui.screens.Screen originalGuiScreen) {
        VanillaGuiScreen gui = new VanillaGuiScreen(originalGuiScreen);
        gui.register();
        return gui;
    }

    private final net.minecraft.client.gui.screens.Screen mcScreen;
    private final EventHandler eventHandler = new EventHandler();

    public VanillaGuiScreen(net.minecraft.client.gui.screens.Screen mcScreen) {
        this.mcScreen = mcScreen;
        this.suppressVanillaKeys = true;

        super.setBackground(Background.NONE);
    }

    // Needs to be called from or after GuiInitEvent.Post, will auto-unregister on any GuiOpenEvent
    public void register() {
        if (!eventHandler.active) {
            eventHandler.active = true;

            eventHandler.register();

            getSuperMcGui().init(MCVer.getMinecraft(), mcScreen.width, mcScreen.height);
        }
    }

    public void display() {
        getMinecraft().setScreen(mcScreen);
        register();
    }

    @Override
    public net.minecraft.client.gui.screens.Screen toMinecraft() {
        return mcScreen;
    }

    @Override
    public void setBackground(Background background) {
        throw new UnsupportedOperationException("Cannot set background of vanilla gui screen.");
    }

    private net.minecraft.client.gui.screens.Screen getSuperMcGui() {
        return super.toMinecraft();
    }

    @Override
    public boolean mouseClick(Click click) {
        return false;
    }

    @Override
    public boolean mouseDrag(Click click) {
        return false;
    }

    @Override
    public boolean mouseRelease(Click click) {
        return false;
    }

    @Override
    public boolean scroll(ReadablePoint mousePosition, int dWheel) {
        return false;
    }

    @Override
    public boolean handleKey(KeyInput keyInput) {
        return false;
    }

    @Override
    public boolean handleChar(CharInput charInput) {
        return false;
    }

    @Override
    public void tick() {
        // TODO this is a workaround for ReplayMod#560 until we remove the inner mc screen
        //      see also the note in ReplayMod's GuiBackgroundProcesses
        // If this screen ever becomes the main screen, something has gone wrong.
        if (getSuperMcGui() == getMinecraft().screen) {
            getMinecraft().setScreen(null);
        }
    }

    // Used when wrapping an already existing mc.GuiScreen
    private
    class EventHandler extends EventRegistrations
        implements KeyboardCallback, MouseCallback
    {
        private boolean active;

        { on(OpenGuiScreenCallback.EVENT, screen -> onGuiClosed()); }
        private void onGuiClosed() {
            unregister();

            if (active) {
                active = false;
                getSuperMcGui().removed();
                WRAPPERS.remove(mcScreen, VanillaGuiScreen.this);
            }
        }

        { on(InitScreenCallback.Pre.EVENT, this::preGuiInit); }
        private void preGuiInit(net.minecraft.client.gui.screens.Screen screen) {
            if (screen == mcScreen && active) {
                active = false;
                unregister();
                getSuperMcGui().removed();
                WRAPPERS.remove(mcScreen, VanillaGuiScreen.this);
            }
        }

        { on(PostRenderScreenCallback.EVENT, this::onGuiRender); }
        private void onGuiRender(GuiGraphics stack, float partialTicks) {
            Point mousePos = MouseUtils.getMousePos();
            getSuperMcGui().render(
                    stack,
                    mousePos.getX(), mousePos.getY(), partialTicks);
        }

        { on(PreTickCallback.EVENT, this::tickOverlay); }
        private void tickOverlay() {
            getSuperMcGui().tick();
        }

        { on(MouseCallback.EVENT, this); }

        @Override
        public boolean mouseDown(Click click) {
            return getSuperMcGui().mouseClicked(click.x, click.y, click.button);
        }

        @Override
        public boolean mouseDrag(Click click, double dx, double dy) {
            return getSuperMcGui().mouseDragged(click.x, click.y, click.button, dx, dy);
        }

        @Override
        public boolean mouseUp(Click click) {
            return getSuperMcGui().mouseReleased(click.x, click.y, click.button);
        }

        @Override
        public boolean mouseScroll(double x, double y, double horizontal, double vertical) {
            return getSuperMcGui().mouseScrolled(x, y, vertical);
        }

        { on(KeyboardCallback.EVENT, this); }

        @Override
        public boolean keyPressed(KeyInput keyInput) {
            return getSuperMcGui().keyPressed(keyInput.key, keyInput.scancode, keyInput.modifiers);
        }

        @Override
        public boolean keyReleased(KeyInput keyInput) {
            return getSuperMcGui().keyReleased(keyInput.key, keyInput.scancode, keyInput.modifiers);
        }

        @Override
        public boolean charTyped(CharInput charInput) {
            return getSuperMcGui().charTyped((char) charInput.codepoint, charInput.modifiers);
        }
    }
}
