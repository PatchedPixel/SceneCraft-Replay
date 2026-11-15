package net.patchedpixel.scenecraft.common.core.events;

import net.patchedpixel.scenecraft.common.gui.utils.Event;

public interface PreRenderHandCallback {
    Event<PreRenderHandCallback> EVENT = Event.create((listeners) ->
            () -> {
                for(PreRenderHandCallback listener : listeners) {
                    if(listener.preRenderHand()) {
                        return true;
                    }
                }
                return false;
            }
    );

    boolean preRenderHand();
}
