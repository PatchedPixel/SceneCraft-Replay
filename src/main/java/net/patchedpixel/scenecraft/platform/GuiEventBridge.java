package net.patchedpixel.scenecraft.platform;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.level.GameType;
import net.minecraft.client.gui.GuiGraphics;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.flamex.event.callbacks.InitScreenCallback;
import net.patchedpixel.scenecraft.common.flamex.event.callbacks.PostRenderScreenCallback;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.patchedpixel.scenecraft.common.core.events.PostRenderWorldCallback;
import net.patchedpixel.scenecraft.common.flamex.event.callbacks.RenderHudCallback;
import net.patchedpixel.scenecraft.common.flamex.event.callbacks.PreTickCallback;
import net.patchedpixel.scenecraft.common.replay.events.RenderHotbarCallback;
import net.patchedpixel.scenecraft.common.replay.InputReplayTimer;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.patchedpixel.scenecraft.common.render.hooks.EntityRendererHandler;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class GuiEventBridge {
    @SubscribeEvent
    public void onRenderPost(ScreenEvent.Render.Post event) {
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        try {
            PostRenderScreenCallback.EVENT.invoker().postRenderScreen(graphics, event.getPartialTick());
        } finally {
            graphics.pose().popPose();
        }
    }

    @SubscribeEvent
    public void onInitPre(ScreenEvent.Init.Pre event) {
        InitScreenCallback.Pre.EVENT.invoker().preInitScreen(event.getScreen());
    }

    @SubscribeEvent
    public void onInitPost(ScreenEvent.Init.Post event) {
        List<AbstractWidget> widgets = new ArrayList<>();
        for(GuiEventListener listener : event.getListenersList()) {
            if(listener instanceof AbstractWidget widget) {
                widgets.add(widget);
            }
        }
        InitScreenCallback.EVENT.invoker().initScreen(event.getScreen(), widgets);
    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if(event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            PostRenderWorldCallback.EVENT.invoker().postRenderWorld(event.getPoseStack());
        }
    }

    @SubscribeEvent
    public void onRenderGuiPost(RenderGuiEvent.Post event) {
        if(((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).sceneCraftRender_getHandler() != null) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 400);
        try {
            RenderHudCallback.EVENT.invoker().renderHud(graphics, event.getPartialTick());
        } finally {
            graphics.pose().popPose();
        }
    }

    @SubscribeEvent
    public void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if(event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            MultiPlayerGameMode gameMode = MCVer.getMinecraft().gameMode;
            if(gameMode != null && gameMode.getPlayerMode() == GameType.SPECTATOR) {
                return;
            }
            Boolean state = RenderHotbarCallback.EVENT.invoker().shouldRenderHotbar();
            if(state == Boolean.FALSE) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if(event.phase == TickEvent.Phase.START) {
            PreTickCallback.EVENT.invoker().preTick();
        }
    }

    @SubscribeEvent
    public void onMouseScrolling(InputEvent.MouseScrollingEvent event) {
        if(SceneCraftReplay.instance != null && SceneCraftReplay.instance.getReplayHandler() != null) {
            InputReplayTimer.handleScroll((int) (event.getScrollDelta() * 120));
            event.setCanceled(true);
        }
    }
}
