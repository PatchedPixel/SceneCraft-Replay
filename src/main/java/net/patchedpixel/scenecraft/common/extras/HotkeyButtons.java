package net.patchedpixel.scenecraft.common.extras;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.replay.events.ReplayOpenedCallback;
import net.patchedpixel.scenecraft.common.replay.gui.overlay.GuiReplayOverlay;
import net.patchedpixel.scenecraft.common.flamex.event.EventRegistrations;
import net.patchedpixel.scenecraft.common.flamex.widget.SpriteButton;
import net.patchedpixel.scenecraft.platform.KeyBindingRegistry;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HotkeyButtons extends EventRegistrations implements Extra {
    private SceneCraft mod;

    @Override
    public void register(SceneCraft mod) {
        this.mod = mod;
        register();
    }

    { on(ReplayOpenedCallback.EVENT, replayHandler -> new Gui(mod, replayHandler.getOverlay())); }

    private static final class KeyButton extends Button {
        private final KeyBindingRegistry.Binding keyBinding;

        KeyButton(KeyBindingRegistry.Binding keyBinding) {
            super(0, 0, 20, 20, Component.empty(), b -> {
                if(keyBinding.supportsAutoActivation() && Screen.hasControlDown()) {
                    keyBinding.setAutoActivating(!keyBinding.isAutoActivating());
                } else {
                    keyBinding.trigger();
                }
            }, DEFAULT_NARRATION);
            this.keyBinding = keyBinding;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // There doesn't seem to be an KeyBindingUpdate event, so we'll just update it every time
            setMessage(MCVer.literalText(keyBinding.isBound() ? keyBinding.getBoundKey() : ""));
            if(keyBinding.supportsAutoActivation()) {
                setTooltip(Tooltip.create(MCVer.literalText(
                        I18n.get("scenecraft.gui.ingame.autoactivating") + "\n"
                                + I18n.get("scenecraft.gui.ingame.autoactivating."
                                + (keyBinding.isAutoActivating() ? "disable" : "enable")))));
                setFGColor(keyBinding.isAutoActivating() ? 0x00ff00 : 0xe0e0e0);
            }
            int textWidth = MCVer.getFontRenderer().width(getMessage());
            setWidth(Math.max(10 /* consistent min width */, textWidth) + 10 /* padding */);
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
        }
    }

    public static final class Gui {
        private static final int CELL_WIDTH = 150;
        private static final int SPACING = 5;

        private final GuiReplayOverlay overlay;
        private final SpriteButton toggleButton;
        private final List<AbstractWidget[]> cells = new ArrayList<>();
        private boolean open;

        public Gui(SceneCraft mod, GuiReplayOverlay overlay) {
            this.overlay = overlay;
            toggleButton = new SpriteButton(20, 20, SceneCraft.TEXTURE, SceneCraft.TEXTURE_SIZE, 20, this::toggle)
                    .uv(0, 120);
            overlay.addWidget(toggleButton);

            final KeyBindingRegistry keyBindingRegistry = mod.getKeyBindingRegistry();
            keyBindingRegistry.getBindings().values().stream()
                    .sorted(Comparator.comparing(it -> I18n.get(it.name)))
                    .forEachOrdered(keyBinding -> {
                        KeyButton button = new KeyButton(keyBinding);
                        StringWidget label = new StringWidget(Component.translatable(keyBinding.name),
                                MCVer.getFontRenderer());
                        label.alignLeft();
                        cells.add(new AbstractWidget[]{button, label});
                    });

            overlay.addLayoutHook(this::layout);
        }

        private void toggle() {
            open = !open;
            for(AbstractWidget[] cell : cells) {
                for(AbstractWidget w : cell) {
                    if(open) overlay.addWidget(w);
                    else overlay.removeWidget(w);
                }
            }
        }

        private void layout(int width, int height) {
            toggleButton.setPosition(5, height - 25);
            if(!open) return;
            int columns = Math.max(1, (width - 10) / (CELL_WIDTH + SPACING));
            int rows = (cells.size() + columns - 1) / columns;
            int panelHeight = rows * 20 + Math.max(0, rows - 1) * SPACING;
            int top = toggleButton.getY() - 5 - panelHeight;
            for(int i = 0; i < cells.size(); i++) {
                int col = i % columns;
                int row = i / columns;
                int x = 5 + col * (CELL_WIDTH + SPACING);
                int y = top + row * (20 + SPACING);
                AbstractWidget button = cells.get(i)[0];
                AbstractWidget label = cells.get(i)[1];
                button.setPosition(x, y);
                label.setPosition(x + button.getWidth() + 4, y + 6);
                label.setWidth(Math.max(10, CELL_WIDTH - button.getWidth() - 4));
            }
        }
    }
}
