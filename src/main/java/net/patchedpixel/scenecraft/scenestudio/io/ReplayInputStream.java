/*
 * Copyright (c) 2021
 *
 * This file is part of ReplayStudio.
 *
 * ReplayStudio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ReplayStudio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ReplayStudio.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.patchedpixel.scenecraft.scenestudio.io;

import net.patchedpixel.scenecraft.scenestudio.PacketData;
import net.patchedpixel.scenecraft.scenestudio.netio.ByteBufNetInput;
import net.patchedpixel.scenecraft.scenestudio.protocol.Packet;
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketType;
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketTypeRegistry;
import net.patchedpixel.scenecraft.scenestudio.protocol.State;
import net.patchedpixel.scenecraft.scenestudio.protocol.packets.PacketJoinGame;
import net.patchedpixel.scenecraft.scenestudio.protocol.registry.Registries;
import net.patchedpixel.scenecraft.scenestudio.stream.PacketStream;
import net.patchedpixel.scenecraft.scenestudio.studio.StudioPacketStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Queue;

import static net.patchedpixel.scenecraft.scenestudio.util.Utils.readInt;

/**
 * Input stream for reading packet data.
 * The packets are returned as they were recorded; the protocol of the replay must be the one of the running game.
 */
public class ReplayInputStream extends InputStream {
    private static final ByteBufAllocator ALLOC = PooledByteBufAllocator.DEFAULT;

    private PacketTypeRegistry registry;
    private Registries mcRegistries;

    /**
     * The actual input stream.
     */
    private final InputStream in;

    /**
     * Whether the packet stream (at the head of the input stream, not any already buffered packets) is currently
     * in the login phase.
     */
    private boolean loginPhase = true;

    /**
     * Whether login phase packets are returned from the stream (otherwise they'll be silently dropped).
     */
    private final boolean outputLoginPhase;

    /**
     * Packets which have already been read from the input but have not yet been requested via {@link #readPacket()}.
     */
    private final Queue<PacketData> buffer = new ArrayDeque<>();

    /**
     * Creates a new replay input stream for reading raw packet data.
     * @param registry Determines whether the login phase is part of the output: if it is a registry for the
     *                 LOGIN state the login phase packets are returned, otherwise they are dropped. The stream
     *                 always starts reading in the LOGIN phase because replays always include it.
     * @param in The actual input stream.
     */
    public ReplayInputStream(PacketTypeRegistry registry, InputStream in) {
        this.outputLoginPhase = registry.getState() == State.LOGIN;
        if (registry.getState() == State.PLAY) {
            throw new IllegalArgumentException("Cannot go directly to the PLAY phase, only LOGIN and CONFIGURATION are valid.");
        }
        this.registry = PacketTypeRegistry.get(State.LOGIN);
        this.in = in;
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    /**
     * @return The registry for the phase the head of the stream is currently in
     */
    public PacketTypeRegistry getRegistry() {
        return registry;
    }

    /**
     * Read the next packet from this input stream.
     * @return The packet
     * @throws IOException if an I/O error occurs.
     */
    public PacketData readPacket() throws IOException {
        fillBuffer();
        return buffer.poll();
    }

    private void fillBuffer() throws IOException {
        while (buffer.isEmpty()) {
            int next = readInt(in);
            int length = readInt(in);
            if (next == -1 || length == -1) {
                break; // reached end of stream
            }
            if (length == 0) {
                continue; // skip empty segments
            }

            ByteBuf buf = ALLOC.buffer(length);
            while (length > 0) {
                int read = buf.writeBytes(in, length);
                if (read == -1) {
                    buf.release();
                    throw new EOFException();
                }
                length -= read;
            }

            int packetId = new ByteBufNetInput(buf).readVarInt();
            Packet packet = new Packet(registry, packetId, registry.getType(packetId), buf);
            PacketType type = packet.getType();

            switch (type) {
                case JoinGame:
                    PacketJoinGame joinGame = PacketJoinGame.read(packet, mcRegistries);
                    mcRegistries = joinGame.registries;
                    // Set entity id to an arbitrary negative value (which are invalid to use, so we know it's unused).
                    // Using the bitwise negation because we still want to be able to read the id later in SquashFilter.
                    joinGame.entityId = ~joinGame.entityId;
                    joinGame.gameMode = 3; // Spectator
                    try (Packet.Writer writer = packet.overwrite()) {
                        joinGame.write(packet, writer);
                    }
                    break;
                default:
                    break;
            }

            // The packet was decoded with the registry of the phase it was recorded in. Switch to the registry
            // of the next phase for the packets that follow.
            boolean loginPhasePacket = loginPhase;
            if (type == PacketType.LoginSuccess) {
                loginPhase = false;
                registry = registry.withLoginSuccess();
            }

            if (type == PacketType.KeepAlive) {
                packet.release();
                continue; // They aren't needed in a replay
            }
            if ((loginPhasePacket || type == PacketType.LoginSuccess) && !outputLoginPhase) {
                packet.release();
                continue;
            }
            buffer.offer(new PacketData(next, packet));
        }
    }

    /**
     * Wraps this {@link ReplayInputStream} into a {@link PacketStream}.
     * Closing the replay input stream will close the packet stream and vice versa.
     */
    public PacketStream asPacketStream() {
        return new StudioPacketStream(this);
    }
}
