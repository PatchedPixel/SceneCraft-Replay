package net.patchedpixel.scenecraft.common.core.events;

import net.patchedpixel.scenecraft.common.flamex.event.Event;

public interface PostRenderCallback {
    Event<PostRenderCallback> EVENT = Event.create((listeners) ->
            () -> {
                for(PostRenderCallback listener : listeners) {
                    listener.postRender();
                }
            }
    );

    void postRender();
}
