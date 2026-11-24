package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.patchedpixel.scenecraft.common.flamex.FlameXTheme;
import org.jetbrains.annotations.NotNull;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

public class TimelineWidget extends AbstractWidget {
    public interface OnClick {
        void run(int time);
    }

    protected static final int TEXTURE_WIDTH = 64;
    protected static final int TEXTURE_HEIGHT = 22;
    protected static final int TEXTURE_X = 0;
    protected static final int TEXTURE_Y = 16;
    public static final int BORDER_LEFT = 4;
    public static final int BORDER_RIGHT = 4;
    public static final int BORDER_TOP = 4;
    public static final int BORDER_BOTTOM = 3;
    protected static final int MARKER_MIN_DISTANCE = 40;

    private OnClick onClick;
    private int length;
    private int cursorPosition;
    private double zoom = 1;
    private int offset;
    private final boolean drawCursor = true;
    private boolean drawMarkers;
    private String lastTooltipText;

    public TimelineWidget(int width, int height) {
        super(0, 0, width, height, CommonComponents.EMPTY);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int width = getWidth();
        int height = getHeight();

        NineSlice.draw(graphics, getX(), getY(), width, height, TEXTURE_X, TEXTURE_Y, TEXTURE_WIDTH, TEXTURE_HEIGHT,
                BORDER_TOP + 1, BORDER_BOTTOM, BORDER_LEFT + 1, BORDER_RIGHT);
        if(drawMarkers) {
            drawTimeMarkers(graphics);
        }

        drawContent(graphics, mouseX, mouseY);
        drawTimelineCursor(graphics);
        updateTooltip(mouseX, mouseY);
    }

    protected void drawContent(GuiGraphics graphics, int mouseX, int mouseY) {}

    protected void drawTimelineCursor(GuiGraphics graphics) {
        if(!drawCursor || length <= 0) {
            return;
        }

        int visibleLength = visibleLength();
        int cursor = Math.max(offset, Math.min(offset + visibleLength, cursorPosition));
        int cursorX = getX() + xOffsetFor(cursor);

        graphics.blit(FlameXTheme.GUI_TEXTURE, cursorX - 2, getY() + BORDER_TOP - 1, 64, 0, 5, 4);
        for(int y = BORDER_TOP - 1; y < getHeight() - BORDER_BOTTOM; y += 11) {
            int segH = Math.min(11, getHeight() - BORDER_BOTTOM - y);
            graphics.blit(FlameXTheme.GUI_TEXTURE, cursorX - 2, getY() + y, 64, 4, 5, segH);
        }
    }

    protected void drawTimeMarkers(GuiGraphics graphics) {
        int visibleLength = visibleLength();
        int interval = getMarkerInterval();
        int small = Math.max(interval / 5, 1);
        int time = offset / interval * interval;

        while(time <= offset + visibleLength) {
            if(time >= offset) {
                boolean big = time % interval == 0;
                int x = getX() + xOffsetFor(time);
                int h = getHeight() / (big ? 3 : 6);
                int color = big ? 0xFFC0C0C0 : 0xFFFFFFFF;
                graphics.fill(x, getY() + getHeight() - BORDER_BOTTOM - h, x + 1, getY() + getHeight() - BORDER_BOTTOM, color);
            }

            time += small;
        }
    }

    protected int visibleLength() {
        return Math.max(1, (int) (length * zoom));
    }

    public int xOffsetFor(int time) {
        double fraction = (double) (time - offset) / visibleLength();
        return (int) (BORDER_LEFT + fraction * (getWidth() - BORDER_LEFT - BORDER_RIGHT));
    }

    public int getTimeAt(double mouseX, double mouseY) {
        double mx = mouseX - getX();
        double my = mouseY - getY();

        if(mx < 0 || my < 0 || mx > getWidth() || my > getHeight()) {
            return -1;
        }

        int bodyWidth = getWidth() - BORDER_LEFT - BORDER_RIGHT;
        double segmentLength = length * zoom;
        double segmentTime = segmentLength * (mx - BORDER_LEFT) / bodyWidth;

        return Math.min(Math.max((int) Math.round(offset + segmentTime), 0), length);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!active || !visible || !isMouseOver(mouseX, mouseY)) return false;
        int time = getTimeAt(mouseX, mouseY);
        if(time != -1 && onClick != null) {
            onClick.run(time);
            return true;
        }

        return false;
    }

    protected String getTooltipText(int mouseX, int mouseY) {
        int ms = getTimeAt(mouseX, mouseY);
        return String.format("%02d:%02d", ms / 1000 / 60, ms / 1000 % 60);
    }

    private void updateTooltip(int mouseX, int mouseY) {
        if(!isHovered) return;
        String text = getTooltipText(mouseX, mouseY);
        if(!text.equals(lastTooltipText)) {
            lastTooltipText = text;
            setTooltip(Tooltip.create(MCVer.literalText(text)));
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}

    public void onClick(OnClick onClick) {
        this.onClick = onClick;
    }

    public TimelineWidget setLength(int length) {
        this.length = length;
        return this;
    }

    public int getLength() {
        return length;
    }

    public TimelineWidget setCursorPosition(int position) {
        this.cursorPosition = Math.min(Math.max(position, 0), length);
        return this;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public TimelineWidget ensureCursorVisible() {
        return ensureCursorVisible(0);
    }

    public void ensureCursorVisibleWithPadding() {
        ensureCursorVisible(Math.max((int) (length * zoom) / 10, 10));
    }

    public TimelineWidget ensureCursorVisible(int padding) {
        int visible = (int) (length * zoom);
        if(cursorPosition - padding < offset) {
            setOffset(cursorPosition - padding);
        } else if(cursorPosition + padding > offset + visible) {
            setOffset(cursorPosition + padding - visible);
        }

        return this;
    }

    public void setZoom(double zoom) {
        this.zoom = Math.min(zoom, 1);
        checkOffset();
    }

    public double getZoom() {
        return zoom;
    }

    public TimelineWidget setOffset(int offset) {
        this.offset = Math.max(offset, 0);
        checkOffset();
        return this;
    }

    public int getOffset() {
        return offset;
    }

    public TimelineWidget setMarkers(boolean active) {
        this.drawMarkers = active;
        return this;
    }

    public TimelineWidget setMarkers() {
        return setMarkers(true);
    }

    public boolean getMarkers() {
        return drawMarkers;
    }

    public int getMarkerInterval() {
        int width = getWidth() - BORDER_LEFT - BORDER_RIGHT;
        if(width <= 0 || length <= 0) {
            return Math.max(length, 1);
        }

        double segmentLength = length * zoom;
        int minDistance = (int) (MARKER_MIN_DISTANCE * (length > 60 * 60 * 1000 ? 1.2 : 1));
        int maxMarkers = Math.max(1, width / minDistance);
        int minInterval = (int) (segmentLength / maxMarkers);

        final int S = 1000, M = 60 * S, H = 60 * M;
        final int[] snapTo = {S, 2 * S, 5 * S, 10 * S, 15 * S, 20 * S, 30 * S, M, 2 * M, 5 * M, 10 * M, 15 * M, 30 * M, H, 2 * H, 5 * H, 10 * H};
        for(int snap : snapTo) {
            if(snap > minInterval) return snap;
        }

        return snapTo[snapTo.length - 1];
    }

    private void checkOffset() {
        int visible = (int) (length * zoom);
        if(visible + offset > length) {
            offset = Math.max(0, length - visible);
        }
    }
}
