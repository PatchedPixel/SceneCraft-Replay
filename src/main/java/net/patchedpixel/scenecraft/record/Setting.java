package net.patchedpixel.scenecraft.record;

import net.patchedpixel.scenecraft.core.SettingsRegistry;

public final class Setting<T> extends SettingsRegistry.SettingKeys<T> {
    public static final Setting<Boolean> RECORD_SINGLEPLAYER = make("recordSingleplayer", "recordsingleplayer", true);
    public static final Setting<Boolean> RECORD_SERVER = make("recordServer", "recordserver", true);
    public static final Setting<Boolean> INDICATOR = make("indicator", "indicator", true);
    public static final Setting<Boolean> AUTO_START_RECORDING = make("autoStartRecording", "autostartrecording", true);
    public static final Setting<Boolean> AUTO_POST_PROCESS = make("autoPostProcess", null, true);
    public static final Setting<Boolean> RENAME_DIALOG = make("renameDialog", "rename_recording_dialog", true);

    private static <T> Setting<T> make(String key, String displayName, T defaultValue) {
        return new Setting<>(key, displayName, defaultValue);
    }

    public Setting(String key, String displayString, T defaultValue) {
        super("record", key, displayString == null ? null : "scenecraft.gui.settings." + displayString, defaultValue);
    }
}
