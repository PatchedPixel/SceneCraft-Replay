package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.NotNull;

public class ScrollbarWidget extends AbstractWidget {
    private static final int FG_X = 0, FG_Y = 0, FG_W = 62, FG_H = 7;
    private static final int BG_X = 0, BG_Y = 7, BG_W = 64, BG_H = 9;
    private static final int B = 1;

    private Runnable onValueChanged;
    private double zoom = 1;
    private double offset;
    private boolean dragging;
    private double lastDragX;

    public ScrollbarWidget(int width, int height) {
        super(0, 0, width, height, CommonComponents.EMPTY);
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int width = getWidth(), height = getHeight();
        NineSlice.draw(graphics, getX(), getY(), width, height, BG_X, BG_Y, BG_W, BG_H, B + 1, B, B + 1, B);
        int bodyWidth = width - 2 * B;
        int barOffset = (int) (bodyWidth * offset) + B;
        int barWidth = (int) (bodyWidth * zoom);
        NineSlice.draw(graphics, getX() + barOffset, getY() + B, barWidth, height - (B + 1) - B,
                FG_X, FG_Y, FG_W, FG_H, 2, 1, 1, 1);
    }

    private boolean overBar(double mouseX, double mouseY) {
        int bodyWidth = getWidth() - 2 * B;
        int barOffset = (int) (bodyWidth * offset) + B;
        int barWidth = (int) (bodyWidth * zoom);

        double mx = mouseX - getX(), my = mouseY - getY();
        return mx >= barOffset && my > B && mx <= barOffset + barWidth && my < getHeight() - B;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(active && visible && overBar(mouseX, mouseY)) {
            dragging = true;
            lastDragX = mouseX;
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if(dragging) {
            offset += (mouseX - lastDragX) / (getWidth() - 2 * B);
            lastDragX = mouseX;
            checkOffset();
            changed();
        }

        return dragging;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean was = dragging;
        dragging = false;

        return was;
    }

    private void changed() {
        if(onValueChanged != null) onValueChanged.run();
    }

    private void checkOffset() {
        if(offset < 0) {
            offset = 0;
        } else if(zoom + offset > 1) {
            offset = 1 - zoom;
        }
    }

    public ScrollbarWidget onValueChanged(Runnable r) { this.onValueChanged = r; return this; }
    public ScrollbarWidget setPosition(double pos) { offset = pos; checkOffset(); changed(); return this; }
    public double getPosition() { return offset; }
    public void setZoom(double z) { zoom = Math.min(1, Math.max(0.0001, z)); checkOffset(); changed(); }
    public double getZoom() { return zoom; }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}
}
