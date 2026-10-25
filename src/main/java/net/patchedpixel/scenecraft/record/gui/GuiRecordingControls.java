package net.patchedpixel.scenecraft.record.gui;

import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.editor.gui.MarkerProcessor;
import net.patchedpixel.scenecraft.record.packet.PacketListener;
import net.patchedpixel.scenecraft.gui.container.GuiPanel;
import net.patchedpixel.scenecraft.gui.container.VanillaGuiScreen;
import net.patchedpixel.scenecraft.gui.element.GuiButton;
import net.patchedpixel.scenecraft.gui.layout.CustomLayout;
import net.patchedpixel.scenecraft.gui.layout.HorizontalLayout;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.patchedpixel.scenecraft.gui.versions.callbacks.InitScreenCallback;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;

import net.minecraft.client.gui.components.AbstractWidget;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public class GuiRecordingControls extends EventRegistrations {
    private SceneCraft core;
    private PacketListener packetListener;
    private boolean paused;
    private boolean stopped;

    private GuiPanel panel = new GuiPanel().setLayout(new HorizontalLayout().setSpacing(4));

    private GuiButton buttonPauseResume = new GuiButton(panel).onClick(() -> {
        if (paused) {
            packetListener.addMarker(MarkerProcessor.MARKER_NAME_END_CUT);
        } else {
            packetListener.addMarker(MarkerProcessor.MARKER_NAME_START_CUT);
        }
        paused = !paused;
        updateState();
    }).setSize(98, 20);

    private GuiButton buttonStartStop = new GuiButton(panel).onClick(() -> {
        if (stopped) {
            paused = false;
            packetListener.addMarker(MarkerProcessor.MARKER_NAME_END_CUT);
            core.printInfoToChat("scenecraft.chat.recordingstarted");
        } else {
            int timestamp = (int) packetListener.getCurrentDuration();
            if (!paused) {
                packetListener.addMarker(MarkerProcessor.MARKER_NAME_START_CUT, timestamp);
            }
            packetListener.addMarker(MarkerProcessor.MARKER_NAME_SPLIT, timestamp + 1);
        }
        stopped = !stopped;
        updateState();
    }).setSize(98, 20);

    public GuiRecordingControls(SceneCraft core, PacketListener packetListener, boolean autoStart) {
        this.core = core;
        this.packetListener = packetListener;

        paused = stopped = !autoStart;

        updateState();
    }

    private void updateState() {
        buttonPauseResume.setI18nLabel("scenecraft.gui.record." + (paused ? "resume" : "pause"));
        buttonStartStop.setI18nLabel("scenecraft.gui.record." + (stopped ? "start" : "stop"));

        buttonPauseResume.setEnabled(!stopped);
    }

    { on(InitScreenCallback.EVENT, this::injectIntoIngameMenu); }
    private void injectIntoIngameMenu(Screen guiScreen,
                                      Collection<AbstractWidget> buttonList
    ) {
        if (!(guiScreen instanceof PauseScreen)) {
            return;
        }
        if (buttonList.isEmpty()) {
            return; // menu-less pause (F3+Esc)
        }
        Function<Integer, Integer> yPos =
                MCVer.findButton(buttonList, "menu.returnToMenu", 1)
                        .map(Optional::of)
                        .orElse(MCVer.findButton(buttonList, "menu.disconnect", 1))
                        .<Function<Integer, Integer>>map(it -> (height) -> it.getY())
                        .orElse((height) -> height / 4 + 120 - 16);
        VanillaGuiScreen vanillaGui = VanillaGuiScreen.wrap(guiScreen);
        vanillaGui.setLayout(new CustomLayout<net.patchedpixel.scenecraft.gui.container.GuiScreen>(vanillaGui.getLayout()) {
            @Override
            protected void layout(net.patchedpixel.scenecraft.gui.container.GuiScreen container, int width, int height) {
                pos(panel, width / 2 - 100, yPos.apply(height) + 16 + 8);
            }
        }).addElements(null, panel);
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isStopped() {
        return stopped;
    }
}
