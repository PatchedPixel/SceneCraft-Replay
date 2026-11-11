package net.patchedpixel.scenecraft.platform;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.neoforged.neoforge.client.ClientHooks;

/**
 * Helper class for chunk-compilation at rendering process
 */
public final class SectionCompilation {
    private SectionCompilation() {}

    public static void compileAsync(SectionRenderDispatcher.RenderSection section, RenderRegionCache cache, ClientLevel level) {
        var additionalRenderers = ClientHooks.gatherAdditionalRenderers(section.getRenderOrigin(), level);
        section.compileAsync(cache.createRegion(level, section.getSectionNode()), additionalRenderers);
    }
}
