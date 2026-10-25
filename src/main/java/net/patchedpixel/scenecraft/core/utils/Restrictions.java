package net.patchedpixel.scenecraft.core.utils;

import net.patchedpixel.scenecraft.SceneCraft;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import static net.patchedpixel.scenecraft.core.versions.MCVer.identifier;
import io.netty.buffer.Unpooled;

/**
 * Restrictions set by the server,
 * @see <a href="https://gist.github.com/Johni0702/2547c463e51f65f312cb">Replay Restrictions Gist</a>
 */
public class Restrictions {
    public static final Identifier PLUGIN_CHANNEL = identifier(SceneCraft.MOD_ID, "restrict");

    public String handle(ClientboundCustomPayloadPacket packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(((Payload) packet.payload()).bytes()));
        while (buffer.isReadable()) {
            String name = buffer.readUtf(64);
            return name;
        }

        return null;
    }

    public static final CustomPacketPayload.Type<Payload> ID = new CustomPacketPayload.Type<>(PLUGIN_CHANNEL);
    public static final StreamCodec<? super FriendlyByteBuf, Payload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBytes(payload.bytes()),
            buf -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new Payload(bytes);
            }
    );
    public record Payload(byte[] bytes) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return ID;
        }
    }
}
