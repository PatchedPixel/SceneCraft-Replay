package net.patchedpixel.scenecraft.common.flamex.event.callbacks;

import net.patchedpixel.scenecraft.common.flamex.event.Event;

import net.minecraft.client.gui.GuiGraphics;

public interface PostRenderScreenCallback {
    Event<PostRenderScreenCallback> EVENT = Event.create((listeners) ->
            (stack, partialTicks) -> {
                for(PostRenderScreenCallback listener : listeners) {
                    listener.postRenderScreen(stack, partialTicks);
                }
            }
    );

    void postRenderScreen(GuiGraphics graphics, float partialTicks);
}
