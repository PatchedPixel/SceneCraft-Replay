package net.patchedpixel.scenecraft.common.core.events;

import com.mojang.blaze3d.platform.InputConstants;
import net.patchedpixel.scenecraft.common.gui.function.KeyInput;
import net.patchedpixel.scenecraft.common.gui.utils.Event;

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

    int ACTION_PRESS = InputConstants.PRESS;

    boolean onKeyEvent(KeyInput keyInput, int action);
}
