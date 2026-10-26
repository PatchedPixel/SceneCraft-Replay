package net.patchedpixel.scenecraft.record.packet;

import net.patchedpixel.scenecraft.scenestudio.protocol.State;

import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.core.utils.Restrictions;
import net.patchedpixel.scenecraft.platform.Utils;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.editor.gui.MarkerProcessor;
import net.patchedpixel.scenecraft.record.SceneCraftRecorder;
import net.patchedpixel.scenecraft.record.Setting;
import net.patchedpixel.scenecraft.record.gui.GuiSavingReplay;
import net.patchedpixel.scenecraft.record.handler.ConnectionEventHandler;
import net.patchedpixel.scenecraft.scenestudio.PacketData;
import net.patchedpixel.scenecraft.scenestudio.data.Marker;
import net.patchedpixel.scenecraft.scenestudio.io.ReplayOutputStream;
import net.patchedpixel.scenecraft.scenestudio.protocol.Packet;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayFile;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayMetaData;
import net.patchedpixel.scenecraft.gui.container.VanillaGuiScreen;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.CrashReport;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static net.patchedpixel.scenecraft.core.versions.MCVer.*;

@ChannelHandler.Sharable // so we can re-order it
public class PacketListener extends ChannelInboundHandlerAdapter {
    public static final String RAW_RECORDER_KEY = "replay_recorder_raw";
    public static final String DECODED_RECORDER_KEY = "replay_recorder_decoded";

    public static final String DECOMPRESS_KEY = "decompress";
    public static final String DECODER_KEY = "decoder";

    private static final Minecraft mc = getMinecraft();
    private static final Logger logger = LogManager.getLogger();

    private final SceneCraft core;
    private final Path outputPath;
    private final ReplayFile replayFile;

    private final ResourcePackRecorder resourcePackRecorder;

    private final ExecutorService saveService = Executors.newSingleThreadExecutor();
    private final ReplayOutputStream packetOutputStream;

    private ReplayMetaData metaData;

    private final Channel channel;
    private Packet currentRawPacket;

    private final long startTime;
    private long lastSentPacket;
    private long timePassedWhilePaused;
    private volatile boolean serverWasPaused;

    /**
     * Used to keep track of the last metadata save job submitted to the save service and
     * as such prevents unnecessary writes.
     */
    private final AtomicInteger lastSaveMetaDataId = new AtomicInteger();

    public PacketListener(SceneCraft core, Channel channel, Path outputPath, ReplayFile replayFile, ReplayMetaData metaData) throws IOException {
        this.core = core;
        this.channel = channel;
        this.outputPath = outputPath;
        this.replayFile = replayFile;
        this.metaData = metaData;
        this.resourcePackRecorder = new ResourcePackRecorder(replayFile);
        this.packetOutputStream = replayFile.writePacketData();
        this.startTime = metaData.getDate();

        saveMetaData();
    }

    private void saveMetaData() {
        int id = lastSaveMetaDataId.incrementAndGet();
        saveService.submit(() -> {
            if (lastSaveMetaDataId.get() != id) {
                return; // Another job has been scheduled, it will do the hard work.
            }
            try {
                synchronized (replayFile) {
                    replayFile.writeMetaData(MCVer.getPacketTypeRegistry(State.LOGIN), metaData);
                }
            } catch (IOException e) {
                logger.error("Writing metadata:", e);
            }
        });
    }

    public void save(net.minecraft.network.protocol.Packet<?> packet) {
        Packet encoded;
        try {
            encoded = encodeMcPacket(getConnectionState(), packet);
        } catch (Exception e) {
            logger.error("Encoding packet:", e);
            return;
        }
        save(encoded);
    }

