package net.patchedpixel.scenecraft.common.core.gui;

import com.google.common.io.Files;
import com.google.gson.Gson;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.utils.Utils;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.scenestudio.io.ReplayInputStream;
import net.patchedpixel.scenecraft.common.scenestudio.io.ReplayOutputStream;
import net.patchedpixel.scenecraft.common.scenestudio.protocol.State;
import net.patchedpixel.scenecraft.common.scenestudio.replay.ReplayFile;
import net.patchedpixel.scenecraft.common.scenestudio.replay.ReplayMetaData;
import net.minecraft.CrashReport;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.flamex.screen.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

import static net.patchedpixel.scenecraft.common.scenestudio.util.Utils.readInt;
import static net.patchedpixel.scenecraft.common.scenestudio.util.Utils.writeInt;

public class RestoreReplayGui extends FlameableScreen {
    private static Logger LOGGER = LogManager.getLogger();

    public final File file;
    private final SceneCraft core;

    public RestoreReplayGui(SceneCraft core, Screen parent, File file) {
        super(Component.empty(), parent, 20);
        this.core = core;
        this.file = file;
        LOGGER.info("Found partially saved replay, offering recovery: " + file);
    }

    @Override
    protected void build(LinearLayout root) {
        LinearLayout text = FlameX.column(3);
        text.addChild(FlameX.label("scenecraft.gui.restorereplay1"));
        text.addChild(FlameX.label(FlameX.t("scenecraft.gui.restorereplay2", Files.getNameWithoutExtension(file.getName()))));
        text.addChild(FlameX.label("scenecraft.gui.restorereplay3"));
        root.addChild(text);

        LinearLayout buttons = FlameX.row(5);
        buttons.addChild(FlameX.button("gui.yes", 150, () -> {
            LOGGER.info("Attempting recovery: " + file);
            recoverInBackground();
            MCVer.getMinecraft().setScreen(parent);
        }));
        buttons.addChild(FlameX.button("gui.no", 150, () -> {
            LOGGER.info("Recovery rejected, marking for deletion: " + file);
            try {
                File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
                File deleted = new File(file.getParentFile(), file.getName() + ".del");
                if(deleted.exists()) {
                    FileUtils.deleteDirectory(deleted);
                }
                Files.move(tmp, deleted);
            } catch(IOException e) {
                e.printStackTrace();
            }
            MCVer.getMinecraft().setScreen(parent);
        }));
        root.addChild(buttons);
    }

    private void recoverInBackground() {
        GuiBackgroundProcesses.Process savingProcess = new GuiBackgroundProcesses.Process("scenecraft.gui.replaysaving.title");
        new Thread(() -> {
            core.runLater(() -> core.getBackgroundProcesses().addProcess(savingProcess));
            try {
                tryRecover(savingProcess.getProgressBar()::setProgress);
            } catch(IOException e) {
                LOGGER.error("Recovering replay file:", e);
                CrashReport crashReport = CrashReport.forThrowable(e, "Recovering replay file");
                core.runLater(() -> Utils.error(LOGGER, crashReport, () -> {}));
            } finally {
                core.runLater(() -> core.getBackgroundProcesses().removeProcess(savingProcess));
            }
        }).start();
    }

    private void tryRecover(Consumer<Float> progress) throws IOException {
        ReplayFile replayFile = SceneCraft.instance.files.open(file.toPath());
        // Commit all not-yet-committed files into the main zip file.
        // If we don't do this, then re-writing packet data below can actually overwrite uncommitted packet data!
        replayFile.save();
        progress.accept(0.4f);
        ReplayMetaData metaData = replayFile.getMetaData();
        if(metaData != null && metaData.getDuration() == 0) {
            // Try to restore replay duration
            // We need to re-write the packet data in case there are any incomplete packets dangling at the end
            try (ReplayInputStream in = replayFile.getPacketData(MCVer.getPacketTypeRegistry(State.LOGIN));
                 ReplayOutputStream out = replayFile.writePacketData()) {
                while(true) {
                    // To prevent failing at un-parsable packets and to support recovery in minimal mode,
                    // we do not use the ReplayIn/OutputStream methods but instead parse the packets ourselves.
                    int time = readInt(in);
                    int length = readInt(in);
                    if(time == -1 || length == -1) {
                        break;
                    }
                    byte[] buf = new byte[length];
                    IOUtils.readFully(in, buf);

                    // Fully read, update replay duration
                    metaData.setDuration(time);

                    // Write packet back into recovered replay
                    writeInt(out, time);
                    writeInt(out, length);
                    out.write(buf);
                }
            } catch(Throwable t) {
                t.printStackTrace();
            }
            // Write back the actual duration
            try (OutputStream out = replayFile.write("metaData.json")) {
                metaData.setGenerator(metaData.getGenerator() + "(+ SceneCraft Replay Recovery)");
                String json = (new Gson()).toJson(metaData);
                out.write(json.getBytes());
            }
        }
        progress.accept(0.6f);
        replayFile.save();
        progress.accept(0.9f);
        replayFile.close();
        progress.accept(1f);
    }
}
