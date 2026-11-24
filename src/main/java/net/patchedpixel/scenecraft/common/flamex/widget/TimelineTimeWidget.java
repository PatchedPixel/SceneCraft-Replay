package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import org.jetbrains.annotations.NotNull;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

public class TimelineTimeWidget extends AbstractWidget {
    private TimelineWidget timeline;

    public TimelineTimeWidget(int width, int height) {
        super(0, 0, width, height, CommonComponents.EMPTY);
        this.active = false;
    }

    public TimelineTimeWidget setTimeline(TimelineWidget timeline) {
        this.timeline = timeline;
        return this;
    }

    public TimelineWidget getTimeline() {
        return timeline;
    }

    @Override
    protected void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if(timeline == null || timeline.getLength() <= 0) return;
        Font font = MCVer.getFontRenderer();

        int shift = timeline.getX() - getX();
        int visibleLength = (int) (timeline.getLength() * timeline.getZoom());
        int interval = timeline.getMarkerInterval();
        int time = timeline.getOffset() / interval * interval;

        while(time <= timeline.getOffset() + visibleLength) {
            if(time >= timeline.getOffset()) {
                String str = timeline.getLength() > 60 * 60 * 1000
                        ? String.format("%02d:%02d:%02d", time / 1000 / 60 / 60, time / 1000 / 60 % 60, time / 1000 % 60)
                        : String.format("%02d:%02d", time / 1000 / 60, time / 1000 % 60);
                int w = font.width(str);
                int x = timeline.xOffsetFor(time) + shift;
                x = Math.max(w / 2, Math.min(getWidth() - w / 2, x));
                graphics.drawString(font, str, getX() + x - w / 2, getY(), 0xFFFFFFFF, true);
            }
            time += interval;
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {}
}
