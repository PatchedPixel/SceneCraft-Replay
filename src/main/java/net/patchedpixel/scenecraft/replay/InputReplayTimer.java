package net.patchedpixel.scenecraft.replay;

import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.replay.camera.CameraController;
import net.patchedpixel.scenecraft.replay.camera.CameraEntity;
import net.patchedpixel.scenecraft.gui.versions.ScreenExt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.LevelLoadingScreen;

public class InputReplayTimer {
    public static void updateInReplay() {
        SceneCraftReplay mod = SceneCraftReplay.instance;
        Minecraft mc = mod.getCore().getMinecraft();

        SceneCraft.instance.runTasks();

        // If we are in a replay, we have to manually process key and mouse events as the
        // tick speed may vary or there may not be any ticks at all (when the replay is paused)
        if (mod.getReplayHandler() != null && mc.level != null && mc.player != null) {
            if (mc.gui.screen() == null || ((ScreenExt) mc.gui.screen()).doesPassEvents()) {
                MCVer.pumpEvents();
                MCVer.processKeyBinds();
            }
            mc.keyboardHandler.tick();

            // As of 1.18.2, this screen always stays open for at least two seconds, and requires ticking to close.
            // Thanks, but we'll have none of that (at least while in a replay).
            if (mc.gui.screen() instanceof LevelLoadingScreen) {
                mc.gui.screen().onClose();
            }
            // The SplashOverlay now only closes on `tick`, but there are no ticks while the replay is paused.
            // so we need to manually tick it to not get stuck.
            Overlay overlay = mc.gui.overlay();
            if (overlay != null) {
                overlay.tick();
            }
        }
    }

    public static void handleScroll(int wheel) {
        if (wheel != 0) {
            ReplayHandler replayHandler = SceneCraftReplay.instance.getReplayHandler();
            if (replayHandler != null) {
                CameraEntity cameraEntity = replayHandler.getCameraEntity();
                if (cameraEntity != null) {
                    CameraController controller = cameraEntity.getCameraController();
                    while (wheel > 0) {
                        controller.increaseSpeed();
                        wheel--;
                    }
                    while (wheel < 0) {
                        controller.decreaseSpeed();
                        wheel++;
                    }
                }
            }
        }
    }
}
