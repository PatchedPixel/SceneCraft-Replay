package net.patchedpixel.scenecraft.common.gui.versions.callbacks;

import net.patchedpixel.scenecraft.common.gui.utils.Event;
import net.minecraft.client.gui.screens.Screen;

public interface OpenGuiScreenCallback {
    Event<OpenGuiScreenCallback> EVENT = Event.create((listeners) ->
            (screen) -> {
                for(OpenGuiScreenCallback listener : listeners) {
                    listener.openGuiScreen(screen);
                }
            }
    );

    void openGuiScreen(Screen screen);
}
