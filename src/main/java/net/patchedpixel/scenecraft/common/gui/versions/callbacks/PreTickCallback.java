package net.patchedpixel.scenecraft.common.gui.versions.callbacks;

import net.patchedpixel.scenecraft.common.gui.utils.Event;

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
