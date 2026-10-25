package net.patchedpixel.scenecraft.replay;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import net.patchedpixel.scenecraft.scenestudio.protocol.Packet;
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketType;
import net.patchedpixel.scenecraft.scenestudio.protocol.State;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayFile;
import net.patchedpixel.scenecraft.scenestudio.rar.RandomAccessReplay;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.patchedpixel.scenecraft.gui.versions.callbacks.PreTickCallback;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.patchedpixel.scenecraft.scenestudio.protocol.packets.PacketEntityTeleport;
import net.patchedpixel.scenecraft.scenestudio.util.Location;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.Consumer;

import static net.patchedpixel.scenecraft.core.utils.Utils.DEFAULT_MS_PER_TICK;
import static net.patchedpixel.scenecraft.core.versions.MCVer.getMinecraft;
import static net.patchedpixel.scenecraft.core.versions.MCVer.getPacketTypeRegistry;
import static net.patchedpixel.scenecraft.replay.SceneCraftReplay.LOGGER;

/**
 * Sends only chunk updates and entity position updates but tries to do so as quickly as possible.
 * To do so, it performs an initial analysis of the replay, scanning all of its packets and storing entity positions
 * and chunk states while doing so.
 * This allows it to later jump to any time by doing a diff from the current time (including backwards jumping).
 */
@ChannelHandler.Sharable
public class QuickReplaySender extends ChannelHandlerAdapter implements ReplaySender {
    private final Minecraft mc = getMinecraft();

    private final SceneCraftReplay mod;
    private final RandomAccessReplay replay;
    private final EventHandler eventHandler = new EventHandler();
    private Channel channel;

    private int currentTimeStamp;
    private double replaySpeed = 1;

    /**
     * Whether async mode is enabled.
     * Async mode is emulated by registering an event handler on client tick.
     */
    private boolean asyncMode;
    private long lastAsyncUpdateTime;

    private ListenableFuture<Void> initPromise;

    public QuickReplaySender(SceneCraftReplay mod, ReplayFile replayFile) {
        this.mod = mod;
        this.replay = new RandomAccessReplay(replayFile, getPacketTypeRegistry(State.PLAY)) {
            private byte[] buf = new byte[0];

            @Override
            protected void dispatch(Packet packet) {
                // Teleports are built with Minecraft's own packet class instead of hand-written bytes: the wire
                // format of entity movement changes between 26.x builds, and one malformed packet makes the
                // decoder keep leftover bytes that corrupt every packet after it.
                if (packet.getType() == PacketType.EntityTeleport) {
                    try {
                        int entityId;
                        try (Packet.Reader in = packet.reader()) {
                            entityId = in.readVarInt();
                        }
                        Location loc =
                                PacketEntityTeleport.getLocation(packet);
                        packet.release();
                        net.minecraft.network.FriendlyByteBuf teleportBuf = new net.minecraft.network.FriendlyByteBuf(Unpooled.buffer());
                        teleportBuf.writeVarInt(entityId);
                        teleportBuf.writeDouble(loc.getX());
                        teleportBuf.writeDouble(loc.getY());
                        teleportBuf.writeDouble(loc.getZ());
                        teleportBuf.writeByte((byte) ((int) (loc.getYaw() * 256.0F / 360.0F)));
                        teleportBuf.writeByte((byte) ((int) (loc.getPitch() * 256.0F / 360.0F)));
                        teleportBuf.writeBoolean(false);
                        channel.pipeline().fireChannelRead(new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(teleportBuf));
                        teleportBuf.release();
                        return;
                    } catch (IOException e) {
                        LOGGER.error("Failed to build entity teleport packet", e);
                        packet.release();
                        return;
                    }
                }

                // Convert SceneStudio-Netty buffer into MC-Netty buffer
                ByteBuf byteBuf = packet.getBuf();
                int size = byteBuf.readableBytes();
                if (buf.length < size) {
                    buf = new byte[size];
                }
                byteBuf.getBytes(byteBuf.readerIndex(), buf, 0, size);
                ByteBuf wrappedBuf = Unpooled.wrappedBuffer(buf);
                wrappedBuf.writerIndex(size);
                packet.release();

                // Combine id + payload
                ByteBuf bufWithId = channel.alloc().heapBuffer(2 + wrappedBuf.readableBytes());
                new FriendlyByteBuf(bufWithId).writeVarInt(packet.getId());
                bufWithId.writeBytes(wrappedBuf);

                channel.pipeline().fireChannelRead(bufWithId);
            }
        };
    }

