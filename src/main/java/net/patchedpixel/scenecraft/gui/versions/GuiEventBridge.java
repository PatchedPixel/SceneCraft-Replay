package net.patchedpixel.scenecraft.gui.versions;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.gui.function.CharInput;
import net.patchedpixel.scenecraft.gui.function.Click;
import net.patchedpixel.scenecraft.gui.function.KeyInput;
import net.patchedpixel.scenecraft.gui.versions.callbacks.InitScreenCallback;
import net.patchedpixel.scenecraft.gui.versions.callbacks.KeyboardCallback;
import net.patchedpixel.scenecraft.gui.versions.callbacks.MouseCallback;
import net.patchedpixel.scenecraft.gui.versions.callbacks.OpenGuiScreenCallback;
import net.patchedpixel.scenecraft.gui.versions.callbacks.PostRenderScreenCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.patchedpixel.scenecraft.core.events.PostRenderWorldCallback;
import net.patchedpixel.scenecraft.gui.versions.callbacks.RenderHudCallback;
import net.patchedpixel.scenecraft.gui.versions.callbacks.PreTickCallback;
import net.patchedpixel.scenecraft.replay.events.RenderHotbarCallback;
import net.patchedpixel.scenecraft.replay.InputReplayTimer;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.patchedpixel.scenecraft.render.hooks.EntityRendererHandler;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

public class GuiEventBridge {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        Click click = new Click(event.getMouseX(), event.getMouseY(), event.getButton(), MCVer.currentModifiers());
        if (MouseCallback.EVENT.invoker().mouseDown(click)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        Click click = new Click(event.getMouseX(), event.getMouseY(), event.getButton(), MCVer.currentModifiers());
        if (MouseCallback.EVENT.invoker().mouseUp(click)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouseDragged(ScreenEvent.MouseDragged.Pre event) {
        Click click = new Click(event.getMouseX(), event.getMouseY(), event.getMouseButton(), MCVer.currentModifiers());
        if (MouseCallback.EVENT.invoker().mouseDrag(click, event.getDragX(), event.getDragY())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (MouseCallback.EVENT.invoker().mouseScroll(event.getMouseX(), event.getMouseY(), 0, event.getScrollDelta())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        KeyInput keyInput = new KeyInput(event.getKeyCode(), event.getScanCode(), event.getModifiers());
        if (KeyboardCallback.EVENT.invoker().keyPressed(keyInput)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onKeyReleased(ScreenEvent.KeyReleased.Pre event) {
        KeyInput keyInput = new KeyInput(event.getKeyCode(), event.getScanCode(), event.getModifiers());
        if (KeyboardCallback.EVENT.invoker().keyReleased(keyInput)) {
            event.setCanceled(true);
            if (event.getScreen() instanceof ScreenExt ext && ext.doesPassEvents()) {
                KeyMapping.set(InputConstants.getKey(event.getKeyCode(), event.getScanCode()), false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onCharTyped(ScreenEvent.CharacterTyped.Pre event) {
        if (KeyboardCallback.EVENT.invoker().charTyped(new CharInput(event.getCodePoint(), event.getModifiers()))) {
            event.setCanceled(true);
        }
    }

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
        for (GuiEventListener listener : event.getListenersList()) {
            if (listener instanceof AbstractWidget widget) {
                widgets.add(widget);
            }
        }
        InitScreenCallback.EVENT.invoker().initScreen(event.getScreen(), widgets);
    }

    @SubscribeEvent
    public void onOpening(ScreenEvent.Opening event) {
        Screen screen = event.getNewScreen();
        OpenGuiScreenCallback.EVENT.invoker().openGuiScreen(screen);
    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            PostRenderWorldCallback.EVENT.invoker().postRenderWorld(event.getPoseStack());
        }
    }

    @SubscribeEvent
    public void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).sceneCraftRender_getHandler() != null) {
            return;
        }
        RenderHudCallback.EVENT.invoker().renderHud(event.getGuiGraphics(), event.getPartialTick());
    }

    @SubscribeEvent
    public void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) {
            Boolean state = RenderHotbarCallback.EVENT.invoker().shouldRenderHotbar();
            if (state == Boolean.FALSE) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            PreTickCallback.EVENT.invoker().preTick();
        }
    }

    @SubscribeEvent
    public void onMouseScrolling(InputEvent.MouseScrollingEvent event) {
        if (SceneCraftReplay.instance != null && SceneCraftReplay.instance.getReplayHandler() != null) {
            InputReplayTimer.handleScroll((int) (event.getScrollDelta() * 120));
            event.setCanceled(true);
        }
    }
}
