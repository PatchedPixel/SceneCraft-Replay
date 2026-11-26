package net.patchedpixel.scenecraft.common.render.hooks;

import net.patchedpixel.scenecraft.common.render.utils.FlawlessFrames;

public class ForceChunkLoadingHook {
    public static boolean enabled;

    public ForceChunkLoadingHook() {
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
