package net.patchedpixel.scenecraft.common.core.events;

import net.patchedpixel.scenecraft.common.flamex.event.Event;

public interface KeyBindingEventCallback {
    Event<KeyBindingEventCallback> EVENT = Event.create((listeners) ->
            () -> {
                for(KeyBindingEventCallback listener : listeners) {
                    listener.onKeybindingEvent();
                }
            }
    );

    void onKeybindingEvent();
}
