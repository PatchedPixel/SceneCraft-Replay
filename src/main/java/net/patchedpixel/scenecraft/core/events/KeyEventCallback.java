package net.patchedpixel.scenecraft.core.events;

import net.patchedpixel.scenecraft.gui.function.KeyInput;
import net.patchedpixel.scenecraft.gui.utils.Event;

public interface KeyEventCallback {
    Event<KeyEventCallback> EVENT = Event.create((listeners) ->
            (keyInput, action) -> {
                for (KeyEventCallback listener : listeners) {
                    if (listener.onKeyEvent(keyInput, action)) {
                        return true;
                    }
                }
                return false;
            }
    );

    int ACTION_PRESS = 1;

    boolean onKeyEvent(KeyInput keyInput, int action);
}
