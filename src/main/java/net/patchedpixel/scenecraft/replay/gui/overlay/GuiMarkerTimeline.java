package net.patchedpixel.scenecraft.replay.gui.overlay;

import net.patchedpixel.scenecraft.core.SceneCraft;
import com.mojang.blaze3d.platform.InputConstants;
import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.replay.camera.CameraEntity;
import net.patchedpixel.scenecraft.scenestudio.data.Marker;
import net.patchedpixel.scenecraft.scenestudio.util.Location;
import net.patchedpixel.scenecraft.gui.GuiRenderer;
import net.patchedpixel.scenecraft.gui.RenderInfo;
import net.patchedpixel.scenecraft.gui.element.advanced.AbstractGuiTimeline;
import net.patchedpixel.scenecraft.gui.function.Click;
import net.patchedpixel.scenecraft.gui.function.Draggable;
import net.patchedpixel.scenecraft.gui.function.KeyHandler;
import net.patchedpixel.scenecraft.gui.function.KeyInput;
import net.patchedpixel.scenecraft.gui.utils.geometry.Point;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;
import net.minecraft.client.resources.language.I18n;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static net.patchedpixel.scenecraft.gui.utils.Utils.clamp;

public class GuiMarkerTimeline extends AbstractGuiTimeline<GuiMarkerTimeline> implements Draggable, KeyHandler {
    protected static final int TEXTURE_MARKER_X = 109;
    protected static final int TEXTURE_MARKER_Y = 20;
    protected static final int TEXTURE_MARKER_SELECTED_X = 114;
    protected static final int TEXTURE_MARKER_SELECTED_Y = 20;
    protected static final int MARKER_SIZE = 5;

    @Nullable
    private final ReplayHandler replayHandler;
    private final Consumer<Set<Marker>> saveMarkers;
    protected Set<Marker> markers;

    private ReadableDimension lastSize;

    private Marker selectedMarker;
    private int draggingStartX;
    private int draggingTimeDelta;
    private boolean dragging;
    private long lastClickTime;

    public GuiMarkerTimeline(@Nonnull ReplayHandler replayHandler) {
        this.replayHandler = replayHandler;
        try {
            this.markers = replayHandler.getReplayFile().getMarkers().orElseGet(HashSet::new);
        } catch (IOException e) {
            SceneCraftReplay.LOGGER.error("Failed to get markers from replay", e);
            this.markers = new HashSet<>();
        }
        this.saveMarkers = (markers) -> {
            try {
                replayHandler.getReplayFile().writeMarkers(markers);
            } catch (IOException e) {
                SceneCraftReplay.LOGGER.error("Failed to save markers to replay", e);
            }
        };
    }

    public GuiMarkerTimeline(Set<Marker> markers, Consumer<Set<Marker>> saveMarkers) {
        this.replayHandler = null;
        this.markers = markers;
        this.saveMarkers = saveMarkers;
    }

    @Override
    protected GuiMarkerTimeline getThis() {
        return this;
    }

    @Override
    public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
        lastSize = size;
        super.draw(renderer, size, renderInfo);

