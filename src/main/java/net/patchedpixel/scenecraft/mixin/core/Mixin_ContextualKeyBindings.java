package net.patchedpixel.scenecraft.mixin.core;

import net.patchedpixel.scenecraft.core.SceneCraft;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * We have bunch of keybindings which only have an effect while in a replay but heavily conflict with vanilla ones
 * otherwise. To work around this, we prevent our keybindings (or conflicting ones) from making it into the keysByCode
 * map, depending on the current context.
 */
@Mixin(KeyMapping.class)
public class Mixin_ContextualKeyBindings {
    @Shadow @Final private static Map<String, KeyMapping> ALL;
    @Unique private static Collection<KeyMapping> keyBindings() { return Mixin_ContextualKeyBindings.ALL.values(); }

    @Unique private static final List<KeyMapping> temporarilyRemoved = new ArrayList<>();

    @Inject(method = "resetMapping", at = @At("HEAD"))
    private static void preContextualKeyBindings(CallbackInfo ci) {
        SceneCraft mod = SceneCraft.instance;
        if (mod == null) {
            return;
        }

        Set<KeyMapping> onlyInReplay = mod.getKeyBindingRegistry().getOnlyInReplay();
        if (SceneCraftReplay.instance.getReplayHandler() != null) {
            // In replay, remove any conflicting key bindings, so that ours are guaranteed in
            keyBindings().removeIf(keyBinding -> {
                for (KeyMapping exclusiveBinding : onlyInReplay) {
                    if (keyBinding.same(exclusiveBinding) && keyBinding != exclusiveBinding) {
                        temporarilyRemoved.add(keyBinding);
                        return true;
                    }
                }

                return false;
            });
        } else {
            // Not in a replay, remove all replay-exclusive keybindings
            keyBindings().removeIf(keyBinding -> {
                if (onlyInReplay.contains(keyBinding)) {
                    temporarilyRemoved.add(keyBinding);
                    return true;
                }

                return false;
            });
        }
    }

    @Inject(method = "resetMapping", at = @At("RETURN"))
    private static void postContextualKeyBindings(CallbackInfo ci) {
        for (KeyMapping keyBinding : temporarilyRemoved) {
            Mixin_ContextualKeyBindings.ALL.put(keyBinding.getName(), keyBinding);
        }

        temporarilyRemoved.clear();
    }
}
