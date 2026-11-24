package net.patchedpixel.scenecraft.platform;

import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.GameData;
import net.minecraftforge.registries.RegistryManager;
import org.slf4j.Logger;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class RegistrySnapshots {
    public static final String ENTRY = "registries.nbt";

    private static final Logger LOGGER = LogUtils.getLogger();

    private RegistrySnapshots() {}

    public static byte[] capture() throws IOException {
        CompoundTag root = new CompoundTag();
        RegistryManager.ACTIVE.takeSnapshot(false)
                .forEach((name, snapshot) -> root.put(name.toString(), snapshot.write()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NbtIo.writeCompressed(root, out);

        return out.toByteArray();
    }

    public static void apply(InputStream in) throws IOException {
        CompoundTag root = NbtIo.readCompressed(in);
        Map<ResourceLocation, ForgeRegistry.Snapshot> snapshots = new HashMap<>();

        for(String key : root.getAllKeys()) {
            snapshots.put(MCVer.identifier(key), ForgeRegistry.Snapshot.read(root.getCompound(key)));
        }

        Multimap<ResourceLocation, ResourceLocation> missing = GameData.injectSnapshot(snapshots, false, false);
        if(!missing.isEmpty()) {
            LOGGER.warn("Replay was recorded with registry entries that are not installed ({}), applying the registry ids of the recording anyway: {}",
                    missing.size(), missing);
            GameData.injectSnapshot(snapshots, false, true);
        }
    }

    public static void revert() {
        GameData.revertToFrozen();
    }
}
