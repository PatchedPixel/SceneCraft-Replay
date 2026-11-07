package net.patchedpixel.scenecraft.common.pathing.player;

import net.patchedpixel.scenecraft.common.gui.utils.Event;
import net.minecraft.client.DeltaTracker;

/**
 * A timer that does not advance by itself.
 */
public class ReplayTimer extends DeltaTracker.Timer {
    public int ticksThisFrame;

    public ReplayTimer() {
        super(0, 0, f -> f);
    }

    @Override
    // This should be handled by Remap but it isn't (was handled before a9724e3).
    public int advanceGameTime(long sysClock) {
        UpdatedCallback.EVENT.invoker().onUpdate();
        return ticksThisFrame;
    }

    public float tickDelta;

    @Override
    public float getGameTimeDeltaPartialTick(boolean bl) {
        return tickDelta;
    }

    public interface UpdatedCallback {
        Event<UpdatedCallback> EVENT = Event.create((listeners) ->
                () -> {
                    for(UpdatedCallback listener : listeners) {
                        listener.onUpdate();
                    }
                }
        );
        void onUpdate();
    }
}
