package net.patchedpixel.scenecraft.gui.versions.callbacks;

import net.patchedpixel.scenecraft.gui.utils.Event;
import net.minecraft.client.gui.screens.Screen;

public interface OpenGuiScreenCallback {
    Event<OpenGuiScreenCallback> EVENT = Event.create((listeners) ->
            (screen) -> {
                for (OpenGuiScreenCallback listener : listeners) {
                    listener.openGuiScreen(screen);
                }
            }
    );

    void openGuiScreen(Screen screen);
}
