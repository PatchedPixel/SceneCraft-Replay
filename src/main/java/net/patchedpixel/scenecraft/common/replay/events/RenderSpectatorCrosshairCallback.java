package net.patchedpixel.scenecraft.common.replay.events;

import net.patchedpixel.scenecraft.common.gui.utils.Event;

public interface RenderSpectatorCrosshairCallback {
    Event<RenderSpectatorCrosshairCallback> EVENT = Event.create((listeners) ->
            () -> {
                for(RenderSpectatorCrosshairCallback listener : listeners) {
                    Boolean state = listener.shouldRenderSpectatorCrosshair();
                    if(state != null) {
                        return state;
                    }
                }
                return null;
            }
    );

    Boolean shouldRenderSpectatorCrosshair();
}
