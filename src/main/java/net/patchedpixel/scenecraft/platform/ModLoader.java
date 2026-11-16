package net.patchedpixel.scenecraft.platform;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public final class ModLoader {
    private static final String LOADER_MOD_ID = "forge";
    private static final String MOD_METADATA_FILE = "/META-INF/mods.toml";

    private ModLoader() {}

    public static boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    public static boolean isModLoaded(String modId) {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(modId);
    }

    public static Optional<String> getModVersion(String modId) {
        return ModList.get().getModContainerById(modId).map(container -> container.getModInfo().getVersion().toString());
    }

    public static List<LoadedMod> getLoadedMods() {
        return ModList.get().getMods().stream().map(mod -> new LoadedMod(mod.getModId(), mod.getDisplayName(), mod.getVersion().toString())).toList();
    }

    public static boolean isLoaderModId(String modId) {
        return LOADER_MOD_ID.equals(modId);
    }

    /**
     * Server configs normally arrive with the login handshake, which a replay does not have.
     */
    public static void loadDefaultServerConfigs() {
        ConfigTracker.INSTANCE.loadDefaultServerConfigs();
    }

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static Path getOwnModRoot() {
        try {
            URL marker = ModLoader.class.getResource(MOD_METADATA_FILE);
            if(marker == null) throw new IllegalStateException(MOD_METADATA_FILE + " not found");
            return Path.of(marker.toURI()).getParent().getParent();
        } catch(URISyntaxException e) {
            throw new RuntimeException("Failed to locate SceneCraft mod root", e);
        }
    }
}
