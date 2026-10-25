package net.patchedpixel.scenecraft.replay.events;

import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.patchedpixel.scenecraft.gui.utils.Event;

public interface ReplayClosedCallback {
    Event<ReplayClosedCallback> EVENT = Event.create((listeners) ->
            (replayHandler) -> {
                for (ReplayClosedCallback listener : listeners) {
                    listener.replayClosed(replayHandler);
                }
            });

    void replayClosed(ReplayHandler replayHandler);
}
