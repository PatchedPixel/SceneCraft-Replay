package net.patchedpixel.scenecraft.platform.compat;

import net.patchedpixel.scenecraft.common.render.utils.FlawlessFrames;
import org.embeddedt.embeddium.api.service.FlawlessFramesService;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Entry point class for Embeddium for FREX integration.
 */
public class EmbeddiumFlawlessFramesService implements FlawlessFramesService {
    @Override
    public void acceptController(Function<String, Consumer<Boolean>> controller) {
        FlawlessFrames.acceptController(controller);
    }
}
