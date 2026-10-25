package net.patchedpixel.scenecraft.render.utils;

import net.patchedpixel.scenecraft.SceneCraft;
import net.minecraftforge.fml.ModList;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Uses the "Flawless Frames" FREX feature which allows us to instruct third-party mods to sacrifice performance (even
 * beyond the point where it can no longer achieve interactive frame rates) in exchange for a noticeable boost to
 * quality.
 * In particular also to force-load all chunks with Canvas/Sodium/Bobby.
 *
 * See https://github.com/grondag/frex/pull/9
 */
public class FlawlessFrames {
    private static final List<Consumer<Boolean>> CONSUMERS = new CopyOnWriteArrayList<>();
    private static volatile boolean hasSodium;

    private FlawlessFrames() {}

    public static void acceptController(Function<String, Consumer<Boolean>> provider) {
        Consumer<Boolean> consumer = provider.apply(SceneCraft.MOD_ID);

        if(consumer != null) {
            CONSUMERS.add(consumer);
        }

        hasSodium = ModList.get().isLoaded("sodium");
    }

    public static void setEnabled(boolean enabled) {
        CONSUMERS.forEach(it -> it.accept(enabled));
    }

    public static boolean hasSodium() {
        return hasSodium;
    }
}
