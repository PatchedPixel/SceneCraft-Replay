package net.patchedpixel.scenecraft.common.flamex.event.callbacks;

import net.patchedpixel.scenecraft.common.flamex.event.Event;

public interface PreTickCallback {
    Event<PreTickCallback> EVENT = Event.create((listeners) ->
            () -> {
                for(PreTickCallback listener : listeners) {
                    listener.preTick();
                }
            }
    );

    void preTick();
}
