package net.patchedpixel.scenecraft.replay;

import net.patchedpixel.scenecraft.core.SettingsRegistry;
import net.patchedpixel.scenecraft.replay.handler.GuiHandler.MainMenuButtonPosition;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class Setting<T> extends SettingsRegistry.SettingKeys<T> {
    public static final Setting<Boolean> SHOW_CHAT = make("showChat", "showchat", true);
    public static final Setting<Boolean> SHOW_SERVER_IPS = new Setting<>("showServerIPs", true);
    public static final SettingsRegistry.MultipleChoiceSettingKeys<String> CAMERA =
            new SettingsRegistry.MultipleChoiceSettingKeys<>(
                    "replay", "camera", "scenecraft.gui.settings.camera", "scenecraft.camera.classic");
    public static final Setting<Boolean> LEGACY_MAIN_MENU_BUTTON = new Setting<>("legacyMainMenuButton", false);
    public static final SettingsRegistry.MultipleChoiceSettingKeys<String> MAIN_MENU_BUTTON =
            new SettingsRegistry.MultipleChoiceSettingKeys<>(
                    "replay", "mainMenuButton", null, MainMenuButtonPosition.DEFAULT.name());
    static {
        MAIN_MENU_BUTTON.setChoices(Arrays.stream(MainMenuButtonPosition.values()).map(Enum::name).collect(Collectors.toList()));
    }

    private static <T> Setting<T> make(String key, String displayName, T defaultValue) {
        return new Setting<>(key, displayName, defaultValue);
    }

    public Setting(String key, String displayString, T defaultValue) {
        super("replay", key, "scenecraft.gui.settings." + displayString, defaultValue);
    }

    public Setting(String key, T defaultValue) {
        super("replay", key, null, defaultValue);
    }
}
