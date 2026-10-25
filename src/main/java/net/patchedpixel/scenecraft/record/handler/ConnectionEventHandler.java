package net.patchedpixel.scenecraft.record.handler;

import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.core.utils.ModCompat;
import net.patchedpixel.scenecraft.platform.Utils;
import net.patchedpixel.scenecraft.editor.gui.MarkerProcessor;
import net.patchedpixel.scenecraft.record.ServerInfoExt;
import net.patchedpixel.scenecraft.record.Setting;
import net.patchedpixel.scenecraft.record.gui.GuiRecordingControls;
import net.patchedpixel.scenecraft.record.gui.GuiRecordingOverlay;
import net.patchedpixel.scenecraft.record.packet.PacketListener;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayFile;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayMetaData;
import io.netty.channel.Channel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.Connection;
import org.apache.logging.log4j.Logger;
import net.minecraft.world.level.Level;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static net.patchedpixel.scenecraft.core.versions.MCVer.getMinecraft;

/**
 * Handles connection events and initiates record if enabled.
 */
public class ConnectionEventHandler {
    private static final String DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";
    private static final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    private static final Minecraft mc = getMinecraft();

    private final Logger logger;
    private final SceneCraft core;

    private RecordingEventHandler recordingEventHandler;
    private PacketListener packetListener;
    private GuiRecordingOverlay guiOverlay;
    private GuiRecordingControls guiControls;

    public ConnectionEventHandler(Logger logger, SceneCraft core) {
        this.logger = logger;
        this.core = core;
    }

    public void onConnectedToServerEvent(Connection networkManager) {
        try {
            boolean local = networkManager.isMemoryConnection();
            if (local) {
                if (mc.getSingleplayerServer().getLevel(Level.OVERWORLD).isDebug()) {
                    logger.info("Debug World record is not supported.");
                    return;
                }
                if(!core.getSettingsRegistry().get(Setting.RECORD_SINGLEPLAYER)) {
                    logger.info("Singleplayer Recording is disabled");
                    return;
                }
            } else {
                if(!core.getSettingsRegistry().get(Setting.RECORD_SERVER)) {
                    logger.info("Multiplayer Recording is disabled");
                    return;
                }
            }

            ServerData serverInfo;
            serverInfo = networkManager.getPacketListener() instanceof ClientHandshakePacketListenerImpl handler
                    ? handler.serverData
                    : null;

            String worldName;
            String serverName = null;
            boolean autoStart = core.getSettingsRegistry().get(Setting.AUTO_START_RECORDING);
            if (local) {
                worldName = mc.getSingleplayerServer().getWorldData().getLevelName();
                serverName = worldName;
            } else if (serverInfo != null && serverInfo.isRealm()) {
                // we can't access the server name without tapping too deep in the Realms Library
                worldName = "A Realms Server";
            } else if (serverInfo != null) {
                worldName = serverInfo.ip;
                if (!I18n.get("selectServer.defaultName").equals(serverInfo.name)) {
                    serverName = serverInfo.name;
                }

                Boolean autoStartServer = ServerInfoExt.from(serverInfo).getAutoRecording();
                if (autoStartServer != null) {
                    autoStart = autoStartServer;
                }
            } else {
                logger.info("Recording not started as the world is neither local nor remote (probably a replay).");
                return;
            }

            String name = sdf.format(Calendar.getInstance().getTime());
            Path outputPath = Utils.replayNameToPath(core.folders.getRecordingFolder(), name);
            ReplayFile replayFile = core.files.open(outputPath);

            replayFile.writeModInfo(ModCompat.getInstalledNetworkMods());

            ReplayMetaData metaData = new ReplayMetaData();
            metaData.setSingleplayer(local);
            metaData.setServerName(worldName);
            metaData.setCustomServerName(serverName);
            metaData.setGenerator("SceneCraft v" + SceneCraft.instance.getVersion());
            metaData.setDate(System.currentTimeMillis());
            metaData.setMcVersion(SceneCraft.instance.getMinecraftVersion());

            Channel channel = networkManager.channel();
            packetListener = new PacketListener(core, channel, outputPath, replayFile, metaData);

            String target = channel.pipeline().get("inbound_config") != null ? "inbound_config" : PacketListener.DECODER_KEY;
            channel.pipeline().addBefore(target, PacketListener.RAW_RECORDER_KEY, packetListener);
            channel.pipeline().addAfter(target, PacketListener.DECODED_RECORDER_KEY, packetListener.new DecodedPacketListener());

            recordingEventHandler = new RecordingEventHandler(packetListener);
            recordingEventHandler.register();

            guiControls = new GuiRecordingControls(core, packetListener, autoStart);
            guiControls.register();

            guiOverlay = new GuiRecordingOverlay(mc, core.getSettingsRegistry(), guiControls);
            guiOverlay.register();

            if (autoStart) {
                core.printInfoToChat("scenecraft.chat.recordingstarted");
            } else {
                packetListener.addMarker(MarkerProcessor.MARKER_NAME_START_CUT, 0);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            core.printWarningToChat("scenecraft.chat.recordingfailed");
        }
    }

    public void reset() {
        if (packetListener != null) {
            guiControls.unregister();
            guiControls = null;
            guiOverlay.unregister();
            guiOverlay = null;
            recordingEventHandler.unregister();
            recordingEventHandler = null;
            packetListener = null;
        }
    }

    public RecordingEventHandler getRecordingEventHandler() {
        return recordingEventHandler;
    }

    public PacketListener getPacketListener() {
        return packetListener;
    }
}
