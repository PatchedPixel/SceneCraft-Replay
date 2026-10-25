package net.patchedpixel.scenecraft.core.events;

import net.patchedpixel.scenecraft.gui.utils.Event;

public interface PreRenderHandCallback {
    Event<PreRenderHandCallback> EVENT = Event.create((listeners) ->
            () -> {
                for (PreRenderHandCallback listener : listeners) {
                    if (listener.preRenderHand()) {
                        return true;
                    }
                }
                return false;
            }
    );

    boolean preRenderHand();
}
