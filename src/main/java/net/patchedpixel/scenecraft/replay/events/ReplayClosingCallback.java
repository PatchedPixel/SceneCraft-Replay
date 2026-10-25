package net.patchedpixel.scenecraft.replay.events;

import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.patchedpixel.scenecraft.gui.utils.Event;

import java.io.IOException;

public interface ReplayClosingCallback {
    Event<ReplayClosingCallback> EVENT = Event.create((listeners) ->
            (replayHandler) -> {
                for (ReplayClosingCallback listener : listeners) {
                    listener.replayClosing(replayHandler);
                }
            });

    void replayClosing(ReplayHandler replayHandler) throws IOException;
}
