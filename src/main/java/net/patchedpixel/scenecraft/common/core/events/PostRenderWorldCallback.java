package net.patchedpixel.scenecraft.common.core.events;

import net.patchedpixel.scenecraft.common.gui.utils.Event;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeStorage;

public interface PostRenderWorldCallback {
    Event<PostRenderWorldCallback> EVENT = Event.create((listeners) ->
            (PoseStack matrixStack, SubmitNodeStorage submitNodeStorage) -> {
                for(PostRenderWorldCallback listener : listeners) {
                    listener.postRenderWorld(matrixStack, submitNodeStorage);
                }
            }
    );

    void postRenderWorld(PoseStack matrixStack, SubmitNodeStorage submitNodeStorage);
}
