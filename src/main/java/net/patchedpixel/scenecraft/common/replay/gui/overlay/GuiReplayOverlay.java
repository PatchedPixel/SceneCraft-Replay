package net.patchedpixel.scenecraft.common.replay.gui.overlay;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.core.events.KeyBindingEventCallback;
import net.patchedpixel.scenecraft.common.core.events.KeyEventCallback;
import net.patchedpixel.scenecraft.common.replay.ReplayHandler;
import net.patchedpixel.scenecraft.common.replay.ReplaySender;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.common.flamex.event.EventRegistrations;
import net.patchedpixel.scenecraft.common.flamex.event.callbacks.RenderHudCallback;
import net.patchedpixel.scenecraft.common.flamex.event.input.KeyInput;
import net.patchedpixel.scenecraft.common.flamex.util.ScreenExt;
import net.patchedpixel.scenecraft.common.flamex.widget.SpriteButton;
import net.patchedpixel.scenecraft.common.flamex.widget.StepSlider;
import net.patchedpixel.scenecraft.common.flamex.widget.WidgetRow;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static net.patchedpixel.scenecraft.common.SceneCraft.TEXTURE_SIZE;

public class GuiReplayOverlay {
    private final SceneCraftReplay mod = SceneCraftReplay.instance;
    private final Minecraft mc = MCVer.getMinecraft();

    public final WidgetRow topPanel = new WidgetRow(5);
    public final SpriteButton playPauseButton;
    public final StepSlider speedSlider = new StepSlider(100, 20, 37);
    public final GuiMarkerTimeline timeline;

    /**
     * This is not used by the replay module itself but may be used by other modules/extras to show
     * when they're active.
     */
    public final WidgetRow statusIndicatorPanel = new WidgetRow(5);

        private final List<AbstractWidget> extraWidgets = new ArrayList<>();
        private final List<BiConsumer<Integer, Integer>> layoutHooks = new ArrayList<>();

    private final EventHandler eventHandler = new EventHandler();
    private final OverlayScreen inputScreen = new OverlayScreen();
    private boolean visible;
    private boolean mouseVisible;
    private boolean closeable = true;
    private boolean hidden;

    public GuiReplayOverlay(final ReplayHandler replayHandler) {
        timeline = new GuiMarkerTimeline(replayHandler);
        timeline.setLength(replayHandler.getReplayDuration());
        timeline.onClick(time -> replayHandler.doJump(time, true));

        playPauseButton = new SpriteButton(20, 20, SceneCraft.TEXTURE, TEXTURE_SIZE, 20, () -> {
            ReplaySender replaySender = replayHandler.getReplaySender();
            if(replaySender.paused()) {
                replaySender.setReplaySpeed(getSpeedSliderValue());
            } else {
                replaySender.setReplaySpeed(0);
            }
        }).uv(() -> 0, () -> replayHandler.getReplaySender().paused() ? 0 : 20)
                .dynamicTooltip(() -> {
                    String label = replayHandler.getReplaySender().paused()
                            ? "scenecraft.gui.ingame.menu.unpause" : "scenecraft.gui.ingame.menu.pause";
                    return MCVer.literalText(I18n.get(label) + " (" + mod.keyPlayPause.getBoundKey() + ")");
                });

        speedSlider.setText(v -> MCVer.literalText(I18n.get("scenecraft.gui.speed") + ": " + speedFor(v) + "x"))
                .onValueChanged(() -> {
                    ReplaySender replaySender = replayHandler.getReplaySender();
                    if(!replaySender.paused()) {
                        replaySender.setReplaySpeed(getSpeedSliderValue());
                    }
                });
        speedSlider.setValue(9);

        topPanel.add(playPauseButton);
        topPanel.add(speedSlider);
        topPanel.add(timeline);
    }

    private static double speedFor(int sliderValue) {
        int value = sliderValue + 1;
        if(value <= 9) {
            return value / 10d;
        } else {
            return 1 + (0.25d * (value - 10));
        }
    }

    public double getSpeedSliderValue() {
        return speedFor(speedSlider.getValue());
    }

    public void addWidget(AbstractWidget widget) {
        if(!extraWidgets.contains(widget)) extraWidgets.add(widget);
        inputScreen.rebuildIfOpen();
    }

    public void removeWidget(AbstractWidget widget) {
        extraWidgets.remove(widget);
        inputScreen.rebuildIfOpen();
    }

    public void addLayoutHook(BiConsumer<Integer, Integer> hook) {
        layoutHooks.add(hook);
    }

    public void addStatusIndicator(AbstractWidget widget) {
        statusIndicatorPanel.add(widget);
        inputScreen.rebuildIfOpen();
    }

    public void removeStatusIndicator(AbstractWidget widget) {
        statusIndicatorPanel.remove(widget);
        inputScreen.rebuildIfOpen();
    }

