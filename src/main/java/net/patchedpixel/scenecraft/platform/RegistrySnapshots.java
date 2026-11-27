package net.patchedpixel.scenecraft.platform;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.fml.config.ConfigTracker;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.payload.FrozenRegistryPayload;
import net.neoforged.neoforge.registries.RegistryManager;
import net.neoforged.neoforge.registries.RegistrySnapshot;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RegistrySnapshots {
    public static final String ENTRY = "registries.bin";

    private static final Logger LOGGER = LoggerFactory.getLogger("SceneCraft");

    private RegistrySnapshots() {}

    public static byte[] capture() {
        List<FrozenRegistryPayload> payloads = RegistryManager.generateRegistryPackets(false);

        ByteBuf byteBuf = Unpooled.buffer();
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            buf.writeVarInt(payloads.size());
            for(FrozenRegistryPayload payload : payloads) {
                FrozenRegistryPayload.STREAM_CODEC.encode(buf, payload);
            }

            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            return bytes;
        } finally {
            byteBuf.release();
        }
    }

    public static boolean apply(InputStream in) throws IOException {
        ByteBuf byteBuf = Unpooled.wrappedBuffer(in.readAllBytes());
        Map<Identifier, RegistrySnapshot> usable = new HashMap<>();
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            int count = buf.readVarInt();
            for(int i = 0; i < count; i++) {
                FrozenRegistryPayload payload = FrozenRegistryPayload.STREAM_CODEC.decode(buf);
                Identifier name = payload.registryName();
                RegistrySnapshot snapshot = payload.snapshot();

                Registry<?> registry = BuiltInRegistries.REGISTRY.getValue(name);
                if(registry == null) {
                    if(!snapshot.getIds().isEmpty()) {
                        LOGGER.warn("Replay has ids for registry {} which is not installed, ignoring it", name);
                    }
                    continue;
                }

                String missing = firstMissing(registry, snapshot);
                if(missing != null) {
                    LOGGER.warn("Replay was recorded with registry entries that are not installed (registry {}, e.g. {}), "
                            + "keeping the local ids for this registry", name, missing);
                    continue;
                }

                usable.put(name, snapshot);
            }
        } finally {
            byteBuf.release();
        }

        if(usable.isEmpty()) {
            return false;
        }

        RegistryManager.applySnapshot(usable, false);
        return true;
    }

    public static void revert() {
        RegistryManager.revertToFrozen();
    }

    private static <T> String firstMissing(Registry<T> registry, RegistrySnapshot snapshot) {
        for(Identifier id : snapshot.getIds().values()) {
            if(!registry.containsKey(ResourceKey.create(registry.key(), id))) {
                return id.toString();
            }
        }
        return null;
    }

    public static void reloadDefaultSyncedConfigs() {
        unloadSyncedConfigs();
        if(MCVer.getMinecraft().hasSingleplayerServer()) {
            return;
        }

        try {
            ConfigTracker.INSTANCE.loadDefaultServerConfigs();
        } catch(Throwable t) {
            LOGGER.warn("Could not load default synced configs for the replay", t);
        }
    }

    public static void unloadSyncedConfigs() {
        if(MCVer.getMinecraft().hasSingleplayerServer()) {
            return;
        }

        try {
            ConfigTracker.INSTANCE.unloadConfigs(ModConfig.Type.SERVER);
        } catch(Throwable t) {
            LOGGER.warn("Could not unload synced configs of the replay", t);
        }
    }
}
