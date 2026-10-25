package net.patchedpixel.scenecraft.extras;

import net.patchedpixel.scenecraft.core.SceneCraft;
import net.patchedpixel.scenecraft.core.events.PostRenderCallback;
import net.patchedpixel.scenecraft.core.events.PreRenderCallback;
import com.mojang.blaze3d.platform.InputConstants;
import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.replay.events.ReplayOpenedCallback;
import net.patchedpixel.scenecraft.replay.gui.overlay.GuiReplayOverlay;
import net.patchedpixel.scenecraft.gui.element.GuiImage;
import net.patchedpixel.scenecraft.gui.element.IGuiImage;
import net.patchedpixel.scenecraft.gui.layout.HorizontalLayout;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class FullBrightness extends EventRegistrations implements Extra {
    private SceneCraft core;
    private SceneCraftReplay module;

    private final IGuiImage indicator = new GuiImage().setTexture(SceneCraft.TEXTURE, 90, 20, 19, 16).setSize(19, 16);

    private Minecraft mc;
    private boolean active;

    private static boolean gamma;

    public static boolean getGamma() {
        return gamma;
    }

    @Override
    public void register(final SceneCraft mod) throws Exception {
        this.core = mod;
        this.module = SceneCraftReplay.instance;
        this.mc = mod.getMinecraft();

        mod.getKeyBindingRegistry().registerKeyBinding("scenecraft.input.lighting", InputConstants.KEY_Z, new Runnable() {
            @Override
            public void run() {
                active = !active;
                // need to tick once to update lightmap when replay is paused
                mod.getMinecraft().gameRenderer.tick();
                ReplayHandler replayHandler = module.getReplayHandler();
                if (replayHandler != null) {
                    updateIndicator(replayHandler.getOverlay());
                }
            }
        }, true);

        register();
    }

    public Type getType() {
        String str = core.getSettingsRegistry().get(Setting.FULL_BRIGHTNESS);
        for (Type type : Type.values()) {
            if (type.toString().equals(str)) {
                return type;
            }
        }
        return Type.Gamma;
    }

    { on(PreRenderCallback.EVENT, this::preRender); }
    private void preRender() {
        if (active && module.getReplayHandler() != null) {
            Type type = getType();
            if (type == Type.Gamma || type == Type.Both) {
                gamma = true;
            }
            if (type == Type.NightVision || type == Type.Both) {
                if (mc.player != null) {
                    mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION
                            , Integer.MAX_VALUE));
                }
            }
        }
    }

    { on(PostRenderCallback.EVENT, this::postRender); }
    private void postRender() {
        if (active && module.getReplayHandler() != null) {
            Type type = getType();
            if (type == Type.Gamma || type == Type.Both) {
                gamma = false;
            }
            if (type == Type.NightVision || type == Type.Both) {
                if (mc.player != null) {
                    mc.player.removeEffect(MobEffects.NIGHT_VISION
                    );
                }
            }
        }
    }

    { on(ReplayOpenedCallback.EVENT, replayHandler -> updateIndicator(replayHandler.getOverlay())); }
    private void updateIndicator(GuiReplayOverlay overlay) {
        if (active) {
            overlay.statusIndicatorPanel.addElements(new HorizontalLayout.Data(1), indicator);
        } else {
            overlay.statusIndicatorPanel.removeElement(indicator);
        }
    }

    enum Type {
        Gamma,
        NightVision,
        Both;

        @Override
        public String toString() {
            return "scenecraft.gui.settings.fullbrightness." + name().toLowerCase();
        }
    }
}
