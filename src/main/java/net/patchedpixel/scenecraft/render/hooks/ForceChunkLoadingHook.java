package net.patchedpixel.scenecraft.render.hooks;

import net.patchedpixel.scenecraft.render.utils.FlawlessFrames;
import net.minecraft.client.renderer.LevelRenderer;

public class ForceChunkLoadingHook {
    public static boolean enabled;

    private final LevelRenderer hooked;

    public ForceChunkLoadingHook(LevelRenderer renderGlobal) {
        this.hooked = renderGlobal;

        enabled = true;
        FlawlessFrames.setEnabled(true);
    }

    public void uninstall() {
        FlawlessFrames.setEnabled(false);
        enabled = false;
    }

    public interface IBlockOnChunkRebuilds {
        boolean uploadEverythingBlocking();
    }
}
