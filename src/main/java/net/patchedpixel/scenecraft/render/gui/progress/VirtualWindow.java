package net.patchedpixel.scenecraft.render.gui.progress;

import net.patchedpixel.scenecraft.render.hooks.FramebufferDelegateHolder;
import net.patchedpixel.scenecraft.render.hooks.WindowDelegateHolder;
import net.patchedpixel.scenecraft.gui.function.Closeable;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



import com.mojang.blaze3d.pipeline.MainTarget;

public class VirtualWindow implements Closeable {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Minecraft mc;
    private final Window window;

    private final RenderTarget guiFramebuffer;
    private boolean isBound;
    private int framebufferWidth, framebufferHeight;

    private int gameWidth, gameHeight;

    public VirtualWindow(Minecraft mc) {
        this.mc = mc;
        this.window = mc.getWindow();

        framebufferWidth = window.framebufferWidth;
        framebufferHeight = window.framebufferHeight;

        guiFramebuffer = new MainTarget(framebufferWidth, framebufferHeight);

        WindowDelegateHolder.get(mc).setWindowDelegate(this);
    }

    @Override
    public void close() {
        guiFramebuffer.destroyBuffers();

        WindowDelegateHolder.get(mc).setWindowDelegate(null);
    }

    public void bind() {
        gameWidth = window.framebufferWidth;
        gameHeight = window.framebufferHeight;
        window.framebufferWidth = framebufferWidth;
        window.framebufferHeight = framebufferHeight;
        applyScaleFactor();
        isBound = true;
    }

    public void unbind() {
        window.framebufferWidth = gameWidth;
        window.framebufferHeight = gameHeight;
        applyScaleFactor();
        isBound = false;
    }

    public void beginWrite() {
        FramebufferDelegateHolder.get(mc).setFramebufferDelegate(guiFramebuffer);
        guiFramebuffer.bindWrite(true);
    }

    public void endWrite() {
        guiFramebuffer.unbindWrite();
        FramebufferDelegateHolder.get(mc).setFramebufferDelegate(null);
        mc.getMainRenderTarget().bindWrite(true);
    }

    public void flip() {
        if (framebufferWidth == 0 || framebufferHeight == 0) {
            return;
        }

        guiFramebuffer.unbindWrite();
        guiFramebuffer.blitToScreen(framebufferWidth, framebufferHeight);
        window.updateDisplay();
    }

    /**
     * Updates the size of the window's framebuffer. Must only be called while this window is bound.
     */
    public void onResolutionChanged(int newWidth, int newHeight) {
        if (newWidth == 0 || newHeight == 0) {
            // These can be zero on Windows if minimized.
            // Creating zero-sized framebuffers however will throw an error, so we never want to switch to zero values.
            return;
        }

        if (framebufferWidth == newWidth && framebufferHeight == newHeight) {
            return; // size is unchanged, nothing to do
        }

        framebufferWidth = newWidth;
        framebufferHeight = newHeight;
        guiFramebuffer.resize(newWidth, newHeight, Minecraft.ON_OSX);

        applyScaleFactor();
        if (mc.screen != null) {
            mc.screen.resize(mc, window.getGuiScaledWidth(), window.getGuiScaledHeight());
        }
    }

    private void applyScaleFactor() {
        window.setGuiScale(window.calculateScale(mc.options.guiScale().get(), mc.isEnforceUnicode()));
    }

    public int getFramebufferWidth() {
        return framebufferWidth;
    }

    public int getFramebufferHeight() {
        return framebufferHeight;
    }

    public boolean isBound() {
        return isBound;
    }
}
