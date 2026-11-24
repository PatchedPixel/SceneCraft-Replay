package net.patchedpixel.scenecraft.common.flamex.event.callbacks;

import net.patchedpixel.scenecraft.common.flamex.event.Event;

import net.minecraft.client.gui.GuiGraphics;

public interface RenderHudCallback {
    Event<RenderHudCallback> EVENT = Event.create((listeners) ->
            (stack, partialTicks) -> {
                for(RenderHudCallback listener : listeners) {
                    listener.renderHud(stack, partialTicks);
                }
            }
    );

    void renderHud(GuiGraphics graphics, float partialTicks);
}
