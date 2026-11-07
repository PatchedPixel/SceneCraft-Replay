package net.patchedpixel.scenecraft.common.gui.versions.callbacks;

import net.patchedpixel.scenecraft.common.gui.utils.Event;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface PostRenderScreenCallback {
    Event<PostRenderScreenCallback> EVENT = Event.create((listeners) ->
            (stack, partialTicks) -> {
                for(PostRenderScreenCallback listener : listeners) {
                    listener.postRenderScreen(stack, partialTicks);
                }
            }
    );

    void postRenderScreen(GuiGraphicsExtractor context, float partialTicks);
}
