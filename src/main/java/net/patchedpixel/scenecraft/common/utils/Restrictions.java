package net.patchedpixel.scenecraft.common.utils;

import net.patchedpixel.scenecraft.common.SceneCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.identifier;

public class Restrictions {
    public static final ResourceLocation PLUGIN_CHANNEL = identifier(SceneCraft.RM_ID, "restrict");

    public String handle(ClientboundCustomPayloadPacket packet) {
        FriendlyByteBuf buffer = packet.getData();
        while(buffer.isReadable()) {
            String name = buffer.readUtf(64);
            return name;
        }
        return null;
    }
}
