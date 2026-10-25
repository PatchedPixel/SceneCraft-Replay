package net.patchedpixel.scenecraft.core.events;

import net.patchedpixel.scenecraft.gui.utils.Event;
import com.mojang.blaze3d.vertex.PoseStack;

public interface PostRenderWorldCallback {
    Event<PostRenderWorldCallback> EVENT = Event.create((listeners) ->
            (PoseStack matrixStack) -> {
                for (PostRenderWorldCallback listener : listeners) {
                    listener.postRenderWorld(matrixStack);
                }
            }
    );

    void postRenderWorld(PoseStack matrixStack);
}
