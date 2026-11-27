package net.patchedpixel.scenecraft.common.record.handler;

import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.core.SettingsRegistry;
import net.patchedpixel.scenecraft.common.record.ServerInfoExt;
import net.patchedpixel.scenecraft.common.record.Setting;
import net.patchedpixel.scenecraft.common.flamex.screen.FlameableScreen;
import net.patchedpixel.scenecraft.common.core.versions.ScreenWidgets;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.event.EventRegistrations;
import net.patchedpixel.scenecraft.common.flamex.event.callbacks.InitScreenCallback;
import net.patchedpixel.scenecraft.common.flamex.widget.LogoButton;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import java.util.Arrays;

public class GuiHandler extends EventRegistrations {
    private final SceneCraft mod;

    public GuiHandler(SceneCraft mod) {
        this.mod = mod;
    }

    { on(InitScreenCallback.EVENT, (screen, buttons) -> onGuiInit(screen)); }
    private void onGuiInit(Screen gui) {
        if(gui instanceof SelectWorldScreen || gui instanceof JoinMultiplayerScreen) {
            boolean sp = gui instanceof SelectWorldScreen;
            SettingsRegistry settingsRegistry = mod.getSettingsRegistry();
            Setting<Boolean> setting = sp ? Setting.RECORD_SINGLEPLAYER : Setting.RECORD_SERVER;

            Checkbox recordingCheckbox = FlameX.checkbox(
                    "scenecraft.gui.settings.record" + (sp ? "singleplayer" : "server"),
                    settingsRegistry.get(setting), checked -> {
                        settingsRegistry.set(setting, checked);
                        settingsRegistry.save();
                    });
            ScreenWidgets.getWidgets(gui).add(recordingCheckbox);
            recordingCheckbox.setPosition(gui.width - recordingCheckbox.getWidth() - 5, 5);
        }

        if(gui instanceof EditServerScreen) {
            LogoButton replayButton = new LogoButton(20, () -> {
                ServerData serverInfo = ((EditServerScreen) gui).serverData;
                MCVer.getMinecraft().setScreen(new AutoRecordingScreen(gui, ServerInfoExt.from(serverInfo)));
            });
            ScreenWidgets.getWidgets(gui).add(replayButton);
            replayButton.setPosition(gui.width - replayButton.getWidth() - 5, 5);
        }
    }

    private static class AutoRecordingScreen extends FlameableScreen {
        private final ServerInfoExt serverInfoExt;

        AutoRecordingScreen(Screen parent, ServerInfoExt serverInfoExt) {
            super(Component.empty(), parent, 10);
            this.serverInfoExt = serverInfoExt;
        }

        @Override
        protected void build(LinearLayout root) {
            Boolean state = serverInfoExt.getAutoRecording();
            // 0 = default, 1 = off, 2 = on
            CycleButton<Integer> autoRecording = FlameX.cycle(
                    FlameX.t("scenecraft.gui.settings.autostartrecording"),
                    Arrays.asList(0, 1, 2), state == null ? 0 : state ? 2 : 1,
                    v -> MCVer.literalText(I18n.get(v == 0 ? "scenecraft.gui.settings.default"
                            : v == 1 ? "options.off" : "options.on")),
                    250, selected -> serverInfoExt.setAutoRecording(selected == 0 ? null : selected == 2));
            root.addChild(autoRecording);
            root.addChild(FlameX.button("gui.back", 150, this::close));
        }
    }
}