    public void save(Packet packet) {
        // If we're not on the main thread (i.e. we're on the netty thread), then we need to schedule the saving
        // to happen on the main thread so we can guarantee correct ordering of inbound and inject packets.
        // Otherwise, injected packets may end up further down the packet stream than they were supposed to and other
        // inbound packets which may rely on the injected packet would behave incorrectly when played back.
        if (!mc.isSameThread()) {
            mc.execute(() -> save(packet));
            return;
        }
        try {
            long now = System.currentTimeMillis();
            if (serverWasPaused) {
                timePassedWhilePaused = now - startTime - lastSentPacket;
                serverWasPaused = false;
            }
            int timestamp = (int) (now - startTime - timePassedWhilePaused);
            lastSentPacket = timestamp;
            PacketData packetData = new PacketData(timestamp, packet);
            saveService.submit(() -> {
                try {
                    packetOutputStream.write(packetData);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch(Exception e) {
            logger.error("Writing packet:", e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        metaData.setDuration((int) lastSentPacket);
        saveMetaData();

        core.runLater(() -> {
            ConnectionEventHandler connectionEventHandler = SceneCraftRecorder.instance.getConnectionEventHandler();
            if (connectionEventHandler.getPacketListener() == this) {
                connectionEventHandler.reset();
            }
        });

        GuiSavingReplay guiSavingReplay = new GuiSavingReplay(core);
        new Thread(() -> {
            core.runLater(guiSavingReplay::open);

            saveService.shutdown();
            try {
                saveService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                logger.error("Waiting for save service termination:", e);
            }
            try {
                packetOutputStream.close();
            } catch (IOException e) {
                logger.error("Failed to close packet output stream:", e);
            }

            List<Pair<Path, ReplayMetaData>> outputPaths;
            synchronized (replayFile) {
                try {
                    if (!MarkerProcessor.producesAnyOutput(replayFile)) {
                        // Immediately close the saving popup, the user doesn't care about it
                        core.runLater(guiSavingReplay::close);

                        // If we crash right here, on the next start we'll prompt the user for recovery
                        // but we don't really want that, so drop a marker file to skip recovery for this replay.
                        Path noRecoverMarker = outputPath.resolveSibling(outputPath.getFileName() + ".no_recover");
                        Files.createFile(noRecoverMarker);

                        // We still have the replay, so we just save it (at least for a few weeks) in case they change their mind
                        String replayName = FilenameUtils.getBaseName(outputPath.getFileName().toString());
                        Path rawFolder = SceneCraft.instance.folders.getRawReplayFolder();
                        Path rawPath = rawFolder.resolve(outputPath.getFileName());
                        for (int i = 1; Files.exists(rawPath); i++) {
                            rawPath = rawPath.resolveSibling(replayName + "." + i + ".mcpr");
                        }
                        replayFile.saveTo(rawPath.toFile());
                        replayFile.close();

                        // Done, clean up the marker
                        Files.delete(noRecoverMarker);
                        return;
                    }

                    replayFile.save();
                    replayFile.close();

                    if (core.getSettingsRegistry().get(Setting.AUTO_POST_PROCESS)) {
                        outputPaths = MarkerProcessor.apply(outputPath, guiSavingReplay.getProgressBar()::setProgress);
                    } else {
                        outputPaths = Collections.singletonList(Pair.of(outputPath, metaData));
                    }
                } catch (Exception e) {
                    logger.error("Saving replay file:", e);
                    CrashReport crashReport = CrashReport.forThrowable(e, "Saving replay file");
                    core.runLater(() -> Utils.error(logger, VanillaGuiScreen.wrap(mc.screen), crashReport, guiSavingReplay::close));
                    return;
                }
            }

            core.runLater(() -> guiSavingReplay.presentRenameDialog(outputPaths));
        }).start();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ConnectionProtocol connectionState = getConnectionState();

        Packet packet = null;
        if (msg instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) msg;
            if (buf.readableBytes() > 0) {
                packet = decodePacket(connectionState, buf);
            }
        } else if (msg instanceof net.minecraft.network.protocol.Packet<?> mcPacket) {
            List<Packet> packets = new ArrayList<>(1);
            BundlerInfo.Provider provider = ctx.channel().attr(BundlerInfo.BUNDLER_PROVIDER).get();
            if (provider != null) {
                provider.getBundlerInfo(PacketFlow.CLIENTBOUND).unbundlePacket(mcPacket, unbundled -> {
                    try {
                        packets.add(encodeMcPacket(connectionState, unbundled));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                packets.add(encodeMcPacket(connectionState, mcPacket));
            }
            if (packets.size() > 1) {
                packets.forEach(this::save);
                super.channelRead(ctx, msg);
                return;
            }
            packet = packets.isEmpty() ? null : packets.get(0);
        }

        currentRawPacket = packet;
        try {
            super.channelRead(ctx, msg);
        } finally {
            if (currentRawPacket != null) {
                currentRawPacket.release();
                currentRawPacket = null;
            }
        }
    }

    private ConnectionProtocol getConnectionState() {
        ConnectionProtocol state = channel.attr(Connection.ATTRIBUTE_PROTOCOL).get();
        return state != null ? state : ConnectionProtocol.LOGIN;
    }

    private Packet encodeMcPacket(ConnectionProtocol connectionState, net.minecraft.network.protocol.Packet<?> packet) throws Exception {
        if (connectionState == ConnectionProtocol.PLAY && ConnectionProtocol.getProtocolForPacket(packet) == ConnectionProtocol.LOGIN) {
            connectionState = ConnectionProtocol.LOGIN;
        }
        int packetId = connectionState.getPacketId(PacketFlow.CLIENTBOUND, packet);
        if (packetId == -1) {
            throw new IOException("Unknown packet type: " + packet.getClass());
        }
        ByteBuf byteBuf = Unpooled.buffer();
        try {
            FriendlyByteBuf packetBuf = new FriendlyByteBuf(byteBuf);
            packetBuf.writeVarInt(packetId);
            packet.write(packetBuf);
            return decodePacket(connectionState, byteBuf);
        } finally {
            byteBuf.release();
        }
    }

    private static Packet decodePacket(ConnectionProtocol connectionState, ByteBuf buf) {
        FriendlyByteBuf packetBuf = new FriendlyByteBuf(buf.slice());
        int packetId = packetBuf.readVarInt();
        byte[] bytes = new byte[packetBuf.readableBytes()];
        packetBuf.readBytes(bytes);
        return new Packet(
                MCVer.getPacketTypeRegistry(connectionState),
                packetId,
                Unpooled.wrappedBuffer(bytes)
        );
    }

    public void addMarker(String name) {
        addMarker(name, (int) getCurrentDuration());
    }

    public void addMarker(String name, int timestamp) {
        Entity view = mc.getCameraEntity();

        Marker marker = new Marker();
        marker.setName(name);
        marker.setTime(timestamp);
        if (view != null) {
            marker.setX(view.getX());
            marker.setY(view.getY());
            marker.setZ(view.getZ());
            marker.setYaw(view.getYRot());
            marker.setPitch(view.getXRot());
        }
        // Roll is always 0
        saveService.submit(() -> {
            synchronized (replayFile) {
                try {
                    Set<Marker> markers = replayFile.getMarkers().orElseGet(HashSet::new);
                    markers.add(marker);
                    replayFile.writeMarkers(markers);
                } catch (IOException e) {
                    logger.error("Writing markers:", e);
                }
            }
        });
    }

    public long getCurrentDuration() {
        return lastSentPacket;
    }

    public void setServerWasPaused() {
        this.serverWasPaused = true;
    }

    public ResourcePackRecorder getResourcePackRecorder() {
        return resourcePackRecorder;
    }

    private void inspectDecoded(Object msg) {
        if (msg instanceof ClientboundCustomPayloadPacket packet) {
            if (Restrictions.PLUGIN_CHANNEL.equals(packet.getIdentifier())) {
                save(new ClientboundDisconnectPacket(net.minecraft.network.chat.Component.literal("Please update to view this replay.")));
            }
        }

        if (msg instanceof ClientboundAddPlayerPacket packet) {
            UUID uuid = packet.getPlayerId();
            Set<String> uuids = new HashSet<>(Arrays.asList(metaData.getPlayers()));
            uuids.add(uuid.toString());
            metaData.setPlayers(uuids.toArray(new String[0]));
            saveMetaData();
        }
    }

    public class DecodedPacketListener extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

            if (msg instanceof ClientboundHelloPacket) {
                super.channelRead(ctx, msg);
                return;
            }
            if (msg instanceof ClientboundLoginCompressionPacket) {
                super.channelRead(ctx, msg);
                return;
            }

            if (msg instanceof BundlePacket<?> bundlePacket) {
                for (net.minecraft.network.protocol.Packet<?> sub : bundlePacket.subPackets()) {
                    inspectDecoded(sub);
                }
                super.channelRead(ctx, msg);
                return;
            }

            inspectDecoded(msg);

            if (msg instanceof ClientboundResourcePackPacket) {
                save(resourcePackRecorder.handleResourcePack((ClientboundResourcePackPacket) msg));
                super.channelRead(ctx, msg);
                return;
            }

            if (currentRawPacket != null) {
                save(currentRawPacket);
                currentRawPacket = null;
            }

            super.channelRead(ctx, msg);
        }
    }
}
