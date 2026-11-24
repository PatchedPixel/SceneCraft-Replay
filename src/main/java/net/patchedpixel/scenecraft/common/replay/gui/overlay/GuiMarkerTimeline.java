package net.patchedpixel.scenecraft.common.replay.gui.overlay;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.replay.ReplayHandler;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.common.replay.camera.CameraEntity;
import net.patchedpixel.scenecraft.common.scenestudio.data.Marker;
import net.patchedpixel.scenecraft.common.scenestudio.util.Location;
import net.patchedpixel.scenecraft.common.flamex.widget.TimelineWidget;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class GuiMarkerTimeline extends TimelineWidget {
    protected static final int TEXTURE_MARKER_X = 109;
    protected static final int TEXTURE_MARKER_Y = 20;
    protected static final int TEXTURE_MARKER_SELECTED_X = 114;
    protected static final int TEXTURE_MARKER_SELECTED_Y = 20;
    protected static final int MARKER_SIZE = 5;

    @Nullable
    private final ReplayHandler replayHandler;
    private final Consumer<Set<Marker>> saveMarkers;
    protected Set<Marker> markers;

    private Marker selectedMarker;
    private int draggingStartX;
    private int draggingTimeDelta;
    private boolean dragging;
    private long lastClickTime;

    public GuiMarkerTimeline(@Nonnull ReplayHandler replayHandler) {
        super(100, 20);
        this.replayHandler = replayHandler;
        try {
            this.markers = replayHandler.getReplayFile().getMarkers().orElseGet(HashSet::new);
        } catch(IOException e) {
            SceneCraftReplay.LOGGER.error("Failed to get markers from replay", e);
            this.markers = new HashSet<>();
        }
        this.saveMarkers = (markers) -> {
            try {
                replayHandler.getReplayFile().writeMarkers(markers);
            } catch(IOException e) {
                SceneCraftReplay.LOGGER.error("Failed to save markers to replay", e);
            }
        };
    }

    public GuiMarkerTimeline(Set<Marker> markers, Consumer<Set<Marker>> saveMarkers) {
        super(100, 20);
        this.replayHandler = null;
        this.markers = markers;
        this.saveMarkers = saveMarkers;
    }

    @Override
    protected void drawContent(GuiGraphics graphics, int mouseX, int mouseY) {
        for(Marker marker : markers) {
            drawMarker(graphics, marker);
        }
    }

    protected void drawMarker(GuiGraphics graphics, Marker marker) {
        int visibleLength = (int) (getLength() * getZoom());
        if(visibleLength <= 0) return;
        int markerPos = Math.max(getOffset(), Math.min(getOffset() + visibleLength, marker.getTime()));
        int markerX = getX() + xOffsetFor(markerPos);
        drawMarker(graphics, marker, markerX);
    }

    protected void drawMarker(GuiGraphics graphics, Marker marker, int markerX) {
        int textureX, textureY;
        if(marker.equals(selectedMarker)) {
            textureX = TEXTURE_MARKER_SELECTED_X;
            textureY = TEXTURE_MARKER_SELECTED_Y;
        } else {
            textureX = TEXTURE_MARKER_X;
            textureY = TEXTURE_MARKER_Y;
        }
        graphics.blit(SceneCraft.TEXTURE, markerX - 2, getY() + getHeight() - BORDER_BOTTOM - MARKER_SIZE,
                textureX, textureY, MARKER_SIZE, MARKER_SIZE);
    }

    /** @return the marker at the absolute mouse position, or null */
    protected Marker getMarkerAt(double mouseX, double mouseY) {
        double mx = mouseX - getX();
        double my = mouseY - getY();
        if(mx < 0 || my < getHeight() - BORDER_BOTTOM - MARKER_SIZE
                || mx > getWidth() || my > getHeight() - BORDER_BOTTOM) {
            return null;
        }
        int visibleLength = (int) (getLength() * getZoom());
        if(visibleLength <= 0) return null;
        for(Marker marker : markers) {
            int markerPos = Math.max(getOffset(), Math.min(getOffset() + visibleLength, marker.getTime()));
            if(Math.abs(xOffsetFor(markerPos) - mx) < 3) {
                return marker;
            }
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(!active || !visible || !isMouseOver(mouseX, mouseY)) return false;
        Marker marker = getMarkerAt(mouseX, mouseY);
        if(marker != null) {
            if(button == InputConstants.MOUSE_BUTTON_LEFT) {
                long now = System.currentTimeMillis();
                selectedMarker = marker;
                if(Math.abs(lastClickTime - now) > 500) { // Single click
                    draggingStartX = (int) mouseX;
                    draggingTimeDelta = marker.getTime() - getTimeAt(mouseX, mouseY);
                } else { // Double click
                    Minecraft mc = MCVer.getMinecraft();
                    mc.setScreen(new GuiEditMarkerPopup(mc.screen, marker, (updatedMarker) -> {
                        markers.remove(marker);
                        markers.add(updatedMarker);
                        saveMarkers.accept(markers);
                    }));
                }
                lastClickTime = now;
            } else if(button == InputConstants.MOUSE_BUTTON_RIGHT) {
                selectedMarker = null;
                if(replayHandler != null) {
                    CameraEntity cameraEntity = replayHandler.getCameraEntity();
                    if(cameraEntity != null) {
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
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if(selectedMarker != null) {
            int diff = (int) mouseX - draggingStartX;
            if(Math.abs(diff) > MARKER_SIZE) {
                dragging = true;
            }
            if(dragging) {
                int timeAt = getTimeAt(mouseX, mouseY);
                if(timeAt != -1) {
                    selectedMarker.setTime(draggingTimeDelta + timeAt);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(selectedMarker != null && dragging) {
            mouseDragged(mouseX, mouseY, button, 0, 0);
            dragging = false;
            saveMarkers.accept(markers);
            return true;
        }
        return false;
    }

    @Override
    protected String getTooltipText(int mouseX, int mouseY) {
        Marker marker = getMarkerAt(mouseX, mouseY);
        if(marker != null) {
            return marker.getName() != null ? marker.getName() : I18n.get("scenecraft.gui.ingame.unnamedmarker");
        }
        return super.getTooltipText(mouseX, mouseY);
    }

    public void setSelectedMarker(Marker selectedMarker) {
        this.selectedMarker = selectedMarker;
    }

    public Marker getSelectedMarker() {
        return selectedMarker;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(keyCode == InputConstants.KEY_DELETE && selectedMarker != null) {
            markers.remove(selectedMarker);
            saveMarkers.accept(markers);
            selectedMarker = null;
            return true;
        }
        return false;
    }

    public void addMarker(Marker marker) {
        markers.add(marker);
        saveMarkers.accept(markers);
    }
}
