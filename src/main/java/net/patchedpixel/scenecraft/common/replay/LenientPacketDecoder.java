package net.patchedpixel.scenecraft.common.replay;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.ConnectionProtocol;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static net.patchedpixel.scenecraft.common.replay.SceneCraftReplay.LOGGER;

/**
 * Makes the replay tolerant of packets that carry more bytes than the vanilla codec reads.
 */
public final class LenientPacketDecoder {
    private static final Set<String> REPORTED = ConcurrentHashMap.newKeySet();

    private LenientPacketDecoder() {}

    /**
     * Discards everything still readable input {@code input}. Called right after the codec has decoded a packet.
     *
     * @param protocol the protocol phase of the decoder, only used for logging
     * @param input       the buffer holding the packet; its reader index is just behind the decoded packet
     */
    public static void skipTrailingBytes(ConnectionProtocol protocol, ByteBuf input) {
        int extra = input.readableBytes();
        if(extra <= 0) {
            return;
        }

        int id = readPacketId(input);
        if(REPORTED.add(protocol + "/" + id)) {
            LOGGER.warn("Replay packet {}/{} has {} unread trailing bytes (extra data appended by a mod or server, "
                    + "or a vanilla format difference). They are skipped so the packet is not lost. "
                    + "Only the first occurrence of each packet is logged.", protocol, id, extra);
        }

        input.skipBytes(extra);
    }

    private static int readPacketId(ByteBuf in) {
        int value = 0;
        for(int i = 0; i < 5 && i < in.writerIndex(); i++) {
            byte b = in.getByte(i);
            value |= (b & 0x7F) << (7 * i);
            if((b & 0x80) == 0) {
                return value;
            }
        }

        return -1;
    }
}
