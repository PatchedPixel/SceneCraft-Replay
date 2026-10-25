package net.patchedpixel.scenecraft.render.hooks;

import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;

public interface FramebufferDelegateHolder {
    void setFramebufferDelegate(RenderTarget framebuffer);

    static FramebufferDelegateHolder get(Minecraft mc) {
        return (FramebufferDelegateHolder) mc;
    }
}
