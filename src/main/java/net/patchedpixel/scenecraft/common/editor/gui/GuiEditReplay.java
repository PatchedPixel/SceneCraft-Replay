package net.patchedpixel.scenecraft.common.editor.gui;

import net.minecraft.CrashReport;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.editor.SceneCraftEditor;
import net.patchedpixel.scenecraft.common.replay.gui.overlay.GuiMarkerTimeline;
import net.patchedpixel.scenecraft.common.scenestudio.data.Marker;
import net.patchedpixel.scenecraft.common.scenestudio.replay.ReplayFile;
import net.patchedpixel.scenecraft.common.flamex.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.popup.ProgressPopup;
import net.patchedpixel.scenecraft.common.flamex.widget.ScrollbarWidget;
import net.patchedpixel.scenecraft.common.flamex.widget.SpriteButton;
import net.patchedpixel.scenecraft.common.flamex.widget.TimelineTimeWidget;
import net.patchedpixel.scenecraft.common.utils.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GuiEditReplay extends FlameableScreen {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Path inputPath;
    private final EditTimeline timeline;
    private final TimelineTimeWidget timelineTime = new TimelineTimeWidget(300, 8);
    private final ScrollbarWidget scrollbar = new ScrollbarWidget(300, 9);
    private Set<Marker> markers;

    protected GuiEditReplay(Screen parent, Path inputPath) throws IOException {
        super("scenecraft.gui.edit", parent, 10);
        this.panel = true;
        this.inputPath = inputPath;

        LOGGER.info("Opening replay in editor: " + inputPath);

        try (ReplayFile replayFile = SceneCraft.instance.files.open(inputPath)) {
            markers = replayFile.getMarkers().orElseGet(HashSet::new);
            timeline = new EditTimeline(new HashSet<>(markers), markers -> this.markers = markers);
            timeline.setWidth(300);
            timeline.setMarkers()
                    .setLength(replayFile.getMetaData().getDuration())
                    .onClick(timeline::setCursorPosition);
        }

        timelineTime.setTimeline(timeline);

        scrollbar.onValueChanged(() -> {
            timeline.setOffset((int) (scrollbar.getPosition() * timeline.getLength()));
            timeline.setZoom(scrollbar.getZoom());
        }).setZoom(1);
    }

    @Override
    protected void build(LinearLayout root) {
        SpriteButton zoomIn = new SpriteButton(9, 9, SceneCraft.TEXTURE, SceneCraft.TEXTURE_SIZE, 9,
                () -> zoomTimeline(2d / 3d)).uv(40, 20);
        zoomIn.setTooltip(Tooltip.create(Component.translatable("scenecraft.gui.ingame.menu.zoomin")));
        SpriteButton zoomOut = new SpriteButton(9, 9, SceneCraft.TEXTURE, SceneCraft.TEXTURE_SIZE, 9,
                () -> zoomTimeline(3d / 2d)).uv(40, 30);
        zoomOut.setTooltip(Tooltip.create(Component.translatable("scenecraft.gui.ingame.menu.zoomout")));

        LinearLayout timelineColumn = FlameX.column(1);
        timelineColumn.addChild(timelineTime);
        timelineColumn.addChild(timeline);
        timelineColumn.addChild(scrollbar);
        LinearLayout zoomColumn = FlameX.column(2);
        zoomColumn.addChild(zoomIn);
        zoomColumn.addChild(zoomOut);
        LinearLayout timelineRow = FlameX.row(2);
        timelineRow.addChild(timelineColumn);
        timelineRow.addChild(zoomColumn);
        root.addChild(timelineRow);

        LinearLayout controls = FlameX.row(4);
        controls.addChild(markerButton("scenecraft.gui.edit.split", "scenecraft.gui.edit.split.tooltip", MarkerProcessor.MARKER_NAME_SPLIT));
        controls.addChild(markerButton("scenecraft.gui.edit.cut.start", "scenecraft.gui.edit.cut.tooltip", MarkerProcessor.MARKER_NAME_START_CUT));
        controls.addChild(markerButton("scenecraft.gui.edit.cut.end", "scenecraft.gui.edit.cut.tooltip", MarkerProcessor.MARKER_NAME_END_CUT));
        root.addChild(controls);

        LinearLayout buttons = FlameX.row(8);
        buttons.addChild(FlameX.button("scenecraft.gui.edit.apply", 150, this::apply));
        buttons.addChild(FlameX.button("scenecraft.gui.close", 150, this::close));
        root.addChild(buttons);
    }

    private Button markerButton(String labelKey, String tooltipKey, String markerName) {
        return FlameX.button(labelKey, 100, tooltipKey, () -> {
            Marker marker = new Marker();
            marker.setTime(timeline.getCursorPosition());
            marker.setName(markerName);
            timeline.addMarker(marker);
        });
    }

    private void zoomTimeline(double factor) {
        scrollbar.setZoom(scrollbar.getZoom() * factor);
    }

    private void apply() {
        ProgressPopup progressPopup = new ProgressPopup("scenecraft.gui.edit.apply", null);
        progressPopup.open();

        new Thread(() -> {
            try (ReplayFile replayFile = SceneCraft.instance.files.open(inputPath)) {
                replayFile.writeMarkers(markers);
                replayFile.save();
            } catch(IOException e) {
                Utils.error(SceneCraftEditor.LOGGER, CrashReport.forThrowable(e, "Writing markers"), this::close);
            }

            try {
                MarkerProcessor.apply(inputPath, progressPopup::setProgress);

                SceneCraft.instance.runLater(() -> {
                    progressPopup.close();
                    close();
                });
            } catch(Throwable e) {
                e.printStackTrace();
                CrashReport crashReport = CrashReport.forThrowable(e, "Running marker processor");
                SceneCraft.instance.runLater(() -> Utils.error(SceneCraftEditor.LOGGER, crashReport, () -> {
                    progressPopup.close();
                    close();
                }));
            }
        }).start();
    }

    @Override
    public void close() {
        super.close();
    }

    private class EditTimeline extends GuiMarkerTimeline {
        EditTimeline(Set<Marker> markers, Consumer<Set<Marker>> saveMarkers) {
            super(markers, saveMarkers);
        }

        @Override
        protected void drawContent(GuiGraphics graphics, int mouseX, int mouseY) {
            drawCutQuads(graphics);
            super.drawContent(graphics, mouseX, mouseY);
        }

        @Override
        protected void drawMarker(GuiGraphics graphics, Marker marker, int markerX) {
            if(MarkerProcessor.MARKER_NAME_SPLIT.equals(marker.getName())) {
                int height = getHeight() - BORDER_BOTTOM - BORDER_TOP - MARKER_SIZE + 1;
                for(int y = 0; y < height; y += 3) {
                    graphics.fill(markerX, getY() + BORDER_TOP + y, markerX + 1, getY() + BORDER_TOP + y + 2, 0xFFFFFFFF);
                }
            }

            super.drawMarker(graphics, marker, markerX);
        }

        private void drawCutQuads(GuiGraphics graphics) {
            boolean inCut = false;
            int startTime = 0;
            for(Marker marker : markers.stream().sorted(Comparator.comparing(Marker::getTime)).collect(Collectors.toList())) {
                if(MarkerProcessor.MARKER_NAME_START_CUT.equals(marker.getName()) && !inCut) {
                    inCut = true;
                    startTime = marker.getTime();
                } else if(MarkerProcessor.MARKER_NAME_END_CUT.equals(marker.getName()) && inCut) {
                    drawCutQuad(graphics, startTime, marker.getTime());
                    inCut = false;
                }
            }

            if(inCut) {
                drawCutQuad(graphics, startTime, getLength());
            }
        }

        private void drawCutQuad(GuiGraphics graphics, int startFrameTime, int endFrameTime) {
            int visibleWidth = getWidth() - BORDER_LEFT - BORDER_RIGHT;
            int startTime = getOffset();
            int visibleTime = (int) (getZoom() * getLength());
            int endTime = getOffset() + visibleTime;

            if(visibleTime <= 0 || startFrameTime >= endTime || endFrameTime <= startTime) {
                return;
            }

            double relativeStart = startFrameTime - startTime;
            double relativeEnd = endFrameTime - startTime;
            int startX = BORDER_LEFT + Math.max(0, (int) (relativeStart / visibleTime * visibleWidth) + MARKER_SIZE / 2 + 1);
            int endX = BORDER_LEFT + Math.min(visibleWidth, (int) (relativeEnd / visibleTime * visibleWidth) - MARKER_SIZE / 2);
            if(startX < endX) {
                int y = getY() + getHeight() - BORDER_BOTTOM - MARKER_SIZE;
                graphics.fill(getX() + startX + 1, y, getX() + endX - 1, y + MARKER_SIZE - 2, 0xFFFF0000);
            }
        }
    }
}
