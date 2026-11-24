package net.patchedpixel.scenecraft.common.extras.playeroverview;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlayerOverviewGui extends Screen {
    protected static final int ENTRY_WIDTH = 200;

    private final PlayerOverview extra;
    private final List<Player> players = new ArrayList<>();
    private PlayerList list;

    public PlayerOverviewGui(final PlayerOverview extra, List<Player> players) {
        super(Component.translatable("scenecraft.input.playeroverview"));
        this.extra = extra;
        for(Player p : players) {
            if(p instanceof AbstractClientPlayer) this.players.add(p);
        }
        this.players.sort(new PlayerComparator());

        SceneCraftReplay.instance.getReplayHandler().getOverlay().setVisible(false);
    }

    public void display() {
        MCVer.getMinecraft().setScreen(this);
    }

    @Override
    protected void init() {
        int panelWidth = ENTRY_WIDTH + 30;
        int left = width / 2 - panelWidth / 2;

        list = new PlayerList(left, panelWidth);
        for(Player p : players) {
            list.add(new PlayerEntry(p));
        }
        list.updateSize(panelWidth, height - 20 - 40, 45, height - 40);
        list.setLeftPos(left);
        addRenderableWidget(list);

        Checkbox save = FlameX.checkbox("scenecraft.gui.playeroverview.remembersettings", extra.isSavingEnabled(),
                extra::setSavingEnabled);
        save.setTooltip(Tooltip.create(Component.translatable("scenecraft.gui.playeroverview.remembersettings.description")));
        save.setPosition(left + 10, height - 30);
        addRenderableWidget(save);

        net.minecraft.client.gui.components.Button showAll = FlameX.button(MCVer.literalText("\u2713"), 20, "scenecraft.gui.playeroverview.showall",
                () -> setAll(true));
        net.minecraft.client.gui.components.Button hideAll = FlameX.button(MCVer.literalText("\u2717"), 20, "scenecraft.gui.playeroverview.hideall",
                () -> setAll(false));
        hideAll.setPosition(left + panelWidth - 28, height - 33);
        showAll.setPosition(hideAll.getX() - 23, height - 33);
        addRenderableWidget(showAll);
        addRenderableWidget(hideAll);
    }

    private void setAll(boolean visible) {
        for(Player p : players) {
            extra.setHidden(p.getUUID(), !visible);
        }
        rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int panelWidth = ENTRY_WIDTH + 30;
        int left = width / 2 - panelWidth / 2;
        graphics.fill(left, 20, left + panelWidth, height - 20, 0xC0000000);
        graphics.drawString(font, Component.translatable("scenecraft.gui.playeroverview.spectate"), left + 10, 30, 0xFFFFFF, true);
        Component visible = Component.translatable("scenecraft.gui.playeroverview.visible");
        graphics.drawString(font, visible, left + panelWidth - 10 - font.width(visible), 30, 0xFFFFFF, true);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void removed() {
        SceneCraftReplay.instance.getReplayHandler().getOverlay().setVisible(true);
        extra.saveHiddenPlayers();
    }

    private static boolean isSpectator(Player e) {
        return e.isInvisible() && e.getEffect(MobEffects.INVISIBILITY) == null;
    }

    private static final class PlayerComparator implements Comparator<Player> {
        @Override
        public int compare(Player o1, Player o2) {
            if(isSpectator(o1) && !isSpectator(o2)) return 1;
            if(isSpectator(o2) && !isSpectator(o1)) return -1;
            return o1.getName().getString().compareToIgnoreCase(o2.getName().getString());
        }
    }

    private class PlayerList extends ContainerObjectSelectionList<PlayerEntry> {
        private final int rowWidth;

        PlayerList(int left, int rowWidth) {
            super(MCVer.getMinecraft(), rowWidth, 100, 45, 100, 20);
            this.rowWidth = rowWidth;
            setRenderBackground(false);
            setRenderTopAndBottom(false);
        }

        void add(PlayerEntry e) {
            addEntry(e);
        }

        @Override
        public int getRowWidth() {
            return ENTRY_WIDTH;
        }

        @Override
        protected int getScrollbarPosition() {
            return x0 + rowWidth - 6;
        }
    }

    private class PlayerEntry extends ContainerObjectSelectionList.Entry<PlayerEntry> {
        private final Player player;
        private final ResourceLocation texture;
        private final Checkbox checkbox;

        PlayerEntry(Player p) {
            this.player = p;
            this.texture = ((AbstractClientPlayer) p).getSkinTextureLocation();
            this.checkbox = new Checkbox(0, 0, 20, 20, Component.empty(), !extra.isHidden(p.getUUID())) {
                @Override
                public void onPress() {
                    super.onPress();
                    extra.setHidden(p.getUUID(), !selected());
                }
            };
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            graphics.blit(texture, left + 2, top + 2, 16, 16, 8f, 8f, 8, 8, 64, 64);
            if(player.isModelPartShown(PlayerModelPart.HAT)) {
                graphics.blit(texture, left + 2, top + 2, 16, 16, 40f, 8f, 8, 8, 64, 64);
            }
            graphics.drawString(MCVer.getFontRenderer(), player.getName().getString(), left + 22, top + 6,
                    isSpectator(player) ? 0xFFAAAAAA : 0xFFFFFFFF, true);
            checkbox.setPosition(left + width - 22, top);
            checkbox.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if(checkbox.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if(button == 0) {
                SceneCraftReplay.instance.getReplayHandler().spectateEntity(player);
                return true;
            }
            return false;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(checkbox);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(checkbox);
        }
    }
}
