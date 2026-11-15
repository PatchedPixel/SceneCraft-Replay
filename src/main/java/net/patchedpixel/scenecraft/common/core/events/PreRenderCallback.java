package net.patchedpixel.scenecraft.common.core.events;

import net.patchedpixel.scenecraft.common.gui.utils.Event;

public interface PreRenderCallback {
    Event<PreRenderCallback> EVENT = Event.create((listeners) ->
            () -> {
                for(PreRenderCallback listener : listeners) {
                    listener.preRender();
                }
            }
    );

    void preRender();
}
