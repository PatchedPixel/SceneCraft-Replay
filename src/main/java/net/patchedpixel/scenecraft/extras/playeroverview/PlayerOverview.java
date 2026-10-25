package net.patchedpixel.scenecraft.extras.playeroverview;

import com.mojang.blaze3d.platform.InputConstants;
import net.patchedpixel.scenecraft.core.SceneCraft;
import net.patchedpixel.scenecraft.core.events.PreRenderHandCallback;
import net.patchedpixel.scenecraft.core.utils.Utils;
import net.patchedpixel.scenecraft.extras.Extra;
import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.replay.camera.CameraEntity;
import net.patchedpixel.scenecraft.replay.events.ReplayClosedCallback;
import net.patchedpixel.scenecraft.replay.events.ReplayOpenedCallback;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.stream.Collectors;
import java.io.IOException;
import java.util.*;

public class PlayerOverview extends EventRegistrations implements Extra {
    private SceneCraftReplay module;

    private final Set<UUID> hiddenPlayers = new HashSet<>();
    private boolean savingEnabled;

    @Override
    public void register(final SceneCraft mod) throws Exception {
        this.module = SceneCraftReplay.instance;

        mod.getKeyBindingRegistry().registerKeyBinding("scenecraft.input.playeroverview", InputConstants.KEY_B, () -> {
            if (module.getReplayHandler() != null) {
                List<Player> players = mod.getMinecraft().level.players()
                        .stream()
                        .map(it -> (Player) it)
                        .filter(it -> !(it instanceof CameraEntity))
                        .collect(Collectors.toList());
                if (!Utils.isCtrlDown()) {
                    // Hide all players that have an UUID v2 (commonly used for NPCs)
                    Iterator<Player> iter = players.iterator();
                    while (iter.hasNext()) {
                        UUID uuid = iter.next().getGameProfile().getId();
                        if (uuid != null && uuid.version() == 2) {
                            iter.remove();
                        }
                    }
                }
                new PlayerOverviewGui(PlayerOverview.this, players).display();
            }
        }, true);

        register();
    }

    public boolean isHidden(UUID uuid) {
        return hiddenPlayers.contains(uuid);
    }

    public void setHidden(UUID uuid, boolean hidden) {
        if (hidden) {
            hiddenPlayers.add(uuid);
        } else {
            hiddenPlayers.remove(uuid);
        }
    }

    { on(ReplayOpenedCallback.EVENT, this::onReplayOpen); }
    private void onReplayOpen(ReplayHandler replayHandler) throws IOException {
        Optional<Set<UUID>> savedData = replayHandler.getReplayFile().getInvisiblePlayers();
        if (savedData.isPresent()) {
            hiddenPlayers.addAll(savedData.get());
            savingEnabled = true;
        } else {
            savingEnabled = false;
        }
    }

    { on(ReplayClosedCallback.EVENT, this::onReplayClose); }
    private void onReplayClose(ReplayHandler replayHandler) {
        hiddenPlayers.clear();
    }

    { on(PreRenderHandCallback.EVENT, this::shouldHideHand); }
    private boolean shouldHideHand() {
        Entity view = module.getCore().getMinecraft().getCameraEntity();
        return view != null && isHidden(view.getUUID());
    }

    public boolean isSavingEnabled() {
        return savingEnabled;
    }

    public void setSavingEnabled(boolean savingEnabled) {
        this.savingEnabled = savingEnabled;
    }

    public void saveHiddenPlayers() {
        if (savingEnabled) {
            try {
                module.getReplayHandler().getReplayFile().writeInvisiblePlayers(hiddenPlayers);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
