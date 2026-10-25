package net.patchedpixel.scenecraft.record.gui;

import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.platform.Utils;
import net.patchedpixel.scenecraft.record.Setting;
import net.patchedpixel.scenecraft.replay.gui.screen.GuiReplayViewer;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayMetaData;
import net.patchedpixel.scenecraft.gui.container.AbstractGuiScreen;
import net.patchedpixel.scenecraft.gui.container.GuiContainer;
import net.patchedpixel.scenecraft.gui.container.GuiPanel;
import net.patchedpixel.scenecraft.gui.container.GuiScreen;
import net.patchedpixel.scenecraft.gui.container.VanillaGuiScreen;
import net.patchedpixel.scenecraft.gui.element.GuiButton;
import net.patchedpixel.scenecraft.gui.element.GuiLabel;
import net.patchedpixel.scenecraft.gui.element.GuiTextField;
import net.patchedpixel.scenecraft.gui.element.GuiTooltip;
import net.patchedpixel.scenecraft.gui.element.advanced.GuiProgressBar;
import net.patchedpixel.scenecraft.gui.function.Focusable;
import net.patchedpixel.scenecraft.gui.layout.CustomLayout;
import net.patchedpixel.scenecraft.gui.layout.HorizontalLayout;
import net.patchedpixel.scenecraft.gui.layout.VerticalLayout;
import net.patchedpixel.scenecraft.gui.utils.Colors;
import net.patchedpixel.scenecraft.gui.utils.geometry.Dimension;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.CrashReport;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.patchedpixel.scenecraft.core.versions.MCVer.getMinecraft;
import static net.patchedpixel.scenecraft.gui.utils.Utils.link;

public class GuiSavingReplay {
    private static final Minecraft mc = getMinecraft();
    private static final Logger logger = LogManager.getLogger();

    private final GuiLabel label = new GuiLabel()
            .setI18nText("scenecraft.gui.replaysaving.title")
            .setColor(Colors.BLACK);

    private final GuiProgressBar progressBar = new GuiProgressBar()
            .setHeight(14);

    private final GuiPanel panel = new GuiPanel()
            .setLayout(new VerticalLayout().setSpacing(2))
            .addElements(new VerticalLayout.Data(0.5), label, progressBar);

    private final SceneCraft core;
    private final List<Runnable> apply = new ArrayList<>();

    public GuiSavingReplay(SceneCraft core) {
        this.core = core;
    }

    public void open() {
        core.getBackgroundProcesses().addProcess(panel);
    }

    public void close() {
        core.getBackgroundProcesses().removeProcess(panel);
        AbstractGuiScreen<?> currentScreen = GuiScreen.from(mc.screen);
        if (currentScreen instanceof GuiReplayViewer) {
            ((GuiReplayViewer) currentScreen).list.load();
        }
    }

    public GuiProgressBar getProgressBar() {
        return progressBar;
    }

    public void presentRenameDialog(List<Pair<Path, ReplayMetaData>> outputPaths) {
        panel.removeElement(progressBar);

        link(outputPaths.stream().map(it -> addOutput(it.getKey(), it.getValue())).toArray(Focusable[]::new));

        GuiButton applyButton = new GuiButton()
                .setSize(150, 20)
                .setI18nLabel("scenecraft.gui.done")
                .onClick(this::apply);

        panel.addElements(new VerticalLayout.Data(0.5), applyButton);

        if (!core.getSettingsRegistry().get(Setting.RENAME_DIALOG)) {
            apply();
        }
    }

    private GuiTextField addOutput(Path path, ReplayMetaData metaData) {
        String originalName = Utils.fileNameToReplayName(path.getFileName().toString());
        GuiTextField textField = new GuiTextField()
                .setSize(130, 20)
                .setText(originalName)
                .setI18nHint("scenecraft.gui.delete")
                .setTextColorDisabled(Colors.RED)
                .onEnter(this::apply)
                .setTooltip(createTooltip(path, metaData));
        GuiButton clearButton = new GuiButton()
                .setSize(20, 20)
                .setLabel("X")
                .setTooltip(new GuiTooltip().setI18nText("scenecraft.gui.delete"))
                .onClick(() -> textField.setText(""));
        GuiPanel row = new GuiPanel()
                .setLayout(new HorizontalLayout())
                .addElements(null, textField, clearButton);
        panel.addElements(new VerticalLayout.Data(0.5), row);

        apply.add(() -> applyOutput(path, textField.getText()));

        return textField;
    }

    private GuiPanel createTooltip(Path path, ReplayMetaData metaData) {
        GuiTooltip tooltip = new GuiTooltip();
        GuiReplayViewer.GuiReplayEntry entry = new GuiReplayViewer.GuiReplayEntry(path.toFile(), metaData, null, new ArrayList<>());
        return new GuiPanel().setLayout(new CustomLayout<GuiPanel>() {
            @Override
            protected void layout(GuiPanel container, int width, int height) {
                pos(entry, 4, 4);
                size(entry, width - 8, height - 8);
                size(tooltip, width, height);
            }

            @Override
            public ReadableDimension calcMinSize(GuiContainer<?> container) {
                ReadableDimension size = entry.calcMinSize();
                return new Dimension(size.getWidth() + 8, size.getHeight() + 8);
            }
        }).addElements(null, tooltip, entry);
    }

    private void apply() {
        apply.forEach(Runnable::run);
        close();
    }

    private void applyOutput(Path path, String newName) {
        if (newName.isEmpty()) {
            try {
                Files.delete(path);
            } catch (IOException e) {
                logger.error("Deleting replay file:", e);
                CrashReport crashReport = CrashReport.forThrowable(e, "Deleting replay file");
                core.runLater(() -> Utils.error(logger, VanillaGuiScreen.wrap(mc.screen), crashReport, () -> {}));
            }
            return;
        }

        try {
            Path replaysFolder = core.folders.getReplayFolder();
            Path newPath = Utils.replayNameToPath(replaysFolder, newName);
            for (int i = 1; Files.exists(newPath); i++) {
                newPath = Utils.replayNameToPath(replaysFolder, newName + " (" + i + ")");
            }
            Files.move(path, newPath);
        } catch (IOException e) {
            logger.error("Renaming replay file:", e);
            CrashReport crashReport = CrashReport.forThrowable(e, "Renaming replay file");
            core.runLater(() -> Utils.error(logger, VanillaGuiScreen.wrap(mc.screen), crashReport, () -> {}));
        }
    }
}
