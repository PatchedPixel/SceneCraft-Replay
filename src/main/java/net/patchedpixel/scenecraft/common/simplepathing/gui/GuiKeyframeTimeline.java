package net.patchedpixel.scenecraft.common.simplepathing.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.pathing.properties.CameraProperties;
import net.patchedpixel.scenecraft.common.pathing.properties.SpectatorProperty;
import net.patchedpixel.scenecraft.common.pathing.properties.TimestampProperty;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.common.replay.gui.overlay.GuiMarkerTimeline;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.change.Change;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.path.Keyframe;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.path.Path;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.path.PathSegment;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.property.Property;
import net.patchedpixel.scenecraft.common.simplepathing.SPTimeline;
import net.patchedpixel.scenecraft.common.simplepathing.SPTimeline.SPPath;
import net.patchedpixel.scenecraft.common.simplepathing.SceneCraftSimplePathing;
import net.patchedpixel.scenecraft.common.flamex.widget.TimelineWidget;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class GuiKeyframeTimeline extends TimelineWidget {
    protected static final int KEYFRAME_SIZE = 5;
    protected static final int KEYFRAME_TEXTURE_X = 74;
    protected static final int KEYFRAME_TEXTURE_Y = 20;
    private static final int DOUBLE_CLICK_INTERVAL = 250;
    private static final int DRAGGING_THRESHOLD = KEYFRAME_SIZE;
    private final GuiPathing gui;

    /**
     * The keyframe (time on timeline) that was last clicked on using the left mouse button.
     */
    private long lastClickedKeyframe;

    /**
     * Path of {@link #lastClickedKeyframe}.
     */
    private SPPath lastClickedPath;

    /**
     * The time at which {@link #lastClickedKeyframe} was updated.
     * According to {@link MCVer#milliTime()}.
     */
    private long lastClickedTime;

    /**
     * Whether to handle dragging events.
     */
    private boolean dragging;

    /**
     * Whether the current left-button press started on empty space of this timeline (click-and-hold scrubbing).
     */
    private boolean scrubbing;

    /**
     * Whether we have surpassed the initial threshold and are actually dragging the keyframe.
     */
    private boolean actuallyDragging;

    /**
     * Where the mouse was when {@link #dragging} started.
     */
    private int draggingStartX;

    /**
     * Change caused by dragging. Whenever the user moves the keyframe further, the previous change is undone
     * and a new one is created. This way when the mouse is released, only one change is in the undo history.
     */
    private Change draggingChange;

    public GuiKeyframeTimeline(GuiPathing gui) {
        super(100, 20);
        this.gui = gui;
    }

    @Override
    protected void drawTimelineCursor(GuiGraphics graphics) {
        SceneCraftSimplePathing mod = gui.getMod();

        int width = getWidth();
        int visibleWidth = width - BORDER_LEFT - BORDER_RIGHT;
        int startTime = getOffset();
        int visibleTime = (int) (getZoom() * getLength());
        int endTime = getOffset() + visibleTime;

        TimeTimelineLinesRenderState linesRenderState = new TimeTimelineLinesRenderState();

        SPTimeline timeline = mod.getCurrentTimeline();

        timeline.getTimeline().getPaths().stream().flatMap(path -> path.getKeyframes().stream()).forEach(keyframe -> {
            if(keyframe.getTime() >= startTime && keyframe.getTime() <= endTime) {
                double relativeTime = keyframe.getTime() - startTime;
                int positonX = BORDER_LEFT + (int) (relativeTime / visibleTime * visibleWidth) - KEYFRAME_SIZE / 2;
                int u = KEYFRAME_TEXTURE_X + (mod.isSelected(keyframe) ? KEYFRAME_SIZE : 0);
                int v = KEYFRAME_TEXTURE_Y;
                if(keyframe.getValue(CameraProperties.POSITION).isPresent()) {
                    if(keyframe.getValue(SpectatorProperty.PROPERTY).isPresent()) {
                        v += 2 * KEYFRAME_SIZE;
                    }
                    graphics.blit(SceneCraft.TEXTURE, getX() + positonX, getY() + BORDER_TOP, u, v, KEYFRAME_SIZE, KEYFRAME_SIZE);
                }
                Optional<Integer> timeProperty = keyframe.getValue(TimestampProperty.PROPERTY);
                if(timeProperty.isPresent()) {
                    v += KEYFRAME_SIZE;
                    graphics.blit(SceneCraft.TEXTURE, getX() + positonX, getY() + BORDER_TOP + KEYFRAME_SIZE, u, v, KEYFRAME_SIZE, KEYFRAME_SIZE);

                    GuiMarkerTimeline replayTimeline = gui.overlay.timeline;
                    GuiKeyframeTimeline keyframeTimeline = this;

                    int replayTimelineLeft = replayTimeline.getX();
                    int replayTimelineRight = replayTimeline.getX() + replayTimeline.getWidth();
                    int replayTimelineTop = replayTimeline.getY();
                    int replayTimelineBottom = replayTimeline.getY() + replayTimeline.getHeight();
                    int replayTimelineWidth = replayTimelineRight - replayTimelineLeft - BORDER_LEFT - BORDER_RIGHT;

                    int keyframeTimelineLeft = keyframeTimeline.getX();
                    int keyframeTimelineTop = keyframeTimeline.getY();

                    float positionXReplayTimeline = BORDER_LEFT + timeProperty.get() / (float) replayTimeline.getLength() * replayTimelineWidth;
                    float positionXKeyframeTimeline = positonX + KEYFRAME_SIZE / 2f;

                    final int color = 0xff0000ff;
                    final float lineWidth = 2f;

                    // Start just below the top border of the replay timeline
                    Vector2f p1 = new Vector2f(replayTimelineLeft + positionXReplayTimeline, replayTimelineTop + BORDER_TOP);
                    // Draw vertically over the replay timeline, including its bottom border
                    Vector2f p2 = new Vector2f(replayTimelineLeft + positionXReplayTimeline, replayTimelineBottom);
                    // Now for the important part: connecting to the keyframe timeline
                    Vector2f p3 = new Vector2f(keyframeTimelineLeft + positionXKeyframeTimeline, keyframeTimelineTop);
                    // And finally another vertical bit (the timeline is already crammed enough, so only the border)
                    Vector2f p4 = new Vector2f(keyframeTimelineLeft + positionXKeyframeTimeline, keyframeTimelineTop + BORDER_TOP);

                    linesRenderState.color = color;
                    linesRenderState.lineWidth = lineWidth;
                    linesRenderState.line(p1, p2);
                    linesRenderState.line(p2, p3);
                    linesRenderState.line(p3, p4);
                }
            }
        });

        if(!linesRenderState.lines.isEmpty()) {
            // The lines connect this timeline to the replay timeline above it and therefore leave the bounds of this
            // element, so they are drawn directly instead of through a clipped sprite call.
            graphics.flush();

            Minecraft mc = MCVer.getMinecraft();
            float guiScale = (float) mc.getWindow().getGuiScale();
            Matrix4f matrix = graphics.pose().last().pose();
            int lineColor = linesRenderState.color;
            int red = (lineColor >> 24) & 0xFF;
            int green = (lineColor >> 16) & 0xFF;
            int blue = (lineColor >> 8) & 0xFF;
            int alpha = lineColor & 0xFF;

            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.lineWidth(linesRenderState.lineWidth * guiScale);
            BufferBuilder buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            for(Pair<Vector2f, Vector2f> line : linesRenderState.lines) {
                buffer.vertex(matrix, line.getLeft().x, line.getLeft().y, 0).color(red, green, blue, alpha).endVertex();
                buffer.vertex(matrix, line.getRight().x, line.getRight().y, 0).color(red, green, blue, alpha).endVertex();
            }
            BufferUploader.drawWithShader(buffer.end());
            RenderSystem.lineWidth(1.0F);
        }

        // Draw colored quads on spectator path segments
        for(PathSegment segment : timeline.getPositionPath().getSegments()) {
            if(segment.getInterpolator() == null
                    || !segment.getInterpolator().getKeyframeProperties().contains(SpectatorProperty.PROPERTY)) {
                continue; // Not a spectator segment
            }
            drawQuadOnSegment(graphics, visibleWidth, segment, BORDER_TOP + 1, 0xFF0088FF);
        }

        // Draw red quads on time path segments that would require time going backwards
        for(PathSegment segment : timeline.getTimePath().getSegments()) {
            long startTimestamp = segment.getStartKeyframe().getValue(TimestampProperty.PROPERTY).orElseThrow(IllegalStateException::new);
            long endTimestamp = segment.getEndKeyframe().getValue(TimestampProperty.PROPERTY).orElseThrow(IllegalStateException::new);
            if(endTimestamp >= startTimestamp) {
                continue; // All is fine, time is not moving backwards
            }
            drawQuadOnSegment(graphics, visibleWidth, segment, BORDER_TOP + KEYFRAME_SIZE + 1, 0xFFFF0000);
        }

        super.drawTimelineCursor(graphics);
    }

    private static class TimeTimelineLinesRenderState {
        List<Pair<Vector2f, Vector2f>> lines = new ArrayList<>();
        int color;
        float lineWidth;

        public void line(Vector2f p1, Vector2f p2) {
            lines.add(Pair.of(p1, p2));
        }
    }

    private void drawQuadOnSegment(GuiGraphics graphics, int visibleWidth, PathSegment segment, int y, int color) {
        int startTime = getOffset();
        int visibleTime = (int) (getZoom() * getLength());
        int endTime = getOffset() + visibleTime;

        long startFrameTime = segment.getStartKeyframe().getTime();
        long endFrameTime = segment.getEndKeyframe().getTime();
        if(startFrameTime >= endTime || endFrameTime <= startTime) {
            return; // Segment out of display range
        }

        double relativeStart = startFrameTime - startTime;
        double relativeEnd = endFrameTime - startTime;
        int startX = BORDER_LEFT + Math.max(0, (int) (relativeStart / visibleTime * visibleWidth) + KEYFRAME_SIZE / 2 + 1);
        int endX = BORDER_LEFT + Math.min(visibleWidth, (int) (relativeEnd / visibleTime * visibleWidth) - KEYFRAME_SIZE / 2);
        if(startX < endX) {
            graphics.fill(getX() + startX + 1, getY() + y, getX() + endX - 1, getY() + y + KEYFRAME_SIZE - 2, color);
        }
    }

    /**
     * Returns the keyframe at the specified position.
     * @param position The raw position
     * @return Pair of path id and keyframe or null when no keyframe was clicked
     */
    private Pair<SPPath, Long> getKeyframe(double mx, double my) {
        int time = getTimeAt(mx, my);
        if(time != -1) {
            int mouseY = (int) (my - getY());
            if(mouseY > BORDER_TOP && mouseY < BORDER_TOP + 2 * KEYFRAME_SIZE) {
                SPPath path;
                if(mouseY <= BORDER_TOP + KEYFRAME_SIZE) {
                    // Position keyframe
                    path = SPPath.POSITION;
                } else {
                    // Time keyframe
                    path = SPPath.TIME;
                }
                int visibleTime = (int) (getZoom() * getLength());
                int tolerance = visibleTime * KEYFRAME_SIZE / (getWidth() - BORDER_LEFT - BORDER_RIGHT) / 2;
                Optional<Keyframe> keyframe = gui.getMod().getCurrentTimeline().getPath(path).getKeyframes().stream()
                        .filter(k -> Math.abs(k.getTime() - time) <= tolerance)
                        .sorted(Comparator.comparing(k -> Math.abs(k.getTime() - time)))
                        .findFirst();
                return Pair.of(path, keyframe.map(Keyframe::getTime).orElse(null));
            }
        }
        return Pair.of(null, null);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int clickButton) {
        if(!active || !visible || !isMouseOver(mouseX, mouseY)) return false;
        scrubbing = false; // drop any stale state from a press whose release never reached us
        int time = getTimeAt(mouseX, mouseY);
        Pair<SPPath, Long> pathKeyframePair = getKeyframe(mouseX, mouseY);
        if(pathKeyframePair.getRight() != null) {
            SPPath path = pathKeyframePair.getLeft();
            // Clicked on keyframe
            long keyframeTime = pathKeyframePair.getRight();
            if(clickButton == InputConstants.MOUSE_BUTTON_LEFT) {
                long now = MCVer.milliTime();
                if(lastClickedKeyframe == keyframeTime) {
                    // Clicked the same keyframe again, potentially a double click
                    if(now - lastClickedTime < DOUBLE_CLICK_INTERVAL) {
                        // Yup, double click, open the edit keyframe gui
                        gui.openEditKeyframePopup(path, keyframeTime);
                        return true;
                    }
                }
                // Not a double click, just update the click time and selection
                lastClickedTime = now;
                lastClickedKeyframe = keyframeTime;
                lastClickedPath = path;
                gui.getMod().setSelected(lastClickedPath, lastClickedKeyframe);
                // We might be dragging
                draggingStartX = (int) mouseX;
                dragging = true;
            } else if(clickButton == InputConstants.MOUSE_BUTTON_RIGHT) {
                Keyframe keyframe = gui.getMod().getCurrentTimeline().getKeyframe(path, keyframeTime);
                for(Property property : keyframe.getProperties()) {
                    applyPropertyToGame(property, keyframe);
                }
            }
            return true;
        } else if(time != -1) {
            // Clicked on timeline but not on any keyframe
            if(clickButton == InputConstants.MOUSE_BUTTON_LEFT) {
                setCursorPosition(time);
                scrubbing = true;
                gui.getMod().setSelected(null, 0);
            } else if(clickButton == InputConstants.MOUSE_BUTTON_RIGHT) {
                if(pathKeyframePair.getLeft() != null) {
                    // Apply the value of the clicked path at the clicked position
                    Path path = gui.getMod().getCurrentTimeline().getPath(pathKeyframePair.getLeft());
                    path.getKeyframes().stream().flatMap(k -> k.getProperties().stream()).distinct().forEach(
                            p -> applyPropertyToGame(p, path, time));
                }
            }
            return true;
        }
        // Missed timeline
        return false;
    }

    // Helper method because generics cannot be defined on blocks
    private <T> void applyPropertyToGame(Property<T> property, Path path, long time) {
        Optional<T> value = path.getValue(property, time);
        if(value.isPresent()) {
            property.applyToGame(value.get(), SceneCraftReplay.instance.getReplayHandler());
        }
    }

    // Helper method because generics cannot be defined on blocks
    private <T> void applyPropertyToGame(Property<T> property, Keyframe keyframe) {
        Optional<T> value = keyframe.getValue(property);
        if(value.isPresent()) {
            property.applyToGame(value.get(), SceneCraftReplay.instance.getReplayHandler());
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int dragButton, double dx, double dy) {
        if(!dragging) {
            if(scrubbing && dragButton == InputConstants.MOUSE_BUTTON_LEFT) {
                // Left click, the user might try to move the cursor by clicking and holding
                int time = getTimeAt(mouseX, mouseY);
                if(time != -1) {
                    // and they are still on the timeline, so update the time appropriately
                    setCursorPosition(time);
                    return true;
                }
            }
            return false;
        }

        if(!actuallyDragging) {
            // Check if threshold has been passed by now
            if(Math.abs((int) mouseX - draggingStartX) >= DRAGGING_THRESHOLD) {
                actuallyDragging = true;
            }
        }
        if(actuallyDragging) {
            if(!gui.loadEntityTracker(() -> mouseDragged(mouseX, mouseY, dragButton, dx, dy))) return true;
            // Threshold passed
            SPTimeline timeline = gui.getMod().getCurrentTimeline();
            int localX = (int) (mouseX - getX());
            int width = getWidth();
            int bodyWidth = width - BORDER_LEFT - BORDER_RIGHT;
            double segmentLength = getLength() * getZoom();
            double segmentTime =  segmentLength * (localX - BORDER_LEFT) / bodyWidth;
            int newTime = Math.min(Math.max((int) Math.round(getOffset() + segmentTime), 0), getLength());
            if(newTime < 0) {
                return true;
            }

            // If there already is a keyframe at the target time, then increase the time by one until there is none
            while(timeline.getKeyframe(lastClickedPath, newTime) != null) {
                newTime++;
            }

            // First undo any previous changes
            if(draggingChange != null) {
                draggingChange.undo(timeline.getTimeline());
            }

            // Move keyframe to new position and
            // store change for later undoing / pushing to history
            draggingChange = timeline.moveKeyframe(lastClickedPath, lastClickedKeyframe, newTime);

            // Selected keyframe has been replaced
            gui.getMod().setSelected(lastClickedPath, newTime);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int releaseButton) {
        scrubbing = false;
        if(dragging) {
            if(actuallyDragging) {
                gui.getMod().getCurrentTimeline().getTimeline().pushChange(draggingChange);
                draggingChange = null;
                actuallyDragging = false;
            }
            dragging = false;
            return true;
        }
        return false;
    }

}
