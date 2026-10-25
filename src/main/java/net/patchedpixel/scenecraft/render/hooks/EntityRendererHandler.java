package net.patchedpixel.scenecraft.render.hooks;

import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.core.events.PreRenderHandCallback;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.render.RenderSettings;
import net.patchedpixel.scenecraft.render.Setting;
import net.patchedpixel.scenecraft.render.capturer.CaptureData;
import net.patchedpixel.scenecraft.render.capturer.RenderInfo;
import net.patchedpixel.scenecraft.render.capturer.WorldRenderer;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import net.patchedpixel.scenecraft.core.events.PostRenderCallback;
import net.patchedpixel.scenecraft.core.events.PreRenderCallback;
import net.minecraft.util.Util;

import java.io.IOException;

public class EntityRendererHandler extends EventRegistrations implements WorldRenderer {
    public final Minecraft mc = MCVer.getMinecraft();

    protected final RenderSettings settings;

    private final RenderInfo renderInfo;

    public CaptureData data;

    public boolean omnidirectional;

    private final long startTime;

    private long fakeFinishTimeNano;

    private boolean warmedUp;

    public EntityRendererHandler(RenderSettings settings, RenderInfo renderInfo) {
        this.settings = settings;
        this.renderInfo = renderInfo;

        this.startTime = Util.getNanos();

        on(PreRenderHandCallback.EVENT, () -> omnidirectional);

        ((IEntityRenderer) mc.gameRenderer).sceneCraftRender_setHandler(this);
        register();
    }

    @Override
    public void renderWorld(CaptureData data) {
        this.data = data;
        long offsetMillis;
        if (SceneCraft.instance.getSettingsRegistry().get(Setting.FRAME_TIME_FROM_WORLD_TIME)) {
            offsetMillis = SceneCraftReplay.instance.getReplayHandler().getReplaySender().currentTimeStamp();
        } else {
            offsetMillis = renderInfo.getFramesDone() * 1_000L / settings.getFramesPerSecond();
        }
        long frameStartTimeNano = startTime + offsetMillis * 1_000_000L;
        renderWorld(frameStartTimeNano);
    }

    public void renderWorld(long finishTimeNano) {
        fakeFinishTimeNano = finishTimeNano;
        PreRenderCallback.EVENT.invoker().preRender();

        if (mc.level != null && mc.player != null) {
            Screen orgScreen = mc.gui.screen();
            boolean orgPauseOnLostFocus = mc.options.pauseOnLostFocus;
            boolean orgHudIsHidden = mc.gui.hud.isHidden();
            try {
                mc.gui.screen = null;
                mc.options.pauseOnLostFocus = false; // do not want the pause menu to open if the window is unfocused
                if (omnidirectional) {
                    // makes no sense, we wouldn't even know where to put it
                    mc.gameRenderer.mainCamera().enablePanoramicMode();
                }
                if (!orgHudIsHidden) mc.gui.hud.toggle();

                mc.gameRenderer.update(mc.getDeltaTracker());
                if(!warmedUp) {
                    warmedUp = true;
                    mc.gameRenderer.extract(mc.getDeltaTracker(), true);
                    mc.gameRenderer.render();
                }
                mc.gameRenderer.extract(mc.getDeltaTracker(), true);
                mc.gameRenderer.render();
            } finally {
                mc.gui.screen = orgScreen;
                mc.options.pauseOnLostFocus = orgPauseOnLostFocus;
                mc.gameRenderer.mainCamera().disablePanoramicMode();
                if (!orgHudIsHidden) mc.gui.hud.toggle();
            }
        }

        PostRenderCallback.EVENT.invoker().postRender();
    }

    @Override
    public void close() throws IOException {
        ((IEntityRenderer) mc.gameRenderer).sceneCraftRender_setHandler(null);
        unregister();
    }

    @Override
    public void setOmnidirectional(boolean omnidirectional) {
        this.omnidirectional = omnidirectional;
    }

    public RenderSettings getSettings() {
        return this.settings;
    }
    public long getFakeFinishTimeNano() {
        return fakeFinishTimeNano;
    }

    public interface IEntityRenderer {
        void sceneCraftRender_setHandler(EntityRendererHandler handler);
        EntityRendererHandler sceneCraftRender_getHandler();
    }
}
