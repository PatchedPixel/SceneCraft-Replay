package net.patchedpixel.scenecraft.common.extras;

import net.patchedpixel.scenecraft.common.SceneCraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.patchedpixel.scenecraft.common.replay.ReplayHandler;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.common.replay.events.ReplayOpenedCallback;
import net.patchedpixel.scenecraft.common.replay.gui.overlay.GuiReplayOverlay;
import net.patchedpixel.scenecraft.common.flamex.widget.SpriteWidget;
import net.patchedpixel.scenecraft.common.flamex.event.EventRegistrations;

public class QuickMode extends EventRegistrations implements Extra {
    private SceneCraftReplay module;

    private final SpriteWidget indicator = new SpriteWidget(SceneCraft.TEXTURE, 40, 100, 16, 16, SceneCraft.TEXTURE_SIZE, SceneCraft.TEXTURE_SIZE);

    @Override
    public void register(final SceneCraft mod) {
        this.module = SceneCraftReplay.instance;

        mod.getKeyBindingRegistry().registerKeyBinding("scenecraft.input.quickmode", InputConstants.KEY_Q, () -> {
            ReplayHandler replayHandler = module.getReplayHandler();
            if(replayHandler == null) {
                return;
            }
            replayHandler.getReplaySender().setSyncModeAndWait();
            mod.runLaterWithoutLock(() -> {
                replayHandler.ensureQuickModeInitialized(() -> {
                    boolean enabled = !replayHandler.isQuickMode();
                    updateIndicator(replayHandler.getOverlay(), enabled);
                    replayHandler.setQuickMode(enabled);
                    replayHandler.getReplaySender().setAsyncMode(true);
                });
            });
        }, true);

        register();
    }

    {
        on(ReplayOpenedCallback.EVENT, replayHandler -> updateIndicator(replayHandler.getOverlay(), replayHandler.isQuickMode()));
    }

    private void updateIndicator(GuiReplayOverlay overlay, boolean enabled) {
        if(enabled) {
            overlay.addStatusIndicator(indicator);
        } else {
            overlay.removeStatusIndicator(indicator);
        }
    }
}