    public Minecraft getMinecraft() {
        return mc;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        if(this.visible == visible) return;
        this.visible = visible;
        if(visible) {
            eventHandler.register();
        } else {
            eventHandler.unregister();
        }
        updateInputScreen();
    }

    public boolean isMouseVisible() {
        return mouseVisible;
    }

    public void setMouseVisible(boolean mouseVisible) {
        this.mouseVisible = mouseVisible;
        updateInputScreen();
    }

    public boolean isCloseable() {
        return closeable;
    }

    public void setCloseable(boolean closeable) {
        this.closeable = closeable;
    }

        public boolean isAllowUserInput() {
        return ((ScreenExt) inputScreen).doesPassEvents();
    }

    public void setAllowUserInput(boolean allowUserInput) {
        ((ScreenExt) inputScreen).setPassEvents(allowUserInput);
    }

    private void updateInputScreen() {
        if(!visible) return;
        if(mouseVisible) {
            if(mc.screen == null) {
                mc.setScreen(inputScreen);
            }
        } else if(mc.screen == inputScreen) {
            mc.setScreen(null);
        }
    }

    private List<AbstractWidget> allWidgets() {
        List<AbstractWidget> all = new ArrayList<>(topPanel.widgets());
        all.addAll(statusIndicatorPanel.widgets());
        all.addAll(extraWidgets);
        return all;
    }

    private void layout(int width, int height) {
        timeline.setWidth(Math.max(20, width - 20 - playPauseButton.getWidth() - speedSlider.getWidth() - 10 - 4));
        topPanel.arrange(10, 10);
        statusIndicatorPanel.arrange(0, 0);
        statusIndicatorPanel.arrange(width - 5 - statusIndicatorPanel.getWidth(), height - 21);
        for(BiConsumer<Integer, Integer> hook : layoutHooks) {
            hook.accept(width, height);
        }
    }

    private void renderOverlay(GuiGraphics graphics, float partialTicks) {
        updateInputScreen();
        if(isAllowUserInput() && (mc.options.hideGui || hidden) && mouseVisible) {
            return;
        }
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        layout(width, height);
        timeline.setCursorPosition(SceneCraftReplay.instance.getReplayHandler() != null
                ? SceneCraftReplay.instance.getReplayHandler().getReplaySender().currentTimeStamp()
                : timeline.getCursorPosition());
        int mouseX = -1, mouseY = -1;
        if(mouseVisible) {
            mouseX = (int) (mc.mouseHandler.xpos() * width / mc.getWindow().getScreenWidth());
            mouseY = (int) (mc.mouseHandler.ypos() * height / mc.getWindow().getScreenHeight());
        }
        renderAll(topPanel.widgets(), graphics, mouseX, mouseY, partialTicks);
        renderAll(statusIndicatorPanel.widgets(), graphics, mouseX, mouseY, partialTicks);
        renderAll(extraWidgets, graphics, mouseX, mouseY, partialTicks);
    }

    private static void renderAll(List<AbstractWidget> widgets, GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        for(int i = 0, n = widgets.size(); i < n; i++) {
            widgets.get(i).render(graphics, mouseX, mouseY, partialTicks);
        }
    }

        private class OverlayScreen extends Screen {
        OverlayScreen() {
            super(Component.empty());
            ((ScreenExt) this).setPassEvents(true);
        }

        void rebuildIfOpen() {
            if(mc.screen == this) {
                rebuildWidgets();
            }
        }

        @Override
        protected void init() {
            layout(this.width, this.height);
            for(AbstractWidget w : allWidgets()) {
                addWidget(w);
            }
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void onClose() {
            if(closeable) {
                super.onClose();
            }
        }

        @Override
        public void removed() {
            if(closeable) {
                mouseVisible = false;
            }
        }
    }

    private class EventHandler extends EventRegistrations {
        { on(RenderHudCallback.EVENT, GuiReplayOverlay.this::renderOverlay); }

        { on(KeyBindingEventCallback.EVENT, this::onKeyBindingEvent); }
        private void onKeyBindingEvent() {
            Options gameSettings = mc.options;
            while(gameSettings.keyChat.consumeClick() || gameSettings.keyCommand.consumeClick()) {
                if(!isMouseVisible()) {
                    setMouseVisible(true);
                }
            }
        }

        { on(KeyEventCallback.EVENT, this::onKeyInput); }
        private boolean onKeyInput(KeyInput keyInput, int action) {
            if(action != KeyEventCallback.ACTION_PRESS) return false;
            // Allow F1 to be used to hide the replay gui (e.graphics. for record with OBS)
            if(isMouseVisible() && keyInput.key == InputConstants.KEY_F1) {
                hidden = !hidden;
                return true;
            }
            return false;
        }
    }
}
