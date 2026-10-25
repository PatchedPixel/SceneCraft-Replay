package net.patchedpixel.scenecraft.core.events;

import net.patchedpixel.scenecraft.gui.utils.Event;

public interface PostRenderCallback {
    Event<PostRenderCallback> EVENT = Event.create((listeners) ->
            () -> {
                for (PostRenderCallback listener : listeners) {
                    listener.postRender();
                }
            }
    );

    void postRender();
}