    public void register() {
        eventHandler.register();
    }

    public void unregister() {
        eventHandler.unregister();
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public ListenableFuture<Void> getInitializationPromise() {
        return initPromise;
    }

    public ListenableFuture<Void> initialize(Consumer<Double> progress) {
        if (initPromise != null) {
            return initPromise;
        }
        SettableFuture<Void> promise = SettableFuture.create();
        initPromise = promise;
        new Thread(() -> {
            try {
                long start = System.currentTimeMillis();
                replay.load(progress);
                LOGGER.info("Initialized quick replay sender in " + (System.currentTimeMillis() - start) + "ms");
            } catch (Throwable e) {
                LOGGER.error("Initializing quick replay sender:", e);
                mod.getCore().runLaterWithoutLock(() -> {
                    mod.getCore().printWarningToChat("Error initializing quick replay sender: %s", e.getLocalizedMessage());
                    promise.setException(e);
                });
                return;
            }
            mod.getCore().runLaterWithoutLock(() -> promise.set(null));
        }).start();
        return promise;
    }

    private void ensureInitialized(Runnable body) {
        if (initPromise == null) {
            LOGGER.warn("QuickReplaySender used without prior initialization!", new Throwable());
            initialize(progress -> {});
        }
        Futures.addCallback(initPromise, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                body.run();
            }

            @Override
            public void onFailure(Throwable t) {
                // Error already printed by initialize method
            }
        }, Runnable::run);
    }

    public void restart() {
        replay.reset();
    }

    @Override
    public int currentTimeStamp() {
        return currentTimeStamp;
    }

    @Override
    public void setReplaySpeed(double factor) {
        if (factor != 0) {
            if (paused() && asyncMode) {
                lastAsyncUpdateTime = System.currentTimeMillis();
            }
            this.replaySpeed = factor;
        }
        mc.timer.msPerTick = DEFAULT_MS_PER_TICK / (float) factor;
    }

    @Override
    public double getReplaySpeed() {
        return replaySpeed;
    }

    @Override
    public boolean isAsyncMode() {
        return asyncMode;
    }

    @Override
    public void setAsyncMode(boolean async) {
        if (this.asyncMode == async) return;
        ensureInitialized(() -> {
            this.asyncMode = async;
            if (async) {
                lastAsyncUpdateTime = System.currentTimeMillis();
            }
        });
    }

    @Override
    public void setSyncModeAndWait() {
        setAsyncMode(false);
        // No waiting required, we emulated async mode via tick events
    }

    @Override
    public void jumpToTime(int value) {
        sendPacketsTill(value);
    }

    private class EventHandler extends EventRegistrations {
        { on(PreTickCallback.EVENT, this::onTick); }
        private void onTick() {
            if (!asyncMode || paused()) return;

            long now = System.currentTimeMillis();
            long realTimePassed = now - lastAsyncUpdateTime;
            lastAsyncUpdateTime = now;
            int replayTimePassed = (int) (realTimePassed * replaySpeed);
            sendPacketsTill(currentTimeStamp + replayTimePassed);
        }
    }

    @Override
    public void sendPacketsTill(int replayTime) {
        ensureInitialized(() -> {
            try {
                replay.seek(replayTime);
            } catch (IOException e) {
                e.printStackTrace();
            }
            currentTimeStamp = replayTime;
        });
    }
}
