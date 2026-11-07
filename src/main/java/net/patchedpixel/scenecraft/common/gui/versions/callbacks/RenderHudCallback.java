package net.patchedpixel.scenecraft.common.gui.versions.callbacks;

import net.patchedpixel.scenecraft.common.gui.utils.Event;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface RenderHudCallback {
    Event<RenderHudCallback> EVENT = Event.create((listeners) ->
            (stack, partialTicks) -> {
                for(RenderHudCallback listener : listeners) {
                    listener.renderHud(stack, partialTicks);
                }
            }
    );

    void renderHud(GuiGraphicsExtractor context, float partialTicks);
}
