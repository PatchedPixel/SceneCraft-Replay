package net.patchedpixel.scenecraft.common.extras.playeroverview;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.FlameXTheme;
import net.patchedpixel.scenecraft.common.flamex.widget.CheckboxWidget;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlayerOverviewGui extends Screen {
    protected static final int ENTRY_WIDTH = 200;
    private static final int PANEL_WIDTH = ENTRY_WIDTH + 30;
    private static final int PANEL_TOP = 20;
    private static final int ROW_HEIGHT = 16;
    private static final int ROW_SPACING = 3;
    private static final int LIST_PADDING = 5;
    private static final int SLIDER_WIDTH = 6;
    private static final int SLIDER_GAP = 3;
    private static final int BOX_BACKGROUND = 0xFF2E2E2E;
    private static final int SPECTATOR_COLOR = 0xFF404040;

    private final PlayerOverview extra;
    private final List<Player> players = new ArrayList<>();

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

    private int panelLeft() {
        return width / 2 - PANEL_WIDTH / 2;
    }

    private int panelHeight() {
        return height - 40;
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int boxHeight = font.lineHeight + 2;
        int listY = PANEL_TOP + 10 + font.lineHeight + 5;
        int bottom = PANEL_TOP + panelHeight();

        PlayerList list = new PlayerList(left + 10, listY, PANEL_WIDTH - 15,
                bottom - 10 - boxHeight - 5 - listY);
        for(Player p : players) {
            list.add(p);
        }
        addRenderableWidget(list);

        CheckboxWidget save = (CheckboxWidget) FlameX.checkbox("scenecraft.gui.playeroverview.remembersettings",
                extra.isSavingEnabled(), extra::setSavingEnabled);
        save.setTooltip(Tooltip.create(Component.translatable("scenecraft.gui.playeroverview.remembersettings.description")));
        save.setPosition(left + 10, bottom - 10 - boxHeight);
        addRenderableWidget(save);

        CheckboxWidget uncheckAll = new BulkCheckbox(false, () -> setAll(false));
        uncheckAll.setTooltip(Tooltip.create(Component.translatable("scenecraft.gui.playeroverview.hideall")));
        uncheckAll.setPosition(left + PANEL_WIDTH - uncheckAll.getWidth() - 8, bottom - boxHeight - 10);
        addRenderableWidget(uncheckAll);

        CheckboxWidget checkAll = new BulkCheckbox(true, () -> setAll(true));
        checkAll.setTooltip(Tooltip.create(Component.translatable("scenecraft.gui.playeroverview.showall")));
        checkAll.setPosition(uncheckAll.getX() - 3 - checkAll.getWidth(), uncheckAll.getY());
        addRenderableWidget(checkAll);
    }

    private void setAll(boolean visible) {
        for(Player p : players) {
            extra.setHidden(p.getUUID(), !visible);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int left = panelLeft();
        graphics.fill(left, PANEL_TOP, left + PANEL_WIDTH, PANEL_TOP + panelHeight(), FlameXTheme.Overlay.DIM);
        graphics.drawString(font, title, width / 2 - font.width(title) / 2, 10, 0xFFFFFFFF, false);
        graphics.drawString(font, Component.translatable("scenecraft.gui.playeroverview.spectate"),
                left + 10, PANEL_TOP + 10, 0xFFFFFFFF, false);
        Component visible = Component.translatable("scenecraft.gui.playeroverview.visible");
        graphics.drawString(font, visible, left + PANEL_WIDTH - 10 - font.width(visible), PANEL_TOP + 10, 0xFFFFFFFF, false);
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

    private static class BulkCheckbox extends CheckboxWidget {
        private final Runnable action;

        BulkCheckbox(boolean selected, Runnable action) {
            super(Component.empty(), selected, v -> {});
            this.action = action;
        }

        @Override
        public void onPress() {
            action.run();
        }

        @Override
        public void playDownSound(@NotNull net.minecraft.client.sounds.SoundManager soundManager) {}
    }

    private class PlayerList extends AbstractWidget {
        private final List<Player> rows = new ArrayList<>();
        private int offset;
        private boolean draggingSlider;

        PlayerList(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
        }

        void add(Player p) {
            rows.add(p);
        }

        private int contentHeight() {
            int count = rows.size();
            return count * ROW_HEIGHT + Math.max(0, count - 1) * ROW_SPACING + LIST_PADDING * 2;
        }

        private int maxOffset() {
            return Math.max(0, contentHeight() - getHeight());
        }

        private int entryX() {
            return getX() + getWidth() / 2 - ENTRY_WIDTH / 2;
        }

        private int sliderX() {
            return getX() + getWidth() / 2 + ENTRY_WIDTH / 2 + SLIDER_GAP;
        }

        private int rowY(int index) {
            return getY() + LIST_PADDING + index * (ROW_HEIGHT + ROW_SPACING) - offset;
        }

        private boolean onSlider(double x, double y) {
            return maxOffset() > 0 && x >= sliderX() && x < sliderX() + SLIDER_WIDTH
                    && y >= getY() && y < getY() + getHeight();
        }

        private int boxSize() {
            return MCVer.getFontRenderer().lineHeight + 2;
        }

        private int boxX() {
            return entryX() + ENTRY_WIDTH - boxSize() - 2 - 5;
        }

        private int boxY(int index) {
            return rowY(index) + ROW_HEIGHT / 2 - boxSize() / 2;
        }

        @Override
        protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            Font font = MCVer.getFontRenderer();
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();
            offset = Math.max(0, Math.min(maxOffset(), offset));

            graphics.fill(x, y, x + w, y + h, 0x96000000);
            graphics.enableScissor(x, y, x + w, y + h);
            int ex = entryX();
            int box = boxSize();
            for(int i = 0; i < rows.size(); i++) {
                int ry = rowY(i);
                if(ry + ROW_HEIGHT < y || ry > y + h) continue;
                Player p = rows.get(i);
                ResourceLocation texture = ((AbstractClientPlayer) p).getSkinTextureLocation();
                graphics.blit(texture, ex + 5, ry, 16, 16, 8f, 8f, 8, 8, 64, 64);
                if(p.isModelPartShown(PlayerModelPart.HAT)) {
                    graphics.blit(texture, ex + 5, ry, 16, 16, 40f, 8f, 8, 8, 64, 64);
                }
                graphics.drawString(font, p.getName().getString(), ex + 5 + 16 + 2, ry + (ROW_HEIGHT - font.lineHeight) / 2,
                        isSpectator(p) ? SPECTATOR_COLOR : 0xFFFFFFFF, false);

                int bx = boxX();
                int by = boxY(i);
                graphics.fill(bx, by, bx + box, by + box, 0xFF000000);
                graphics.fill(bx + 1, by + 1, bx + box - 1, by + box - 1, BOX_BACKGROUND);
                if(!extra.isHidden(p.getUUID())) {
                    graphics.drawCenteredString(font, "x", bx + box / 2 + 1, by + 1, 0xFFE0E0E0);
                }
            }
            graphics.fillGradient(x, y, x + w, y + 4, 0xFF000000, 0x00000000);
            graphics.fillGradient(x, y + h - 4, x + w, y + h, 0x00000000, 0xFF000000);
            graphics.disableScissor();

            if(maxOffset() > 0) {
                int sx = sliderX();
                int barY = y + offset * h / contentHeight();
                int barHeight = h * h / contentHeight();

                graphics.fill(sx, y, sx + SLIDER_WIDTH, y + h, FlameXTheme.Slider.TRACK);
                graphics.fill(sx, barY, sx + SLIDER_WIDTH, barY + barHeight, FlameXTheme.Slider.THUMB);
                graphics.fill(sx + SLIDER_WIDTH - 1, barY, sx + SLIDER_WIDTH, barY + barHeight, FlameXTheme.Slider.SHADOW);
                graphics.fill(sx, barY + barHeight - 1, sx + SLIDER_WIDTH, barY + barHeight, FlameXTheme.Slider.SHADOW);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if(!active || !visible || button != 0) return false;
            if(onSlider(mouseX, mouseY)) {
                draggingSlider = true;

                return true;
            }
            if(!isMouseOver(mouseX, mouseY)) return false;
            Font font = MCVer.getFontRenderer();

            int ex = entryX();
            for(int i = 0; i < rows.size(); i++) {
                int ry = rowY(i);
                if(mouseY < ry || mouseY >= ry + ROW_HEIGHT) continue;
                Player p = rows.get(i);
                int bx = boxX();
                int by = boxY(i);
                if(mouseX >= bx && mouseX < bx + boxSize() && mouseY >= by && mouseY < by + boxSize()) {
                    MCVer.getMinecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    extra.setHidden(p.getUUID(), !extra.isHidden(p.getUUID()));

                    return true;
                }
                int nameEnd = ex + 5 + 16 + 2 + font.width(p.getName().getString());
                if(mouseX >= ex + 5 && mouseX < nameEnd) {
                    SceneCraftReplay.instance.getReplayHandler().spectateEntity(p);
                    return true;
                }

                return false;
            }

            return false;
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
            if(!draggingSlider) return false;
            offset = Math.max(0, Math.min(maxOffset(), offset + (int) Math.round(dy * contentHeight() / getHeight())));

            return true;
        }

        @Override
        public boolean mouseReleased(double mouseX, double mouseY, int button) {
            boolean was = draggingSlider;
            draggingSlider = false;

            return was;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
            if(!isMouseOver(mouseX, mouseY) || maxOffset() == 0) return false;
            offset = Math.max(0, Math.min(maxOffset(), offset - (int) Math.signum(delta) * 30));

            return true;
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}
    }
}
