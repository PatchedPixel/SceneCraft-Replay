package net.patchedpixel.scenecraft.editor.handler;

import net.patchedpixel.scenecraft.platform.Utils;
import net.patchedpixel.scenecraft.editor.SceneCraftEditor;
import net.patchedpixel.scenecraft.editor.gui.GuiEditReplay;
import net.patchedpixel.scenecraft.replay.gui.screen.GuiReplayViewer;
import net.patchedpixel.scenecraft.gui.container.AbstractGuiScreen;
import net.patchedpixel.scenecraft.gui.container.GuiScreen;
import net.patchedpixel.scenecraft.gui.element.GuiButton;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.patchedpixel.scenecraft.gui.versions.callbacks.InitScreenCallback;
import net.minecraft.CrashReport;

import java.io.IOException;

public class GuiHandler extends EventRegistrations {
    { on(InitScreenCallback.EVENT, (vanillaGuiScreen, buttonList) -> injectIntoReplayViewer(vanillaGuiScreen)); }
    public void injectIntoReplayViewer(net.minecraft.client.gui.screens.Screen vanillaGuiScreen) {
        AbstractGuiScreen guiScreen = GuiScreen.from(vanillaGuiScreen);
        if (!(guiScreen instanceof GuiReplayViewer)) {
            return;
        }
        final GuiReplayViewer replayViewer = (GuiReplayViewer) guiScreen;
        // Inject Edit button
        if (!replayViewer.editorButton.getChildren().isEmpty()) return;
        replayViewer.replaySpecificButtons.add(new GuiButton(replayViewer.editorButton).onClick(() -> {
            try {
                new GuiEditReplay(replayViewer, replayViewer.list.getSelected().get(0).file.toPath()) {
                    @Override
                    protected void close() {
                        super.close();
                        replayViewer.list.load();
                    }
                }.open();
            } catch (IOException e) {
                Utils.error(SceneCraftEditor.LOGGER, replayViewer, CrashReport.forThrowable(e, "Opening replay editor"), () -> {});
            }
        }).setSize(73, 20).setI18nLabel("scenecraft.gui.edit").setDisabled());
    }
}
