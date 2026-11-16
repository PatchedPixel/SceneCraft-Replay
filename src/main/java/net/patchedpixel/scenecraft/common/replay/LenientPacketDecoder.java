package net.patchedpixel.scenecraft.common.replay;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketDecoder;
import net.minecraft.network.protocol.BundleDelimiterPacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Same as vanilla's {@link PacketDecoder}, except that unread trailing bytes do not make the
 * packet fail.
 * <p>
 * With many mods installed, a packet recorded on a modded connection can contain more data than the same packet read
 * on the "vanilla connection" the replay pretends to be (e.g. Forge's extra recipe book types, or data appended by
 * other mods). Vanilla then throws and the whole packet is lost, which for example removes the recorded player.
 */
public class LenientPacketDecoder extends ByteToMessageDecoder {
    private static final Logger LOGGER = LogManager.getLogger();
    private final PacketFlow flow;
    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    public LenientPacketDecoder(PacketFlow flow) {
        this.flow = flow;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if(in.readableBytes() == 0) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(in);
        int id = buf.readVarInt();
        ConnectionProtocol protocol = ctx.channel().attr(Connection.ATTRIBUTE_PROTOCOL).get();
        Packet<?> packet = protocol.createPacket(flow, id, buf);
        if(packet == null) {
            throw new IOException("Bad packet id " + id);
        }
        int extra = buf.readableBytes();
        if(extra > 0) {
            buf.skipBytes(extra);
            String name = packet.getClass().getSimpleName();
            if(warned.add(name)) {
                LOGGER.warn("Packet {}/{} ({}) has {} unread trailing bytes (appended by a mod or only readable on a "
                        + "modded connection), ignoring them", protocol.getId(), id, name, extra);
            }
            if(packet instanceof BundleDelimiterPacket) {
                // A delimiter never carries data, this is not a real delimiter. Delivering it would make the bundler
                // swallow every following packet.
                return;
            }
        }
        out.add(packet);
    }
}
