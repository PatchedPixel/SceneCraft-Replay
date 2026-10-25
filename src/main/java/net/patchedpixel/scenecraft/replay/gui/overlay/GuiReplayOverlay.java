package net.patchedpixel.scenecraft.replay.gui.overlay;

import net.patchedpixel.scenecraft.core.SceneCraft;
import net.patchedpixel.scenecraft.core.events.KeyBindingEventCallback;
import net.patchedpixel.scenecraft.core.events.KeyEventCallback;
import com.mojang.blaze3d.platform.InputConstants;
import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.replay.ReplaySender;
import net.patchedpixel.scenecraft.gui.GuiRenderer;
import net.patchedpixel.scenecraft.gui.RenderInfo;
import net.patchedpixel.scenecraft.gui.container.AbstractGuiOverlay;
import net.patchedpixel.scenecraft.gui.container.GuiPanel;
import net.patchedpixel.scenecraft.gui.element.GuiButton;
import net.patchedpixel.scenecraft.gui.element.GuiElement;
import net.patchedpixel.scenecraft.gui.element.GuiSlider;
import net.patchedpixel.scenecraft.gui.element.GuiTooltip;
import net.patchedpixel.scenecraft.gui.element.advanced.IGuiTimeline;
import net.patchedpixel.scenecraft.gui.function.KeyInput;
import net.patchedpixel.scenecraft.gui.layout.CustomLayout;
import net.patchedpixel.scenecraft.gui.layout.HorizontalLayout;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadablePoint;
import net.patchedpixel.scenecraft.gui.utils.geometry.WritablePoint;
import net.minecraft.client.Options;
import net.minecraft.client.resources.language.I18n;

import static net.patchedpixel.scenecraft.core.SceneCraft.TEXTURE_SIZE;

public class GuiReplayOverlay extends AbstractGuiOverlay<GuiReplayOverlay> {
    private final SceneCraftReplay mod = SceneCraftReplay.instance;

    public final GuiPanel topPanel = new GuiPanel(this)
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.LEFT).setSpacing(5));
    public final GuiButton playPauseButton = new GuiButton() {
        @Override
        public GuiElement getTooltip(RenderInfo renderInfo) {
            GuiTooltip tooltip = (GuiTooltip) super.getTooltip(renderInfo);
            if (tooltip != null) {
                String label;
                if (getSpriteUV().getY() == 0) { // Play button
                    label = "scenecraft.gui.ingame.menu.unpause";
                } else { // Pause button
                    label = "scenecraft.gui.ingame.menu.pause";
                }
                tooltip.setText(I18n.get(label) + " (" + mod.keyPlayPause.getBoundKey() + ")");
            }
            return tooltip;
        }
    }.setSize(20, 20).setTexture(SceneCraft.TEXTURE, TEXTURE_SIZE).setTooltip(new GuiTooltip());
    public final GuiSlider speedSlider = new GuiSlider().setSize(100, 20).setSteps(37); // 0.0 is not included
    public final GuiMarkerTimeline timeline;

    /**
     * This is not used by the replay module itself but may be used by other modules/extras to show
     * when they're active.
     */
    public final GuiPanel statusIndicatorPanel = new GuiPanel(this).setSize(100, 16)
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(5));

    private final EventHandler eventHandler = new EventHandler();
    private boolean hidden;

    public GuiReplayOverlay(final ReplayHandler replayHandler) {
        timeline = new GuiMarkerTimeline(replayHandler){
            @Override
            public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
                setCursorPosition(replayHandler.getReplaySender().currentTimeStamp());
                super.draw(renderer, size, renderInfo);
            }
        }.setSize(Integer.MAX_VALUE, 20);

        topPanel.addElements(null, playPauseButton, speedSlider, timeline);
        setLayout(new CustomLayout<GuiReplayOverlay>() {
            @Override
            protected void layout(GuiReplayOverlay container, int width, int height) {
                pos(topPanel, 10, 10);
                size(topPanel, width - 20, 20);

                pos(statusIndicatorPanel, width / 2, height - 21);
                width(statusIndicatorPanel, width / 2 - 5);
            }
        });

        playPauseButton.setSpriteUV(new ReadablePoint() {
            @Override
            public int getX() {
                return 0;
            }

            @Override
            public int getY() {
                return replayHandler.getReplaySender().paused() ? 0 : 20;
            }

            @Override
            public void getLocation(WritablePoint dest) {
                dest.setLocation(getX(), getY());
            }
        }).onClick(new Runnable() {
            @Override
            public void run() {
                ReplaySender replaySender = replayHandler.getReplaySender();
                // If currently paused
                if (replaySender.paused()) {
                    // then play
                    replaySender.setReplaySpeed(getSpeedSliderValue());
                } else {
                    // else pause
                    replaySender.setReplaySpeed(0);
                }
            }
        });

        speedSlider.onValueChanged(new Runnable() {
            @Override
            public void run() {
                double speed = getSpeedSliderValue();
                speedSlider.setText(I18n.get("scenecraft.gui.speed") + ": " + speed + "x");
                ReplaySender replaySender = replayHandler.getReplaySender();
                if (!replaySender.paused()) {
                    replaySender.setReplaySpeed(speed);
                }
            }
        }).setValue(9);

        timeline.onClick(new IGuiTimeline.OnClick() {
            @Override
            public void run(int time) {
                replayHandler.doJump(time, true);
            }
        }).setLength(replayHandler.getReplayDuration());
    }

    public double getSpeedSliderValue() {
        int value = speedSlider.getValue() + 1;
        if (value <= 9) {
            return value / 10d;
        } else {
            return 1 + (0.25d * (value - 10));
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (isVisible() != visible) {
            if (visible) {
                eventHandler.register();
            } else {
                eventHandler.unregister();
            }
        }
        super.setVisible(visible);
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        // Do not render overlay if all hud, or this one specifically, is hidden and we're not in some popup
        if ((getMinecraft().options.hideGui || hidden) && isAllowUserInput()) {
            // Note that this only applies to when the mouse is visible, otherwise
            // the draw method isn't called in the first place
            return;
        }
        super.draw(renderer, size, renderInfo);
    }

    @Override
    protected GuiReplayOverlay getThis() {
        return this;
    }

    private class EventHandler extends EventRegistrations {
        { on(KeyBindingEventCallback.EVENT, this::onKeyBindingEvent); }
        private void onKeyBindingEvent() {
            Options gameSettings = getMinecraft().options;
            while (gameSettings.keyChat.consumeClick() || gameSettings.keyCommand.consumeClick()) {
                if (!isMouseVisible()) {
                    setMouseVisible(true);
                }
            }
        }

        { on(KeyEventCallback.EVENT, this::onKeyInput); }
        private boolean onKeyInput(KeyInput keyInput, int action) {
            if (action != KeyEventCallback.ACTION_PRESS) return false;
            // Allow F1 to be used to hide the replay gui (e.g. for record with OBS)
            if (isMouseVisible() && keyInput.key == InputConstants.KEY_F1) {
                hidden = !hidden;
                return true;
            }
            return false;
        }
    }
}