        drawMarkers(renderer, size);
    }

    protected void drawMarkers(GuiRenderer renderer, ReadableDimension size) {
        renderer.bindTexture(SceneCraft.TEXTURE);

        for (Marker marker : markers) {
            drawMarker(renderer, size, marker);
        }
    }

    protected void drawMarker(GuiRenderer renderer, ReadableDimension size, Marker marker) {
        int visibleLength = (int) (getLength() * getZoom());
        int markerPos = clamp(marker.getTime(), getOffset(), getOffset() + visibleLength);
        double positionInVisible = markerPos - getOffset();
        double fractionOfVisible = positionInVisible / visibleLength;
        int markerX = (int) (BORDER_LEFT + fractionOfVisible * (size.getWidth() - BORDER_LEFT - BORDER_RIGHT));
        drawMarker(renderer, size, marker, markerX);
    }

    protected void drawMarker(GuiRenderer renderer, ReadableDimension size, Marker marker, int markerX) {
        int textureX, textureY;
        if (marker.equals(selectedMarker)) {
            textureX = TEXTURE_MARKER_SELECTED_X;
            textureY = TEXTURE_MARKER_SELECTED_Y;
        } else {
            textureX = TEXTURE_MARKER_X;
            textureY = TEXTURE_MARKER_Y;
        }
        renderer.drawTexturedRect(markerX - 2, size.getHeight() - BORDER_BOTTOM - MARKER_SIZE,
                textureX, textureY, MARKER_SIZE, MARKER_SIZE);
    }

    /**
     * Returns the marker which the mouse is at.
     * @param mouseX X coordinate of the mouse
     * @param mouseY Y coordinate of the mouse
     * @return The marker or {@code null} if the mouse isn't on a marker
     */
    protected Marker getMarkerAt(int mouseX, int mouseY) {
        if (lastSize == null) {
            return null;
        }
        Point mouse = new Point(mouseX, mouseY);
        getContainer().convertFor(this, mouse);
        mouseX = mouse.getX();
        mouseY = mouse.getY();

        if (mouseX < 0 || mouseY < lastSize.getHeight() - BORDER_BOTTOM - MARKER_SIZE
                || mouseX > lastSize.getWidth() || mouseY > lastSize.getHeight() - BORDER_BOTTOM) {
            return null;
        }

        int visibleLength = (int) (getLength() * getZoom());
        int contentWidth = lastSize.getWidth() - BORDER_LEFT - BORDER_RIGHT;
        for (Marker marker : markers) {
            int markerPos = clamp(marker.getTime(), getOffset(), getOffset() + visibleLength);
            double positionInVisible = markerPos - getOffset();
            double fractionOfVisible = positionInVisible / visibleLength;
            int markerX = (int) (BORDER_LEFT + fractionOfVisible * contentWidth);
            if (Math.abs(markerX - mouseX) < 3) {
                return marker;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClick(Click click) {
        Marker marker = getMarkerAt(click.x, click.y);
        if (marker != null) {
            if (click.button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) { // Left click
                long now = System.currentTimeMillis();
                selectedMarker = marker;
                if (Math.abs(lastClickTime - now) > 500) { // Single click
                    draggingStartX = click.x;
                    draggingTimeDelta = marker.getTime() - getTimeAt(click.x, click.y);
                } else { // Double click
                    new GuiEditMarkerPopup(getContainer(), marker, (updatedMarker) -> {
                        markers.remove(marker);
                        markers.add(updatedMarker);
                        saveMarkers.accept(markers);
                    }).open();
                }
                lastClickTime = now;
            } else if (click.button == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT) { // Right click
                selectedMarker = null;
                if (replayHandler != null) {
                    CameraEntity cameraEntity = replayHandler.getCameraEntity();
                    if (cameraEntity != null) {
                        cameraEntity.setCameraPosRot(new Location(
                                marker.getX(), marker.getY(), marker.getZ(),
                                marker.getYaw(), marker.getPitch()
                        ));
                    }
                    replayHandler.doJump(marker.getTime(), true);
                } else {
                    setCursorPosition(marker.getTime());
                }
            }
            return true;
        } else {
            selectedMarker = null;
        }
        return super.mouseClick(click);
    }

    @Override
    public boolean mouseDrag(Click click) {
        if (selectedMarker != null) {
            int diff = click.y - draggingStartX;
            if (Math.abs(diff) > MARKER_SIZE) {
                dragging = true;
            }
            if (dragging) {
                int timeAt = getTimeAt(click.x, click.y);
                if (timeAt != -1) {
                    selectedMarker.setTime(draggingTimeDelta + timeAt);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseRelease(Click click) {
        if (selectedMarker != null) {
            mouseDrag(click);
            if (dragging) {
                dragging = false;
                saveMarkers.accept(markers);
                return true;
            }
        }
        return false;
    }

    @Override
    protected String getTooltipText(RenderInfo renderInfo) {
        Marker marker = getMarkerAt(renderInfo.mouseX, renderInfo.mouseY);
        if (marker != null) {
            return marker.getName() != null ? marker.getName() : I18n.get("scenecraft.gui.ingame.unnamedmarker");
        }
        return super.getTooltipText(renderInfo);
    }

    public void setSelectedMarker(Marker selectedMarker) {
        this.selectedMarker = selectedMarker;
    }

    public Marker getSelectedMarker() {
        return selectedMarker;
    }

    @Override
    public boolean handleKey(KeyInput keyInput) {
        if (keyInput.key == InputConstants.KEY_DELETE && selectedMarker != null) {
            markers.remove(selectedMarker);
            saveMarkers.accept(markers);

            return true;
        }

        return false;
    }

    public void addMarker(Marker marker) {
        markers.add(marker);
        saveMarkers.accept(markers);
    }

    @Override
    public ReadableDimension getLastSize() {
        return super.getLastSize();
    }
}
