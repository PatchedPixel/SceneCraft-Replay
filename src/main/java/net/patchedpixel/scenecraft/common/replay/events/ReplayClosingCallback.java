package net.patchedpixel.scenecraft.common.replay.events;

import net.patchedpixel.scenecraft.common.replay.ReplayHandler;
import net.patchedpixel.scenecraft.common.flamex.event.Event;

import java.io.IOException;

public interface ReplayClosingCallback {
    Event<ReplayClosingCallback> EVENT = Event.create((listeners) ->
            (replayHandler) -> {
                for(ReplayClosingCallback listener : listeners) {
                    listener.replayClosing(replayHandler);
                }
            });

    void replayClosing(ReplayHandler replayHandler) throws IOException;
}
