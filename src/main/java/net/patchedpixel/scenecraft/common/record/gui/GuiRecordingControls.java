package net.patchedpixel.scenecraft.common.record.gui;

import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.editor.gui.MarkerProcessor;
import net.patchedpixel.scenecraft.common.record.packet.PacketListener;
import net.patchedpixel.scenecraft.common.flamex.event.EventRegistrations;
import net.patchedpixel.scenecraft.common.flamex.event.callbacks.InitScreenCallback;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;

import net.minecraft.client.gui.components.AbstractWidget;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.core.versions.ScreenWidgets;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

public class GuiRecordingControls extends EventRegistrations {
    private SceneCraft core;
    private PacketListener packetListener;
    private boolean paused;
    private boolean stopped;

    private Button buttonPauseResume;
    private Button buttonStartStop;

    public GuiRecordingControls(SceneCraft core, PacketListener packetListener, boolean autoStart) {
        this.core = core;
        this.packetListener = packetListener;
        paused = stopped = !autoStart;
    }

    private void togglePause() {
        if(paused) {
            packetListener.addMarker(MarkerProcessor.MARKER_NAME_END_CUT);
        } else {
            packetListener.addMarker(MarkerProcessor.MARKER_NAME_START_CUT);
        }
        paused = !paused;
        updateState();
    }

    private void toggleStartStop() {
        if(stopped) {
            paused = false;
            packetListener.addMarker(MarkerProcessor.MARKER_NAME_END_CUT);
            core.printInfoToChat("scenecraft.chat.recordingstarted");
        } else {
            int timestamp = (int) packetListener.getCurrentDuration();
            if(!paused) {
                packetListener.addMarker(MarkerProcessor.MARKER_NAME_START_CUT, timestamp);
            }
            packetListener.addMarker(MarkerProcessor.MARKER_NAME_SPLIT, timestamp + 1);
        }
        stopped = !stopped;
        updateState();
    }

    private void updateState() {
        if(buttonPauseResume != null) {
            buttonPauseResume.setMessage(FlameX.t("scenecraft.gui.record." + (paused ? "resume" : "pause")));
            buttonPauseResume.active = !stopped;
        }
        if(buttonStartStop != null) {
            buttonStartStop.setMessage(FlameX.t("scenecraft.gui.record." + (stopped ? "start" : "stop")));
        }
    }

    { on(InitScreenCallback.EVENT, this::injectIntoIngameMenu); }
    private void injectIntoIngameMenu(Screen guiScreen, Collection<AbstractWidget> buttonList) {
        if(!(guiScreen instanceof PauseScreen)) {
            return;
        }
        if(buttonList.isEmpty()) {
            return; // menu-less pause (F3+Esc)
        }

        Function<Integer, Integer> yPos =
                MCVer.findButton(buttonList, "menu.returnToMenu", 1)
                        .map(Optional::of)
                        .orElse(MCVer.findButton(buttonList, "menu.disconnect", 1))
                        .<Function<Integer, Integer>>map(it -> (height) -> it.getY())
                        .orElse((height) -> height / 4 + 120 - 16);

        buttonPauseResume = FlameX.button(Component.empty(), 98, this::togglePause);
        buttonStartStop = FlameX.button(Component.empty(), 98, this::toggleStartStop);
        ScreenWidgets.getWidgets(guiScreen).add(buttonPauseResume);
        ScreenWidgets.getWidgets(guiScreen).add(buttonStartStop);
        updateState();

        int y = yPos.apply(guiScreen.height) + 16 + 8;
        buttonPauseResume.setPosition(guiScreen.width / 2 - 100, y);
        buttonStartStop.setPosition(guiScreen.width / 2 - 100 + 98 + 4, y);
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isStopped() {
        return stopped;
    }
}
