package net.patchedpixel.scenecraft.replay;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.patchedpixel.scenecraft.compat.SceneCraftCompat;
import net.patchedpixel.scenecraft.platform.KeyBindingRegistry;
import net.patchedpixel.scenecraft.core.Module;
import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.core.utils.ModCompat;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import com.mojang.blaze3d.platform.InputConstants;
import net.patchedpixel.scenecraft.replay.camera.CameraController;
import net.patchedpixel.scenecraft.replay.camera.CameraControllerRegistry;
import net.patchedpixel.scenecraft.replay.camera.CameraEntity;
import net.patchedpixel.scenecraft.replay.camera.ClassicCameraController;
import net.patchedpixel.scenecraft.replay.camera.VanillaCameraController;
import net.patchedpixel.scenecraft.replay.gui.screen.GuiModCompatWarning;
import net.patchedpixel.scenecraft.replay.handler.GuiHandler;
import net.patchedpixel.scenecraft.scenestudio.data.Marker;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayFile;
import net.patchedpixel.scenecraft.gui.function.Click;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class SceneCraftReplay implements Module {
    { instance = this; }
    public static SceneCraftReplay instance;

    private SceneCraft core;
    public KeyBindingRegistry.Binding keyPlayPause;

    private final CameraControllerRegistry cameraControllerRegistry = new CameraControllerRegistry();

    public static Logger LOGGER = LogManager.getLogger();

    private ReplayHandler replayHandler;

    public ReplayHandler getReplayHandler() {
        return replayHandler;
    }

    public static long pendingBorderReplayTime = -1L;

    public SceneCraftReplay(SceneCraft core) {
        this.core = core;

        core.getSettingsRegistry().register(Setting.class);
    }

    @Override
    public void registerKeyBindings(KeyBindingRegistry registry) {
        registry.registerKeyBinding("scenecraft.input.marker", InputConstants.KEY_M, () -> {
            if (replayHandler != null ) {
                CameraEntity camera = replayHandler.getCameraEntity();
                if (camera != null) {
                    Marker marker = new Marker();
                    marker.setTime(replayHandler.getReplaySender().currentTimeStamp());
                    marker.setX(camera.getX());
                    marker.setY(camera.getY());
                    marker.setZ(camera.getZ());
                    marker.setYaw(camera.getYRot());
                    marker.setPitch(camera.getXRot());
                    marker.setRoll(camera.roll);
                    replayHandler.getOverlay().timeline.addMarker(marker);
                }
            }
        }, true);

        registry.registerKeyBinding("scenecraft.input.thumbnail", InputConstants.KEY_N, () -> {
            if (replayHandler != null) {
                Minecraft mc = MCVer.getMinecraft();
                ListenableFuture<NoGuiScreenshot> future = NoGuiScreenshot.take(mc, 1280, 720);
                Futures.addCallback(future, new FutureCallback<>() {
                    @Override
                    public void onSuccess(NoGuiScreenshot result) {
                        try {
                            core.printInfoToChat("scenecraft.chat.savingthumb");
                            ByteArrayOutputStream thumbOut = new ByteArrayOutputStream();
                            result.getImage().writePNG(thumbOut);
                            replayHandler.getReplayFile().writeThumbBytes(thumbOut.toByteArray());
                            core.printInfoToChat("scenecraft.chat.savedthumb");
                        } catch (IOException e) {
                            e.printStackTrace();
                            core.printWarningToChat("scenecraft.chat.failedthumb");
                        } finally {
                            result.getImage().close();
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        t.printStackTrace();
                        core.printWarningToChat("scenecraft.chat.failedthumb");
                    }
                }, Runnable::run);
            }
        }, true);

        keyPlayPause = registry.registerKeyBinding("scenecraft.input.playpause", InputConstants.KEY_P, () -> {
            if (replayHandler != null) {
                replayHandler.getOverlay().playPauseButton.onClick(new Click(-1, -1, 0, 0));
            }
        }, true);

        core.getKeyBindingRegistry().registerKeyBinding("scenecraft.input.rollclockwise", InputConstants.KEY_L, () -> {
            // Noop, actual handling logic in CameraEntity#update
        }, true);

        core.getKeyBindingRegistry().registerKeyBinding("scenecraft.input.rollcounterclockwise", InputConstants.KEY_J, () -> {
            // Noop, actual handling logic in CameraEntity#update
        }, true);

        core.getKeyBindingRegistry().registerKeyBinding("scenecraft.input.resettilt", InputConstants.KEY_K, () -> {
            Optional.ofNullable(replayHandler).map(ReplayHandler::getCameraEntity).ifPresent(c -> c.roll = 0);
        }, true);
    }

    @Override
    public void initClient() {
        cameraControllerRegistry.register("scenecraft.camera.classic", new Function<>() {
            @Nullable
            @Override
            public CameraController apply(CameraEntity cameraEntity) {
                return new ClassicCameraController(cameraEntity);
            }
        });
        cameraControllerRegistry.register("scenecraft.camera.vanilla", new Function<>() {
            @Nullable
            @Override
            public CameraController apply(@Nullable CameraEntity cameraEntity) {
                return new VanillaCameraController(core.getMinecraft(), cameraEntity);
            }
        });

        new GuiHandler(this).register();
    }

    public void startReplay(File file) throws IOException {
        startReplay(core.files.open(file.toPath()));
    }

    public void startReplay(ReplayFile replayFile) throws IOException {
        startReplay(replayFile, true, true);
    }

    public ReplayHandler startReplay(ReplayFile replayFile, boolean checkModCompat, boolean asyncMode) throws IOException {
        if (replayHandler != null) {
            SceneCraftCompat.showModHud();
            replayHandler.endReplay();
        }
        if (checkModCompat) {
            ModCompat.ModInfoDifference modDifference = new ModCompat.ModInfoDifference(replayFile.getModInfo());
            if (!modDifference.getMissing().isEmpty() || !modDifference.getDiffering().isEmpty()) {
                GuiModCompatWarning screen = new GuiModCompatWarning(modDifference);
                screen.loadButton.onClick(() -> {
                    try {
                        startReplay(replayFile, false, asyncMode);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                screen.display();
                return null;
            }
        }
        replayHandler = new ReplayHandler(replayFile, asyncMode);
        SceneCraftCompat.hideModHud();
        KeyMapping.resetMapping(); // see Mixin_ContextualKeyBindings

        return replayHandler;
    }

    public void forcefullyStopReplay() {
        replayHandler = null;
        SceneCraftCompat.showModHud();
        KeyMapping.resetMapping(); // see Mixin_ContextualKeyBindings
    }

    public SceneCraft getCore() {
        return core;
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public CameraController createCameraController(CameraEntity cameraEntity) {
        String controllerName = core.getSettingsRegistry().get(Setting.CAMERA);
        return cameraControllerRegistry.create(controllerName, cameraEntity);
    }
}
