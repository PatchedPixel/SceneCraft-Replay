package net.patchedpixel.scenecraft.common.record.handler;

import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.core.SettingsRegistry;
import net.patchedpixel.scenecraft.common.core.gui.GuiReplayButton;
import net.patchedpixel.scenecraft.common.record.ServerInfoExt;
import net.patchedpixel.scenecraft.common.record.Setting;
import net.patchedpixel.scenecraft.common.gui.container.GuiScreen;
import net.patchedpixel.scenecraft.common.gui.container.VanillaGuiScreen;
import net.patchedpixel.scenecraft.common.gui.element.GuiButton;
import net.patchedpixel.scenecraft.common.gui.element.GuiCheckbox;
import net.patchedpixel.scenecraft.common.gui.element.GuiToggleButton;
import net.patchedpixel.scenecraft.common.gui.layout.CustomLayout;
import net.patchedpixel.scenecraft.common.gui.popup.GuiInfoPopup;
import net.patchedpixel.scenecraft.common.gui.utils.EventRegistrations;
import net.patchedpixel.scenecraft.common.gui.versions.callbacks.InitScreenCallback;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.language.I18n;

public class GuiHandler extends EventRegistrations {
    private final SceneCraft mod;

    public GuiHandler(SceneCraft mod) {
        this.mod = mod;
    }

    { on(InitScreenCallback.EVENT, (screen, buttons) -> onGuiInit(screen)); }
    private void onGuiInit(net.minecraft.client.gui.screens.Screen gui) {
        if(gui instanceof SelectWorldScreen || gui instanceof JoinMultiplayerScreen) {
            boolean sp = gui instanceof SelectWorldScreen;
            SettingsRegistry settingsRegistry = mod.getSettingsRegistry();
            Setting<Boolean> setting = sp ? Setting.RECORD_SINGLEPLAYER : Setting.RECORD_SERVER;

            GuiCheckbox recordingCheckbox = new GuiCheckbox()
                    .setI18nLabel("scenecraft.gui.settings.record" + (sp ? "singleplayer" : "server"))
                    .setChecked(settingsRegistry.get(setting));
            recordingCheckbox.onClick(() -> {
                settingsRegistry.set(setting, recordingCheckbox.isChecked());
                settingsRegistry.save();
            });

            VanillaGuiScreen vanillaGui = VanillaGuiScreen.wrap(gui);
            vanillaGui.setLayout(new CustomLayout<GuiScreen>(vanillaGui.getLayout()) {
                @Override
                protected void layout(GuiScreen container, int width, int height) {
                    //size(recordingCheckbox, 200, 20);
                    pos(recordingCheckbox, width - width(recordingCheckbox) - 5, 5);
                }
            }).addElements(null, recordingCheckbox);
        }

        if(gui instanceof ManageServerScreen) {
            VanillaGuiScreen vanillaGui = VanillaGuiScreen.wrap(gui);
            GuiButton replayButton = new GuiReplayButton().onClick(() -> {
                ServerData serverInfo = ((ManageServerScreen) gui).serverData;
                ServerInfoExt serverInfoExt = ServerInfoExt.from(serverInfo);
                Boolean state = serverInfoExt.getAutoRecording();
                GuiToggleButton<String> autoRecording = new GuiToggleButton<String>()
                        .setI18nLabel("scenecraft.gui.settings.autostartrecording")
                        .setValues(
                                I18n.get("scenecraft.gui.settings.default"),
                                I18n.get("options.off"),
                                I18n.get("options.on")
                        )
                        .setSelected(state == null ? 0 : state ? 2 : 1);
                autoRecording.onClick(() -> {
                    int selected = autoRecording.getSelected();
                    serverInfoExt.setAutoRecording(selected == 0 ? null : selected == 2);
                });
                GuiInfoPopup.open(vanillaGui, autoRecording);
            });
            vanillaGui.setLayout(new CustomLayout<GuiScreen>(vanillaGui.getLayout()) {
                @Override
                protected void layout(GuiScreen container, int width, int height) {
                    size(replayButton, 20, 20);
                    pos(replayButton, width - width(replayButton) - 5, 5);
                }
            }).addElements(null, replayButton);
        }
    }
}
