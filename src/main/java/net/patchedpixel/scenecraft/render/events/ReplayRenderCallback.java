package net.patchedpixel.scenecraft.render.events;

import net.patchedpixel.scenecraft.render.rendering.VideoRenderer;
import net.patchedpixel.scenecraft.gui.utils.Event;

public interface ReplayRenderCallback {
    interface Pre {
        Event<Pre> EVENT = Event.create((listeners) ->
                (renderer) -> {
                    for (Pre listener : listeners) {
                        listener.beforeRendering(renderer);
                    }
                });

        void beforeRendering(VideoRenderer renderer);
    }

    interface Post {
        Event<Post> EVENT = Event.create((listeners) ->
                (renderer) -> {
                    for (Post listener : listeners) {
                        listener.afterRendering(renderer);
                    }
                });

        void afterRendering(VideoRenderer renderer);
    }
}
