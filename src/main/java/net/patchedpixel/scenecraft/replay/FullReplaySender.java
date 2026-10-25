package net.patchedpixel.scenecraft.replay;

import net.patchedpixel.scenecraft.scenestudio.netio.NetOutput;
import net.patchedpixel.scenecraft.scenestudio.netio.ByteBufNetOutput;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.core.utils.Restrictions;
import net.patchedpixel.scenecraft.replay.camera.CameraEntity;
import net.patchedpixel.scenecraft.scenestudio.io.ReplayInputStream;
import net.patchedpixel.scenecraft.scenestudio.protocol.State;
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketType;
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketTypeRegistry;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayFile;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.patchedpixel.scenecraft.gui.versions.callbacks.PreTickCallback;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.world.entity.Relative;
import org.apache.commons.lang3.mutable.MutableBoolean;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.patchedpixel.scenecraft.platform.Utils.DEFAULT_MS_PER_TICK;
import static net.patchedpixel.scenecraft.core.versions.MCVer.*;

/**
 * Sends replay packets to netty channels.
 * Even though {@link Sharable}, this should never be added to multiple pipes at once, it may however be re-added when
 * the replay restart from the beginning.
 */
@Sharable
public class FullReplaySender extends ChannelInboundHandlerAdapter implements ReplaySender {
    /**
     * These packets are ignored completely during replay.
     */
    private static final List<Class<?>> BAD_PACKETS = Arrays.asList(
            ClientboundHelloPacket.class, // workaround for an issue where ReplayMod prior to 2.6.20 would record these
            ClientboundTransferPacket.class,
            ClientboundBlockChangedAckPacket.class,
            ClientboundOpenBookPacket.class,
            ClientboundOpenScreenPacket.class,
            ClientboundUpdateRecipesPacket.class,
            ClientboundUpdateAdvancementsPacket.class,
            ClientboundSelectAdvancementsTabPacket.class,
            ClientboundSetCameraPacket.class,
            ClientboundSetTitleTextPacket.class,
            ClientboundSetHealthPacket.class,
            ClientboundMountScreenOpenPacket.class,
            ClientboundContainerClosePacket.class,
            ClientboundContainerSetSlotPacket.class,
            ClientboundContainerSetDataPacket.class,
            ClientboundOpenSignEditorPacket.class,
            ClientboundAwardStatsPacket.class,
            ClientboundSetExperiencePacket.class,
            ClientboundPlayerAbilitiesPacket.class
    );

    private static final int TP_DISTANCE_LIMIT = 128;

    /**
     * The replay handler responsible for the current replay.
     */
    private final ReplayHandler replayHandler;

    /**
     * Whether to work in async mode.
     * <p>
     * When in async mode, a separate thread send packets and waits according to their delays.
     * This is default in normal playback mode.
     * <p>
     * When in sync mode, no packets will be sent until {@link #sendPacketsTill(int)} is called.
     * This is used during path playback and video rendering.
     */
    protected boolean asyncMode;

    /**
     * Timestamp of the last packet sent in milliseconds since the start.
     */
    protected int lastTimeStamp;

    /**
     * @see #currentTimeStamp()
     */
    protected int currentTimeStamp;

    /**
     * The replay file.
     */
    protected ReplayFile replayFile;

    /**
     * The channel used to send packets to minecraft.
     */
    protected Channel channel;

    /**
     * The replay input stream from which new packets are read.
     * When accessing this stream make sure to synchronize on {@code this} as it's used from multiple threads.
     */
    protected ReplayInputStream replayIn;

    /**
     * The next packet that should be sent.
     * This is required as some actions such as jumping to a specified timestamp have to peek at the next packet.
     */
    protected PacketData nextPacket;

    /**
     * Which protocol (state) we're currently in.
     */
    private PacketTypeRegistry registry = getPacketTypeRegistry(State.LOGIN);

    /**
     * Whether we need to restart the current replay. E.g. when jumping backwards in time
     */
    protected boolean startFromBeginning = true;

    /**
     * Whether to terminate the replay. This only has an effect on the async mode and is {@code true} during sync mode.
     */
    protected boolean terminate;

    /**
     * The speed of the replay. 1 is normal, 2 is twice as fast, 0.5 is half speed and 0 is frozen
     */
    protected double replaySpeed = 1f;

