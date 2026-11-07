package net.patchedpixel.scenecraft.common.render.gui.progress;

import net.patchedpixel.scenecraft.common.render.hooks.FramebufferDelegateHolder;
import net.patchedpixel.scenecraft.common.render.hooks.WindowDelegateHolder;
import net.patchedpixel.scenecraft.common.gui.function.Closeable;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.device.SurfaceException;

import com.mojang.blaze3d.systems.RenderSystem;

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
    }

    public void endWrite() {
        FramebufferDelegateHolder.get(mc).setFramebufferDelegate(null);
    }

    private boolean surfaceIsInvalid;
    private boolean windowSurfaceNeedsReconfiguring;
    public void flip() {
        if(framebufferWidth == 0 || framebufferHeight == 0) {
            return;
        }

        GpuSurface windowSurface = mc.windowSurface();

        if(windowSurfaceNeedsReconfiguring || windowSurface.isSuboptimal() && !surfaceIsInvalid) {
            GpuSurface.PresentMode presentMode = GpuSurface.PresentMode.getSupportedVsyncMode(windowSurface.supportedPresentModes(), mc.options.enableVsync().get());
            GpuSurface.Configuration config = new GpuSurface.Configuration(framebufferWidth, framebufferHeight, presentMode);
            try {
                windowSurface.configure(config);
                this.surfaceIsInvalid = false;
            } catch(SurfaceException e) {
                LOGGER.warn("Couldn't configure surface to {}: {}", config, e);
                this.surfaceIsInvalid = true;
            }
        }

        if(surfaceIsInvalid || window.isIconified()) {
            return;
        }

        try {
            windowSurface.acquireNextTexture();
        } catch(SurfaceException e) {
            LOGGER.warn("Couldn't acquire next surface texture with config {}: {}", windowSurface.currentConfiguration(), e);
            this.surfaceIsInvalid = true;
            this.windowSurfaceNeedsReconfiguring = true;
            return;
        }

        windowSurface.blitFromTexture(RenderSystem.getDevice().createCommandEncoder(), guiFramebuffer.getColorTextureView());
        RenderSystem.getDevice().createCommandEncoder().submit();
        windowSurface.present();
    }

    /**
     * Updates the size of the window's framebuffer. Must only be called while this window is bound.
     */
    public void onResolutionChanged(int newWidth, int newHeight) {
        if(newWidth == 0 || newHeight == 0) {
            // These can be zero on Windows if minimized.
            // Creating zero-sized framebuffers however will throw an error, so we never want to switch to zero values.
            return;
        }

        if(framebufferWidth == newWidth && framebufferHeight == newHeight) {
            return; // size is unchanged, nothing to do
        }

        framebufferWidth = newWidth;
        framebufferHeight = newHeight;
        windowSurfaceNeedsReconfiguring = true;

        guiFramebuffer.resize(newWidth, newHeight
        );

        applyScaleFactor();
        if(mc.gui.screen() != null) {
            mc.gui.screen().resize(window.getGuiScaledWidth(), window.getGuiScaledHeight());
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
