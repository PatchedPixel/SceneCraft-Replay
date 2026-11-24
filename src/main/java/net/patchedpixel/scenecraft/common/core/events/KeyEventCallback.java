package net.patchedpixel.scenecraft.common.core.events;

import net.patchedpixel.scenecraft.common.flamex.event.input.KeyInput;
import net.patchedpixel.scenecraft.common.flamex.event.Event;

public interface KeyEventCallback {
    Event<KeyEventCallback> EVENT = Event.create((listeners) ->
            (keyInput, action) -> {
                for(KeyEventCallback listener : listeners) {
                    if(listener.onKeyEvent(keyInput, action)) {
                        return true;
                    }
                }
                return false;
            }
    );

    int ACTION_PRESS = 1;

    boolean onKeyEvent(KeyInput keyInput, int action);
}
