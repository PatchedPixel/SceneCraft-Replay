package net.patchedpixel.scenecraft.core.events;

import net.patchedpixel.scenecraft.gui.utils.Event;

public interface PreRenderCallback {
    Event<PreRenderCallback> EVENT = Event.create((listeners) ->
            () -> {
                for (PreRenderCallback listener : listeners) {
                    listener.preRender();
                }
            }
    );

    void preRender();
}
