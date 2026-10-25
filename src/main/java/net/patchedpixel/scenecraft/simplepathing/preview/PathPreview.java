package net.patchedpixel.scenecraft.simplepathing.preview;

import net.patchedpixel.scenecraft.core.KeyBindingRegistry;
import net.patchedpixel.scenecraft.core.SettingsRegistry;
import net.patchedpixel.scenecraft.core.events.SettingsChangedCallback;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import com.mojang.blaze3d.platform.InputConstants;
import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.patchedpixel.scenecraft.replay.events.ReplayClosedCallback;
import net.patchedpixel.scenecraft.replay.events.ReplayOpenedCallback;
import net.patchedpixel.scenecraft.simplepathing.SceneCraftSimplePathing;
import net.patchedpixel.scenecraft.simplepathing.Setting;

public class PathPreview extends EventRegistrations {
    private final SceneCraftSimplePathing mod;

    private ReplayHandler replayHandler;
    private PathPreviewRenderer renderer;

    public PathPreview(SceneCraftSimplePathing mod) {
        this.mod = mod;

        on(SettingsChangedCallback.EVENT, (registry, key) -> {
            if (key == Setting.PATH_PREVIEW) {
                update();
            }
        });

        on(ReplayOpenedCallback.EVENT, replayHandler -> {
            this.replayHandler = replayHandler;
            update();
        });

        on(ReplayClosedCallback.EVENT, replayHandler -> {
            this.replayHandler = null;
            update();
        });
    }

    public void registerKeyBindings(KeyBindingRegistry registry) {
        registry.registerKeyBinding("scenecraft.input.pathpreview", InputConstants.KEY_H, () -> {
            SettingsRegistry settings = mod.getCore().getSettingsRegistry();
            settings.set(Setting.PATH_PREVIEW, !settings.get(Setting.PATH_PREVIEW));
            settings.save();
        }, true);
    }

    private void update() {
        if (mod.getCore().getSettingsRegistry().get(Setting.PATH_PREVIEW) && replayHandler != null) {
            if (renderer == null) {
                renderer = new PathPreviewRenderer(mod, replayHandler);
                renderer.register();
            }
        } else {
            if (renderer != null) {
                renderer.unregister();
                renderer = null;
            }
        }
    }
}
