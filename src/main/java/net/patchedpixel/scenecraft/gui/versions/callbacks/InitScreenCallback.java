package net.patchedpixel.scenecraft.gui.versions.callbacks;

import net.patchedpixel.scenecraft.gui.utils.Event;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.AbstractWidget;

import java.util.Collection;

public interface InitScreenCallback {
    Event<InitScreenCallback> EVENT = Event.create((listeners) ->
            (screen, buttons) -> {
                for (InitScreenCallback listener : listeners) {
                    listener.initScreen(screen, buttons);
                }
            }
    );

    void initScreen(Screen screen, Collection<AbstractWidget> buttons);

    interface Pre {
        Event<Pre> EVENT = Event.create((listeners) ->
                (screen) -> {
                    for (Pre listener : listeners) {
                        listener.preInitScreen(screen);
                    }
                }
        );

        void preInitScreen(Screen screen);
    }
}
