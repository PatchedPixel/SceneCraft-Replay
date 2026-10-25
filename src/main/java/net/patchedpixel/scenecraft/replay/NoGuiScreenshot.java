package net.patchedpixel.scenecraft.replay;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.gui.versions.Image;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screens.Screen;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.GpuFence;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.blaze3d.platform.NativeImage;

public class NoGuiScreenshot {
    private final Image image;
    private final int width;
    private final int height;

    private NoGuiScreenshot(Image image, int width, int height) {
        this.image = image;
        this.width = width;
        this.height = height;
    }

    public Image getImage() {
        return image;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static ListenableFuture<NoGuiScreenshot> take(final Minecraft mc, final int width, final int height) {
        final SettableFuture<NoGuiScreenshot> future = SettableFuture.create();
        Runnable runnable = () -> {
            if (future.isCancelled()) {
                return;
            }

            int frameWidth = mc.getWindow().getWidth();
            int frameHeight = mc.getWindow().getHeight();

            final boolean guiHidden = mc.gui.hud.isHidden();
            final Screen orgScreen = mc.gui.screen();
            try {
                mc.gui.screen = null;
                if (!guiHidden) mc.gui.hud.toggle();

                mc.gameRenderer.update(mc.getDeltaTracker());
                mc.gameRenderer.extract(mc.getDeltaTracker(), true);
                mc.gameRenderer.render();
            } catch (Throwable t) {
                future.setException(t);
                return;
            } finally {
                // Reset GUI settings
                mc.gui.screen = orgScreen;
                if (!guiHidden) mc.gui.hud.toggle();
            }

            // The frame without GUI has been rendered
            // Read it, create the screenshot and finish the future
            try {
                Image image;
                GpuDevice device = RenderSystem.getDevice();
                try (GpuBuffer gpuBuffer = device.createBuffer(null, GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, (long) frameWidth * frameHeight * 4)) {
                    CommandEncoder cmd = device.createCommandEncoder();
                    cmd.copyTextureToBuffer(mc.gameRenderer.mainRenderTarget().getColorTexture(), gpuBuffer, 0, () -> {}, 0);
                    GpuFence fence = cmd.createFence();
                    cmd.submit();
                    fence.awaitCompletion(Long.MAX_VALUE);
                    try (GpuBufferSlice.MappedView readView = gpuBuffer.map(true, false)) {
                        NativeImage nativeImage = new NativeImage(frameWidth, frameHeight, false);
                        for (int y = 0; y < frameHeight; ++y) {
                            for (int x = 0; x < frameWidth; ++x) {
                                int color = readView.data().getInt((x + y * frameWidth) * 4);
                                nativeImage.setPixelABGR(x, frameHeight - y - 1, 0xff000000 | color);
                            }
                        }
                        image = new Image(nativeImage);
                    }
                }
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();

                // Scale & crop
                float scaleFactor = Math.max((float) width / imageWidth, (float) height / imageHeight);
                int croppedWidth = Math.clamp((int) (width / scaleFactor), 0, imageWidth);
                int croppedHeight = Math.clamp((int) (height / scaleFactor), 0, imageHeight);
                int offsetX = (imageWidth - croppedWidth) / 2;
                int offsetY = (imageHeight - croppedHeight) / 2;
                image = image.scaledSubRect(offsetX, offsetY, croppedWidth, croppedHeight, width, height);

                // Finish
                future.set(new NoGuiScreenshot(image, width, height));
            } catch (Throwable t) {
                future.setException(t);
            }
        };

        // Make sure we are not somewhere in the middle of the rendering process but always at the beginning
        // of the game loop. We cannot use the addScheduledTask method as it'll run the task if called
        // from the minecraft thread which is exactly what we want to avoid.
        SceneCraft.instance.runLater(runnable);
        return future;
    }
}
