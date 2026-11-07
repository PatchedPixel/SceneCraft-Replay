package net.patchedpixel.scenecraft.common.core.events;

import net.patchedpixel.scenecraft.common.core.SettingsRegistry;
import net.patchedpixel.scenecraft.common.gui.utils.Event;

public interface SettingsChangedCallback {
    Event<SettingsChangedCallback> EVENT = Event.create((listeners) ->
            (registry, key) -> {
                for(SettingsChangedCallback listener : listeners) {
                    listener.onSettingsChanged(registry, key);
                }
            }
    );

    void onSettingsChanged(SettingsRegistry registry, SettingsRegistry.SettingKey<?> key);
}
