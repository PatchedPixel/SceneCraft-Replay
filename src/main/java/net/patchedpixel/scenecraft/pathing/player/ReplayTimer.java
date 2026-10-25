package net.patchedpixel.scenecraft.pathing.player;

import net.patchedpixel.scenecraft.gui.utils.Event;
import net.minecraft.client.Timer;

public class ReplayTimer extends Timer {
    public int ticksThisFrame;

    public ReplayTimer() {
        super(20.0F, 0L);
    }

    @Override
    public int advanceTime(long sysClock) {
        UpdatedCallback.EVENT.invoker().onUpdate();
        return ticksThisFrame;
    }

    public interface UpdatedCallback {
        Event<UpdatedCallback> EVENT = Event.create((listeners) ->
                () -> {
                    for (UpdatedCallback listener : listeners) {
                        listener.onUpdate();
                    }
                }
        );

        void onUpdate();
    }
}