    /**
     * Whether the world has been loaded and the dirt-screen should go away.
     */
    protected boolean hasWorldLoaded;

    /**
     * Whether we are currently in the middle of a bundle packet.
     */
    protected boolean inBundle;

    /**
     * The minecraft instance.
     */
    protected Minecraft mc = getMinecraft();

    /**
     * The total length of this replay in milliseconds.
     */
    protected final int replayLength;

    /**
     * Our actual entity net that the server gave to us.
     */
    protected int actualID = -1;

    /**
     * Whether to allow (process) the next player movement packet.
     *
     * Must only be accessed from the main thread.
     */
    protected boolean allowMovement;

    /**
     * Directory to which resource packs are extracted.
     */
    private final File tempResourcePackFolder = Files.createTempDir();

    private final EventHandler events = new EventHandler();

    /** Replay timestamp of the packet currently being pushed through the pipeline (sender thread). */
    private volatile int sendingTimeStamp = -1;

    /**
     * Create a new replay sender.
     * @param file The replay file
     */
    public FullReplaySender(ReplayHandler replayHandler, ReplayFile file) throws IOException {
        this.replayHandler = replayHandler;
        this.replayFile = file;
        this.replayLength = file.getMetaData().getDuration();

        events.register();
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /** Replay timestamps of border packets, read later on the main thread by the border mixins. */
    private static final Map<Packet<?>, Integer> BORDER_PACKET_TIMES = Collections.synchronizedMap(new IdentityHashMap<>());

    public static int borderPacketTime(Packet<?> packet) {
        Integer time = BORDER_PACKET_TIMES.get(packet);
        return time != null ? time : -1;
    }

    public static void forgetBorderPacket(Packet<?> packet) {
        BORDER_PACKET_TIMES.remove(packet);
    }

    /**
     * Set whether this replay sender operates in async mode.
     * When in async mode, it will send packets timed from a separate thread.
     * When not in async mode, it will send packets when {@link #sendPacketsTill(int)} is called.
     * @param asyncMode {@code true} to enable async mode
     */
    @Override
    public void setAsyncMode(boolean asyncMode) {
        if (this.asyncMode == asyncMode) return;
        this.asyncMode = asyncMode;
        if (asyncMode) {
            this.terminate = false;
            new Thread(asyncSender, "scenecraft-async-sender").start();
        } else {
            this.terminate = true;
        }
    }

    @Override
    public boolean isAsyncMode() {
        return asyncMode;
    }

    /**
     * Set whether this replay sender  to operate in sync mode.
     * When in sync mode, it will send packets when {@link #sendPacketsTill(int)} is called.
     * This call will block until the async worker thread has stopped.
     */
    @Override
    public void setSyncModeAndWait() {
        if (!this.asyncMode) return;
        this.asyncMode = false;
        this.terminate = true;
        // This will wait for the worker thread to leave the synchronized code part
    }

    /**
     * Return a fake system tile in milliseconds value that respects slowdown/speedup/pause and works in both,
     * sync and async mode.
     * Note: For sync mode this returns the last value passed to {@link #sendPacketsTill(int)}.
     * @return The timestamp in milliseconds since the start of the replay
     */
    @Override
    public int currentTimeStamp() {
        if (asyncMode && !paused()) {
            return (int) ((System.currentTimeMillis() - realTimeStart) * realTimeStartSpeed);
        } else {
            return lastTimeStamp;
        }
    }

    /**
     * Terminate this replay sender.
     */
    public void terminateReplay() {
        if (terminate) {
            return;
        }
        terminate = true;
        syncSender.shutdown();
        events.unregister();
        try {
            channel.pipeline().fireChannelInactive();
            channel.pipeline().close();
            FileUtils.deleteDirectory(tempResourcePackFolder);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static class EventHandler extends EventRegistrations {
        { on(PreTickCallback.EVENT, this::onWorldTick); }
        private void onWorldTick() {
            // Spawning a player into an empty chunk (which we might do with the record player)
            // prevents it from being moved by teleport packets (it essentially gets stuck) because
            // Entity#addedToChunk is not set and it is therefore not updated every tick.
            // To counteract this, we need to manually update it's position if it hasn't been added
            // to any chunk yet.
            // The `updateNeeded` flag appears to have been removed in 1.17, so this should no longer be an issue.
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        // When in async mode and the replay sender shut down, then don't send packets
        if(terminate && asyncMode) {
            return;
        }

        if (msg instanceof Packet<?> p) {
            try {
                p = processPacket(p);
                if (p != null) {
                    super.channelRead(ctx, p);
                }

                maybeRemoveDeadEntities(p);

                if (p instanceof ClientboundLevelChunkWithLightPacket) {
                    Runnable doLightUpdates = () -> {
                        ClientLevel world = mc.level;
                        if (world != null) {
                            MutableBoolean done = new MutableBoolean();
                            world.queueLightUpdate(done::setTrue);
                            while (!done.booleanValue()) {
                                world.pollLightUpdates();
                            }
                            LevelLightEngine provider = world.getChunkSource().getLightEngine();
                            while (provider.hasLightWork()) {
                                provider.runLightUpdates();
                            }
                        }
                    };
                    if (mc.isSameThread()) {
                        doLightUpdates.run();
                    } else {
                        mc.schedule(doLightUpdates);
                    }
                }
            } catch (Exception e) {
                // We'd rather not have a failure parsing one packet screw up the whole replay process
                e.printStackTrace();
            }
        }

    }

    // If we do not give minecraft time to tick, there will be dead entity artifacts left in the world
    // Therefore we have to remove all loaded, dead entities manually if we are in sync mode.
    // We do this after every SpawnX packet and after the destroy entities packet.
    private void maybeRemoveDeadEntities(Packet<?> packet) {
        if (asyncMode) {
            return; // MC should have enough time to tick
        }

        boolean relevantPacket = packet instanceof ClientboundAddEntityPacket
                || packet instanceof ClientboundRemoveEntitiesPacket;
        if (!relevantPacket) {
            return; // don't want to do it too often, only when there's likely to be a dead entity
        }

        mc.schedule(() -> {
            ClientLevel world = mc.level;
        });
    }

    /**
     * Process a packet and return the result.
     * @param p The packet to process
     * @return The processed packet or {@code null} if no packet shall be sent
     */
    protected Packet<?> processPacket(Packet<?> p) throws Exception {
        if (p instanceof ClientboundSetBorderLerpSizePacket || p instanceof ClientboundInitializeBorderPacket) {
            BORDER_PACKET_TIMES.put(p, sendingTimeStamp);
        }
        if (p instanceof ClientboundLoginFinishedPacket) {
            registry = registry.withLoginSuccess();
            return p;
        }
        if (p instanceof ClientboundFinishConfigurationPacket) {
            registry = registry.withState(State.PLAY);
            return p;
        }
        if (p instanceof ClientboundStartConfigurationPacket) {
            registry = registry.withState(State.CONFIGURATION);
            hasWorldLoaded = false;
            return p;
        }

        if (p instanceof ClientboundCustomPayloadPacket packet) {
            if (Restrictions.PLUGIN_CHANNEL.equals(packet.payload().type().id())) {
                final String unknown = replayHandler.getRestrictions().handle(packet);
                if (unknown == null) {
                    return null;
                } else {
                    // Failed to parse options, make sure that under no circumstances further packets are parsed
                    terminateReplay();
                    // Then end replay and show error GUI
                    SceneCraft.instance.runLater(() -> {
                        try {
                            replayHandler.endReplay();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mc.gui.setScreen(new AlertScreen(
                                () -> mc.gui.setScreen(null),
                                Component.translatable("scenecraft.error.unknownrestriction1"),
                                Component.translatable("scenecraft.error.unknownrestriction2", unknown)
                        ));
                    });
                }
            }
        }
        if (p instanceof ClientboundDisconnectPacket) {
            Component reason = ((ClientboundDisconnectPacket) p).reason();
            String message = reason.getString();
            if ("Please update to view this replay.".equals(message)) {
                // This version of the mod supports replay restrictions so we are allowed
                // to remove this packet.
                return null;
            }
        }

        if(BAD_PACKETS.contains(p.getClass())) return null;

        if (p instanceof ClientboundCustomPayloadPacket packet) {
            Identifier channelName = packet.payload().type().id();
            String channelNameStr = channelName.toString();

            if (channelNameStr.startsWith("voicechat:")) {
                // The recorded Simple Voice Chat handshake (secret, player state, ...) belongs to a live server session.
                // Replaying it makes the voice chat client try to connect to the original server.
                return null;
            }
        }

        if(p instanceof ClientboundResourcePackPushPacket packet) {
            String url = packet.url();
            if (url.startsWith("replay://")) {
                int id = Integer.parseInt(url.substring("replay://".length()));
                Map<Integer, String> index = replayFile.getResourcePackIndex();
                if (index != null) {
                    String hash = index.get(id);
                    if (hash != null) {
                        File file = new File(tempResourcePackFolder, hash + ".zip");
                        if (!file.exists()) {
                            IOUtils.copy(replayFile.getResourcePack(hash).get(), new FileOutputStream(file));
                        }
                        schedulePacketHandler(() -> mc.getDownloadedPackSource().pushLocalPack(packet.id(), file.toPath()));
                    }
                }
                return null;
            }
        }

        if(p instanceof ClientboundLoginPacket packet) {
            int entId = packet.playerId();
            schedulePacketHandler(() -> allowMovement = true);
            actualID = entId;
            entId = -1789435; // Camera entity net should be negative which is an invalid net and can't be used by servers
            p = new ClientboundLoginPacket(
                    entId,
                    packet.hardcore(),
                    packet.levels(),
                    packet.maxPlayers(),
                    packet.chunkRadius(),
                    packet.simulationDistance(),
                    packet.reducedDebugInfo(),
                    packet.showDeathScreen(),
                    packet.doLimitedCrafting(),
                    withSpectatorMode(packet.commonPlayerSpawnInfo()),
                    packet.onlineMode(),
                    packet.enforcesSecureChat()
            );
        }

        if(p instanceof ClientboundRespawnPacket respawn) {
            p = new ClientboundRespawnPacket(
                    withSpectatorMode(respawn.commonPlayerSpawnInfo()),
                    (byte) 0
            );

            schedulePacketHandler(() -> allowMovement = true);
        }

        if(p instanceof ClientboundPlayerPositionPacket ppl) {
            if(!hasWorldLoaded) hasWorldLoaded = true;

            SceneCraft.instance.runLater(() -> {
                if (mc.gui.screen() instanceof LevelLoadingScreen) {
                    // Close the world loading screen manually in case we swallow the packet
                    mc.gui.setScreen(null);
                }
            });

            for (Relative relative : ppl.relatives()) {
                if (relative == Relative.X || relative == Relative.Y || relative == Relative.Z) {
                    return null; // At least one of the coordinates is relative, so we don't care
                }
            }

            schedulePacketHandler(new Runnable() {
                @Override
                @SuppressWarnings("unchecked")
                public void run() {
                    // FIXME: world shouldn't ever be null at this point, now that we use the packet queue
                    //        probably fine to remove on the next non-patch version (don't want to break stuff now)
                    if (mc.level == null || !mc.isSameThread()) {
                        SceneCraft.instance.runLater(this);
                        return;
                    }

                    CameraEntity cent = replayHandler.getCameraEntity();
                    if (!allowMovement && !((Math.abs(cent.getX() - ppl.change().position().x) > TP_DISTANCE_LIMIT) ||
                            (Math.abs(cent.getZ() - ppl.change().position().z) > TP_DISTANCE_LIMIT))) {
                        return;
                    } else {
                        allowMovement = false;
                    }
                    cent.setCameraPosition(ppl.change().position().x, ppl.change().position().y, ppl.change().position().z);
                    cent.setCameraRotation(ppl.change().yRot(), ppl.change().xRot(), cent.roll);
                }
            });

            return null;
        }

        if(p instanceof ClientboundGameEventPacket) {
            ClientboundGameEventPacket pg = (ClientboundGameEventPacket)p;
            // only allow the following packets:
            // 1 - End raining
            // 2 - Begin raining
            //
            // The following values are to control sky color (e.g. if thunderstorm)
            // 7 - Fade value
            // 8 - Fade time
            if (!Arrays.asList(
                    ClientboundGameEventPacket.START_RAINING,
                    ClientboundGameEventPacket.STOP_RAINING,
                    ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
                    ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE
            ).contains(pg.getEvent())) {
                return null;
            }
        }

        if (p instanceof ClientboundSystemChatPacket || p instanceof ClientboundPlayerChatPacket || p instanceof ClientboundDisguisedChatPacket) {
            if (!SceneCraftReplay.instance.getCore().getSettingsRegistry().get(Setting.SHOW_CHAT)) {
                return null;
            }
        }

        if (asyncMode) {
            return processPacketAsync(p);
        } else {
            Packet<?> fp = p;
            schedulePacketHandler(() -> processPacketSync(fp));
            return p;
        }
    }

    private CommonPlayerSpawnInfo withSpectatorMode(CommonPlayerSpawnInfo org) {
        return new CommonPlayerSpawnInfo(
                org.dimensionType(),
                org.dimension(),
                org.seed(),
                GameType.SPECTATOR,
                Optional.of(GameType.SPECTATOR),
                org.isDebug(),
                org.isFlat(),
                org.lastDeathLocation(),
                org.portalCooldown(),
                org.seaLevel()
        );
    }

    /**
     * Returns the speed of the replay. 1 being normal speed, 0.5 half and 2 twice as fast.
     * If 0 is returned, the replay is paused.
     * @return speed multiplier
     */
    @Override
    public double getReplaySpeed() {
        if(!paused()) return replaySpeed;
        else return 0;
    }

    /**
     * Set the speed of the replay. 1 being normal speed, 0.5 half and 2 twice as fast.
     * The speed may not be set to 0 nor to negative values.
     * @param d Speed multiplier
     */
    @Override
    public void setReplaySpeed(final double d) {
        if (d != 0) {
            this.replaySpeed = d;
            this.realTimeStartSpeed = d;
            this.realTimeStart = System.currentTimeMillis() - (long) (lastTimeStamp / d);
        }
        DeltaTracker.Timer timer = mc.deltaTracker;
        timer.msPerTick = DEFAULT_MS_PER_TICK / (float) d;
    }

    /////////////////////////////////////////////////////////
    //       Asynchronous packet processing                //
    /////////////////////////////////////////////////////////

    /**
     * Timestamp in milliseconds of when we started (or would have started when taking pauses and speed into account)
     * the playback of the replay.
     * Updated only when replay speed changes or on pause/unpause but definitely not on every packet to prevent gradual
     * drifting.
     */
    private long realTimeStart;

    /**
     * The replay speed used for {@link #realTimeStart}.
     * If the target speed differs from this one, the timestamp is recalculated.
     */
    private double realTimeStartSpeed;

    /**
     * There is no waiting performed until a packet with at least this timestamp is reached (but not yet sent).
     * If this is -1, then timing is normal.
     */
    private long desiredTimeStamp = -1;

    /**
     * Runnable which performs timed dispatching of packets from the input stream.
     */
    private Runnable asyncSender = new Runnable() {
        public void run() {
            try {
                REPLAY_LOOP:
                while (!terminate) {
                    synchronized (FullReplaySender.this) {
                        if (replayIn == null) {
                            replayIn = replayFile.getPacketData(getPacketTypeRegistry(State.LOGIN));
                        }
                        // Packet loop
                        while (true) {
                            try {
                                // When playback is paused and the world has loaded (we don't want any dirt-screens) we sleep
                                while (paused() && hasWorldLoaded && !inBundle) {
                                    // Unless we are going to terminate, restart or jump
                                    if (terminate || startFromBeginning || desiredTimeStamp != -1) {
                                        break;
                                    }
                                    Thread.sleep(10);
                                }

                                if (terminate && !inBundle) {
                                    break REPLAY_LOOP;
                                }

                                if (startFromBeginning) {
                                    // In case we need to restart from the beginning
                                    // break out of the loop sending all packets which will
                                    // cause the replay to be restarted by the outer loop
                                    break;
                                }

                                // Read the next packet if we don't already have one
                                if (nextPacket == null) {
                                    nextPacket = new PacketData(replayIn);
                                }

                                int nextTimeStamp = nextPacket.timestamp;

                                // If we aren't jumping and the world has already been loaded (no dirt-screens) then wait
                                // the required amount to get proper packet timing
                                if (!isHurrying() && hasWorldLoaded && !inBundle) {
                                    // Timestamp of when the next packet should be sent
                                    long expectedTime = realTimeStart + (long) (nextTimeStamp / replaySpeed);
                                    long now = System.currentTimeMillis();
                                    // If the packet should not yet be sent, wait a bit
                                    if (expectedTime > now) {
                                        Thread.sleep(expectedTime - now);
                                    }
                                }

                                // Process packet
                                if (nextPacket.type == PacketType.Bundle) inBundle = !inBundle;
                                sendingTimeStamp = nextTimeStamp;
                                channel.pipeline().fireChannelRead(Unpooled.wrappedBuffer(nextPacket.bytes));
                                nextPacket = null;

                                lastTimeStamp = nextTimeStamp;

                                // MC as of 1.20.2 relies on autoRead, so it can update the connection state on the main
                                // thread before the next packet is read. As such, we need to stall if that was just
                                // enabled.
                                // Might be safe to do the same on older versions too, but I'd rather not poke the
                                // monster that is Forge networking.
                                while (!channel.config().isAutoRead()) {
                                    Thread.sleep(0, 100_000);
                                }

                                // In case we finished jumping
                                // We need to check that we aren't planing to restart so we don't accidentally run this
                                // code before we actually restarted
                                if (isHurrying() && lastTimeStamp > desiredTimeStamp && !startFromBeginning) {
                                    desiredTimeStamp = -1;

                                    replayHandler.moveCameraToTargetPosition();

                                    // Pause after jumping (this will also reset realTimeStart accordingly)
                                    setReplaySpeed(0);
                                }
                            } catch (EOFException eof) {
                                // Reached end of file
                                // Pause the replay which will cause it to freeze before getting restarted
                                setReplaySpeed(0);
                                // Then wait until the user tells us to continue
                                while (paused() && hasWorldLoaded && desiredTimeStamp == -1 && !terminate) {
                                    Thread.sleep(10);
                                }

                                if (terminate) {
                                    break REPLAY_LOOP;
                                }
                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        // Restart the replay.
                        hasWorldLoaded = false;
                        inBundle = false;
                        lastTimeStamp = 0;
                        registry = getPacketTypeRegistry(State.LOGIN);
                        startFromBeginning = false;
                        nextPacket = null;
                        realTimeStart = System.currentTimeMillis();
                        if (replayIn != null) {
                            replayIn.close();
                            replayIn = null;
                        }
                        SceneCraft.instance.runSync(replayHandler::restartedReplay);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Return whether this replay sender is currently rushing. When rushing, all packets are sent without waiting until
     * a specified timestamp is passed.
     * @return {@code true} if currently rushing, {@code false} otherwise
     */
    public boolean isHurrying() {
        return desiredTimeStamp != -1;
    }

    /**
     * Return the timestamp to which this replay sender is currently rushing. All packets with an lower or equal
     * timestamp will be sent out without any sleeping.
     * @return The timestamp in milliseconds since the start of the replay
     */
    public long getDesiredTimestamp() {
        return desiredTimeStamp;
    }

    /**
     * Jumps to the specified timestamp when in async mode by rushing all packets until one with a timestamp greater
     * than the specified timestamp is found.
     * If the timestamp has already passed, this causes the replay to restart and then rush all packets.
     * @param millis Timestamp in milliseconds since the start of the replay
     */
    @Override
    public void jumpToTime(int millis) {
        Preconditions.checkState(asyncMode, "Can only jump in async mode. Use sendPacketsTill(int) instead.");
        if(millis < lastTimeStamp && !isHurrying()) {
            startFromBeginning = true;
        }

        desiredTimeStamp = millis;
    }

    protected Packet<?> processPacketAsync(Packet<?> p) {
        //If hurrying, ignore some packets, except for short durations
        if(desiredTimeStamp - lastTimeStamp > 1000) {
            if(p instanceof ClientboundLevelParticlesPacket) return null;

            if(p instanceof ClientboundAddEntityPacket) {
                ClientboundAddEntityPacket pso = (ClientboundAddEntityPacket)p;
                if (pso.getType() == EntityTypes.FIREWORK_ROCKET) return null;
            }
        }
        return p;
    }

    /////////////////////////////////////////////////////////
    //        Synchronous packet processing                //
    /////////////////////////////////////////////////////////

    // Even in sync mode, we send from another thread because mods may rely on that
    private final ExecutorService syncSender = Executors.newSingleThreadExecutor(runnable ->
            new Thread(runnable, "scenecraft-sync-sender"));

    /**
     * Sends all packets until the specified timestamp is reached (inclusive).
     * If the timestamp is smaller than the last packet sent, the replay is restarted from the beginning.
     * @param timestamp The timestamp in milliseconds since the beginning of this replay
     */
    @Override
    public void sendPacketsTill(int timestamp) {
        Preconditions.checkState(!asyncMode, "This method cannot be used in async mode. Use jumpToTime(int) instead.");

        // Submit our target to the sender thread and track its progress
        AtomicBoolean doneSending = new AtomicBoolean();
        syncSender.submit(() -> {
            try {
                doSendPacketsTill(timestamp);
            } finally {
                doneSending.set(true);
            }
        });

        // Drain the task queue while we are sending (in case a mod blocks the io thread waiting for the main thread)
        while (!doneSending.get()) {
            executeTaskQueue();

            // Wait until the sender thread has made progress
            try {
                //noinspection BusyWait
                Thread.sleep(0, 100_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        // Everything has been sent, drain the queue one last time
        executeTaskQueue();
    }

    private void doSendPacketsTill(int timestamp) {
        try {
            synchronized (this) {
                if (timestamp == lastTimeStamp) { // Do nothing if we're already there
                    return;
                }
                if (timestamp < lastTimeStamp) { // Restart the replay if we need to go backwards in time
                    hasWorldLoaded = false;
                    inBundle = false;
                    lastTimeStamp = 0;
                    if (replayIn != null) {
                        replayIn.close();
                        replayIn = null;
                    }
                    registry = getPacketTypeRegistry(State.LOGIN);
                    startFromBeginning = false;
                    nextPacket = null;
                    SceneCraft.instance.runSync(replayHandler::restartedReplay);
                }

                if (replayIn == null) {
                    replayIn = replayFile.getPacketData(getPacketTypeRegistry(State.LOGIN));
                }

                while (true) { // Send packets
                    try {
                        PacketData pd;
                        if (nextPacket != null) {
                            // If there is still a packet left from before, use it first
                            pd = nextPacket;
                            nextPacket = null;
                        } else {
                            // Otherwise read one from the input stream
                            pd = new PacketData(replayIn);
                        }

                        int nextTimeStamp = pd.timestamp;
                        if (nextTimeStamp > timestamp && !inBundle) {
                            // We are done sending all packets
                            nextPacket = pd;
                            break;
                        }

                        // Process packet
                        if (pd.type == PacketType.Bundle) inBundle = !inBundle;
                        sendingTimeStamp = nextTimeStamp;
                        channel.pipeline().fireChannelRead(Unpooled.wrappedBuffer(pd.bytes));

                        // MC as of 1.20.2 relies on autoRead, so it can update the connection state on the main
                        // thread before the next packet is read. As such, we need to stall if that was just
                        // enabled.
                        // Might be safe to do the same on older versions too, but I'd rather not poke the
                        // monster that is Forge networking.
                        while (!channel.config().isAutoRead()) {
                            Thread.sleep(0, 100_000);
                        }
                    } catch (EOFException eof) {
                        // Shit! We hit the end before finishing our job! What shall we do now?
                        // well, let's just pretend we're done...
                        replayIn = null;
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // This might be required if we change to async mode anytime soon
                realTimeStart = System.currentTimeMillis() - (long) (timestamp / replaySpeed);
                lastTimeStamp = timestamp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void executeTaskQueue() {
        ((MinecraftMethodAccessor) mc).sceneCraftExecuteTaskQueue();
        SceneCraft.instance.runTasks();
    }

    /**
     * Runs the given runnable on the main thread as if it was a packet handler.
     * Note that the packet handler queue has different behavior than the standard SceneCraft queue.
     */
    private void schedulePacketHandler(Runnable runnable) {
        if (mc.isSameThread()) {
            runnable.run();
        } else {
            mc.packetProcessor().scheduleIfPossible(channel.pipeline().get(Connection.class).getPacketListener(), new Packet<>() {
                @Override
                public net.minecraft.network.protocol.@Nullable PacketType<? extends Packet<PacketListener>> type() {
                    return null;
                }
                @Override
                public void handle(@NonNull PacketListener listener) {
                    runnable.run();
                }
            });
        }
    }

    protected void processPacketSync(Packet<?> p) {
        if (p instanceof ClientboundForgetLevelChunkPacket packet) {
            int x = packet.pos().x();
            int z = packet.pos().z();
            // If the chunk is getting unloaded, we will have to forcefully update the position of all entities
            // within. Otherwise, if there wasn't a game tick recently, there may be entities that have moved
            // out of the chunk by now but are still registered in it. If we do not update those, they will get
            // unloaded even though they shouldn't.
            // Note: This is only half of the truth. Entities may be removed by chunk-unloading, see else-case below.
            // To make things worse, it seems like players were never supposed to be unloaded this way because
            // they will remain glitched in the World#playerEntities list.
            // 1.14+: The update issue remains but only for non-players and the unloading list bug appears to have been
            //        fixed (chunk unloading no longer removes the entities).
            // Get the chunk that will be unloaded
            ClientLevel world = mc.level;
            ChunkSource chunkProvider = world.getChunkSource();
            LevelChunk chunk = chunkProvider.getChunkNow(x, z);
            if (chunk != null) {
                List<Entity> entitiesInChunk = new ArrayList<>();
                // Gather all entities in that chunk
                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity.chunkPosition().equals(chunk.getPos())) {
                        entitiesInChunk.add(entity);
                    }
                }
                for (Entity entity : entitiesInChunk) {
                    // Skip interpolation of position updates coming from server
                    // (See: newX in EntityLivingBase or otherPlayerMPX in EntityOtherPlayerMP)
                    forcePositionForVehicleAndSelf(entity);

                    // Check whether the entity has left the chunk
                    // This is now handled automatically in Entity.setPos (called from tick())
                }
            }
        }
    }

    private void forcePositionForVehicleAndSelf(Entity entity) {
        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            forcePositionForVehicleAndSelf(vehicle);
        }

        // Skip interpolation of position updates coming from server
        // (See: newX in EntityLivingBase or otherPlayerMPX in EntityOtherPlayerMP)
        int ticks = 0;
        Vec3 prevPos;
        do {
            prevPos = entity.position();
            if (vehicle != null) {
                entity.rideTick();
            } else {
                entity.tick();
            }
        } while (prevPos.distanceToSqr(entity.position()) > 0.0001 && ticks++ < 100);
    }

    private static final class PacketData {
        private static final io.netty.buffer.ByteBuf byteBuf = Unpooled.buffer();
        private static final NetOutput netOutput = new ByteBufNetOutput(byteBuf);

        private final int timestamp;
        private final byte[] bytes;
        private final PacketType type;

        PacketData(ReplayInputStream in) throws IOException {
            net.patchedpixel.scenecraft.scenestudio.PacketData data = in.readPacket();
            if (data == null) {
                throw new EOFException();
            }
            timestamp = (int) data.getTime();
            net.patchedpixel.scenecraft.scenestudio.protocol.Packet packet = data.getPacket();
            type = packet.getType();
            // Workaround for ReplayMod 2.7.16-17 saving the LoginSuccess packet with an incorrect packet net
            // A fake one will have been sythesized by ReplayStudo, so we can simply drop the broken one.
            if (packet.getId() == -1) {
                bytes = new byte[0];
                return;
            }
            // We need to re-encode SceneStudio packets, so we can later decode them as NMS packets
            // The main reason we aren't reading them as NMS packets is that SceneStudio has its own
            // (version independent) packet representation which also handles the cache/squash filters.
            synchronized (byteBuf) {
                byteBuf.markReaderIndex(); // Mark the current reader and writer index (should be at start)
                byteBuf.markWriterIndex();

                netOutput.writeVarInt(packet.getId());
                int idSize = byteBuf.readableBytes();
                int contentSize = packet.getBuf().readableBytes();
                bytes = new byte[idSize + contentSize]; // Create bytes array of sufficient size
                byteBuf.readBytes(bytes, 0, idSize);
                packet.getBuf().readBytes(bytes, idSize, contentSize);

                byteBuf.resetReaderIndex(); // Reset reader & writer index for next use
                byteBuf.resetWriterIndex();
            }
            packet.getBuf().release();
        }
    }
}
