package net.patchedpixel.scenecraft.common.record.gui;

import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.core.gui.GuiBackgroundProcesses;
import net.patchedpixel.scenecraft.common.record.Setting;
import net.patchedpixel.scenecraft.common.replay.gui.screen.GuiReplayViewer;
import net.patchedpixel.scenecraft.common.scenestudio.replay.ReplayMetaData;
import net.patchedpixel.scenecraft.common.flamex.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.widget.ProgressBarWidget;
import net.patchedpixel.scenecraft.common.utils.Utils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.getMinecraft;

public class GuiSavingReplay {
    private static final Minecraft mc = getMinecraft();
    private static final Logger logger = LogManager.getLogger();

    private final GuiBackgroundProcesses.Process process = new GuiBackgroundProcesses.Process("scenecraft.gui.replaysaving.title");
    private final SceneCraft core;
    private final List<Runnable> apply = new ArrayList<>();

    public GuiSavingReplay(SceneCraft core) {
        this.core = core;
    }

    public void open() {
        core.getBackgroundProcesses().addProcess(process);
    }

    public void close() {
        core.getBackgroundProcesses().removeProcess(process);
        if(mc.screen instanceof GuiReplayViewer viewer) {
            viewer.list.load();
        }
    }

    public ProgressBarWidget getProgressBar() {
        return process.getProgressBar();
    }

    public void presentRenameDialog(List<Pair<Path, ReplayMetaData>> outputPaths) {
        core.getBackgroundProcesses().removeProcess(process);
        if(!core.getSettingsRegistry().get(Setting.RENAME_DIALOG)) {
            for(Pair<Path, ReplayMetaData> output : outputPaths) {
                applyOutput(output.getKey(), Utils.fileNameToReplayName(output.getKey().getFileName().toString()));
            }
            close();
            return;
        }
        mc.setScreen(new RenameScreen(mc.screen, outputPaths));
    }

    private class RenameScreen extends FlameableScreen {
        private final List<Pair<Path, ReplayMetaData>> outputs;
        private final List<EditBox> fields = new ArrayList<>();

        RenameScreen(Screen parent, List<Pair<Path, ReplayMetaData>> outputs) {
            super("scenecraft.gui.replaysaving.title", parent, 4);
            this.outputs = outputs;
        }

        @Override
        protected void build(LinearLayout root) {
            fields.clear();
            for(Pair<Path, ReplayMetaData> output : outputs) {
                Path path = output.getKey();
                EditBox field = FlameX.textField(130, Utils.fileNameToReplayName(path.getFileName().toString()), null, null);
                field.setHint(Component.translatable("scenecraft.gui.delete"));
                field.setTooltip(Tooltip.create(MCVer.literalText(path.getFileName().toString())));
                fields.add(field);
                LinearLayout row = FlameX.row(0);
                row.addChild(field);
                row.addChild(FlameX.button(MCVer.literalText("X"), 20, "scenecraft.gui.delete", () -> field.setValue("")));
                root.addChild(row);
            }
            root.addChild(FlameX.button("scenecraft.gui.done", 150, this::apply));
        }

        private void apply() {
            for(int i = 0; i < outputs.size(); i++) {
                applyOutput(outputs.get(i).getKey(), fields.get(i).getValue());
            }
            GuiSavingReplay.this.close();
            MCVer.getMinecraft().setScreen(parent);
        }

        @Override
        public void onClose() {
            apply();
        }
    }

    private void applyOutput(Path path, String newName) {
        if(newName.isEmpty()) {
            try {
                Files.delete(path);
            } catch(IOException e) {
                logger.error("Deleting replay file:", e);
                CrashReport crashReport = CrashReport.forThrowable(e, "Deleting replay file");
                core.runLater(() -> Utils.error(logger, crashReport, () -> {}));
            }
            return;
        }
        try {
            Path replaysFolder = core.folders.getReplayFolder();
            Path newPath = Utils.replayNameToPath(replaysFolder, newName);
            for(int i = 1; Files.exists(newPath); i++) {
                newPath = Utils.replayNameToPath(replaysFolder, newName + " (" + i + ")");
            }
            Files.move(path, newPath);
        } catch(IOException e) {
            logger.error("Renaming replay file:", e);
            CrashReport crashReport = CrashReport.forThrowable(e, "Renaming replay file");
            core.runLater(() -> Utils.error(logger, crashReport, () -> {}));
        }
    }
}
