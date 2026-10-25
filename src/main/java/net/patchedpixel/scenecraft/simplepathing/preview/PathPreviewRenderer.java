package net.patchedpixel.scenecraft.simplepathing.preview;

import net.patchedpixel.scenecraft.core.SceneCraft;
import net.patchedpixel.scenecraft.core.events.PostRenderWorldCallback;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.pathing.properties.CameraProperties;
import net.patchedpixel.scenecraft.pathing.properties.SpectatorProperty;
import net.patchedpixel.scenecraft.pathing.properties.TimestampProperty;
import net.patchedpixel.scenecraft.render.hooks.EntityRendererHandler;
import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.patchedpixel.scenecraft.scenestudio.pathing.interpolation.Interpolator;
import net.patchedpixel.scenecraft.scenestudio.pathing.path.Keyframe;
import net.patchedpixel.scenecraft.scenestudio.pathing.path.Path;
import net.patchedpixel.scenecraft.scenestudio.pathing.path.PathSegment;
import net.patchedpixel.scenecraft.scenestudio.util.EntityPositionTracker;
import net.patchedpixel.scenecraft.scenestudio.util.Location;
import net.patchedpixel.scenecraft.simplepathing.SceneCraftSimplePathing;
import net.patchedpixel.scenecraft.simplepathing.SPTimeline;
import net.patchedpixel.scenecraft.simplepathing.gui.GuiPathing;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.Optional;

import static net.patchedpixel.scenecraft.core.SceneCraft.TEXTURE;
import static net.patchedpixel.scenecraft.core.versions.MCVer.identifier;

public class PathPreviewRenderer extends EventRegistrations {
    private static final ResourceLocation CAMERA_HEAD = identifier(SceneCraft.MOD_ID, "camera_head.png");
    private static final Minecraft mc = MCVer.getMinecraft();

    private static final int SLOW_PATH_COLOR = 0xffcccc;
    private static final int FAST_PATH_COLOR = 0x660000;
    private static final double FASTEST_PATH_SPEED = 0.01;

    private final SceneCraftSimplePathing mod;
    private final ReplayHandler replayHandler;

    public PathPreviewRenderer(SceneCraftSimplePathing mod, ReplayHandler replayHandler) {
        this.mod = mod;
        this.replayHandler = replayHandler;
    }

    { on(PostRenderWorldCallback.EVENT, this::renderCameraPath); }
    private void renderCameraPath(PoseStack matrixStack) {
        if (((EntityRendererHandler.IEntityRenderer) mc.gameRenderer).sceneCraftRender_getHandler() != null) return;
        if (!replayHandler.getReplaySender().isAsyncMode() || mc.options.hideGui) return;

        Entity view = mc.getCameraEntity();
        if (view == null) return;

        GuiPathing guiPathing = mod.getGuiPathing();
        if (guiPathing == null) return;
        EntityPositionTracker entityTracker = guiPathing.getEntityTracker();

        SPTimeline timeline = mod.getCurrentTimeline();
        if (timeline == null) return;
        Path path = timeline.getPositionPath();
        if (path.getKeyframes().isEmpty()) return;
        Path timePath = timeline.getTimePath();

        path.update();

        int renderDistance = mc.options.renderDistance().get() * 16;
        int renderDistanceSquared = renderDistance * renderDistance;

        Vector3f viewPos = new Vector3f(
                (float) view.getX(),
                (float) view.getY(),
                (float) view.getZ()
        );

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        try {
            for (PathSegment segment : path.getSegments()) {
                Interpolator interpolator = segment.getInterpolator();
                Keyframe start = segment.getStartKeyframe();
                Keyframe end = segment.getEndKeyframe();
                long diff = (int) (end.getTime() - start.getTime());

                boolean spectator = interpolator.getKeyframeProperties().contains(SpectatorProperty.PROPERTY);
                if (spectator && entityTracker == null) {
                    continue; // Cannot render spectator positions when entity tracker is not yet loaded
                }
                // Spectator segments have 20 lines per second (at least 10) whereas normal segments have a fixed 100
                long steps = spectator ? Math.max(diff / 50, 10) : 100;
                Vector3f prevPos = null;
                for (int i = 0; i <= steps; i++) {
                    long time = start.getTime() + diff * i / steps;
                    if (spectator) {
                        Optional<Integer> entityId = path.getValue(SpectatorProperty.PROPERTY, time);
                        Optional<Integer> replayTime = timePath.getValue(TimestampProperty.PROPERTY, time);
                        if (entityId.isPresent() && replayTime.isPresent()) {
                            Location loc = entityTracker.getEntityPositionAtTimestamp(entityId.get(), replayTime.get());
                            if (loc != null) {
                                Vector3f pos = loc2Vec(loc);
                                if (prevPos != null) {
                                    drawConnection(matrixStack, viewPos, prevPos, pos, 0x0000ffff, renderDistanceSquared);
                                }
                                prevPos = pos;
                                continue;
                            }
                        }
                    } else {
                        Optional<Vector3f> optPos = path.getValue(CameraProperties.POSITION, time).map(this::tripleD2Vec);
                        if (optPos.isPresent()) {
                            Vector3f pos = optPos.get();
                            if (prevPos != null) {
                                double distance = Math.sqrt(distanceSquared(prevPos, pos));
                                double speed = Math.min(distance / (diff / steps), FASTEST_PATH_SPEED);
                                double speedFraction = speed / FASTEST_PATH_SPEED;
                                int color = interpolateColor(SLOW_PATH_COLOR, FAST_PATH_COLOR, speedFraction);
                                drawConnection(matrixStack, viewPos, prevPos, pos, (color << 8) | 0xff, renderDistanceSquared);
                            }
                            prevPos = pos;
                            continue;
                        }
                    }
                    prevPos = null;
                }
            }

            path.getKeyframes().stream()
                    .map(k -> Pair.of(k, k.getValue(CameraProperties.POSITION).map(this::tripleD2Vec)))
                    .filter(p -> p.getRight().isPresent())
                    .map(p -> Pair.of(p.getLeft(), p.getRight().get()))
                    .filter(p -> distanceSquared(p.getRight(), viewPos) < renderDistanceSquared)
                    .sorted(new KeyframeComparator(viewPos)) // Need to render the furthest first
                    .forEachOrdered(p -> drawPoint(matrixStack, viewPos, p.getRight(), p.getLeft()));

            int time = guiPathing.timeline.getCursorPosition();
            Optional<Integer> entityId = path.getValue(SpectatorProperty.PROPERTY, time);
            if (entityId.isPresent()) {
                // Spectating an entity
                if (entityTracker != null) {
                    Optional<Integer> replayTime = timePath.getValue(TimestampProperty.PROPERTY, time);
                    if (replayTime.isPresent()) {
                        Location loc = entityTracker.getEntityPositionAtTimestamp(entityId.get(), replayTime.get());
                        if (loc != null) {
                            drawCamera(matrixStack, viewPos, loc2Vec(loc), new Vector3f(loc.getYaw(), loc.getPitch(), 0f));
                        }
                    }
                }
            } else {
                // Normal camera path
                Optional<Vector3f> cameraPos = path.getValue(CameraProperties.POSITION, time).map(this::tripleD2Vec);
                Optional<Vector3f> cameraRot = path.getValue(CameraProperties.ROTATION, time).map(this::tripleF2Vec);
                if (cameraPos.isPresent() && cameraRot.isPresent()) {
                    drawCamera(matrixStack, viewPos, cameraPos.get(), cameraRot.get());
                }
            }
        } finally {
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.lineWidth(1.0F);
        }
    }

    private Vector3f loc2Vec(Location loc) {
        return new Vector3f((float) loc.getX(), (float) loc.getY(), (float) loc.getZ());
    }

    private Vector3f tripleD2Vec(Triple<Double, Double, Double> loc) {
        return new Vector3f(loc.getLeft().floatValue(), loc.getMiddle().floatValue(), loc.getRight().floatValue());
    }

    private Vector3f tripleF2Vec(Triple<Float, Float, Float> loc) {
        return new Vector3f(loc.getLeft(), loc.getMiddle(), loc.getRight());
    }

    private static int interpolateColor(int c1, int c2, double weight) {
        return (interpolateColorComponent((c1 >> 16) & 0xff, (c2 >> 16) & 0xff, weight) << 16)
                | (interpolateColorComponent((c1 >> 8) & 0xff, (c2 >> 8) & 0xff, weight) << 8)
                | interpolateColorComponent(c1 & 0xff, c2 & 0xff, weight);
    }

    private static int interpolateColorComponent(int c1, int c2, double weight) {
        return (int) (c1 + (1 - Math.pow(Math.E, -4 * weight)) * (c2 - c1)) & 0xff;
    }

    private static double distanceSquared(Vector3f p1, Vector3f p2) {
        return p1.distanceSquared(p2);
    }

    private void drawConnection(PoseStack stack, Vector3f view, Vector3f pos1, Vector3f pos2, int color, int renderDistanceSquared) {
        if (distanceSquared(view, pos1) > renderDistanceSquared) return;
        if (distanceSquared(view, pos2) > renderDistanceSquared) return;

        emitLines(stack, new Vector3f(pos1).sub(view), new Vector3f(pos2).sub(view), color);
    }

    private void emitLines(PoseStack stack, Vector3f from, Vector3f to, int color) {
        Matrix4f matrix = stack.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.lineWidth(3f);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        int red = (color >> 24) & 0xFF;
        int green = (color >> 16) & 0xFF;
        int blue = (color >> 8) & 0xFF;
        int alpha = color & 0xFF;
        buffer.vertex(matrix, from.x, from.y, from.z).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, to.x, to.y, to.z).color(red, green, blue, alpha).endVertex();
        BufferUploader.drawWithShader(buffer.end());
    }

    private void drawPoint(PoseStack stack, Vector3f view, Vector3f pos, Keyframe keyframe) {
        stack.pushPose();

        Vector3f t = new Vector3f(pos).sub(view);
        stack.translate(t.x, t.y, t.z);
        stack.mulPose(MCVer.quaternion(-mc.getEntityRenderDispatcher().camera.getYRot(), new Vector3f(0, 1, 0)));
        stack.mulPose(MCVer.quaternion(mc.getEntityRenderDispatcher().camera.getXRot(), new Vector3f(1, 0, 0)));

        float size = 10f / SceneCraft.TEXTURE_SIZE;
        float posX = 80f / SceneCraft.TEXTURE_SIZE + (keyframe.getValue(SpectatorProperty.PROPERTY).isPresent() ? size : 0);
        float posY = 0f + (mod.isSelected(keyframe) ? size : 0);

        float minX = -0.5f;
        float minY = -0.5f;
        float maxX = 0.5f;
        float maxY = 0.5f;

        RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
        Matrix4f pose = stack.last().pose();
        vertex(buffer, pose, minX, minY, 0, posX + size, posY + size, 255);
        vertex(buffer, pose, minX, maxY, 0, posX + size, posY, 255);
        vertex(buffer, pose, maxX, maxY, 0, posX, posY, 255);
        vertex(buffer, pose, maxX, minY, 0, posX, posY + size, 255);
        BufferUploader.drawWithShader(buffer.end());

        stack.popPose();
    }

    private void drawCamera(PoseStack stack, Vector3f view, Vector3f pos, Vector3f rot) {
        stack.pushPose();

        Vector3f t = new Vector3f(pos).sub(view);
        stack.translate(t.x, t.y, t.z);
        stack.mulPose(MCVer.quaternion(-rot.x, new Vector3f(0, 1, 0)));
        stack.mulPose(MCVer.quaternion(rot.y, new Vector3f(1, 0, 0)));
        stack.mulPose(MCVer.quaternion(rot.z, new Vector3f(0, 0, 1)));

        emitLines(stack, new Vector3f(0, 0, 0), new Vector3f(0, 0, 2), 0x00ff00aa);

        float cubeSize = 0.5f;
        float r = -cubeSize / 2;

        RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
        RenderSystem.setShaderTexture(0, CAMERA_HEAD);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
        Matrix4f pose = stack.last().pose();

        //back
        vertex(buffer, pose, r, r + cubeSize, r, 3 * 8 / 64f, 8 / 64f, 200);
        vertex(buffer, pose, r + cubeSize, r + cubeSize, r, 4 * 8 / 64f, 8 / 64f, 200);
        vertex(buffer, pose, r + cubeSize, r, r, 4 * 8 / 64f, 2 * 8 / 64f, 200);
        vertex(buffer, pose, r, r, r, 3 * 8 / 64f, 2 * 8 / 64f, 200);

        //front
        vertex(buffer, pose, r + cubeSize, r, r + cubeSize, 2 * 8 / 64f, 2 * 8 / 64f, 200);
        vertex(buffer, pose, r + cubeSize, r + cubeSize, r + cubeSize, 2 * 8 / 64f, 8 / 64f, 200);
        vertex(buffer, pose, r, r + cubeSize, r + cubeSize, 8 / 64f, 8 / 64f, 200);
        vertex(buffer, pose, r, r, r + cubeSize, 8 / 64f, 2 * 8 / 64f, 200);

        //left
        vertex(buffer, pose, r + cubeSize, r + cubeSize, r, 0, 8 / 64f, 200);
        vertex(buffer, pose, r + cubeSize, r + cubeSize, r + cubeSize, 8 / 64f, 8 / 64f, 200);
        vertex(buffer, pose, r + cubeSize, r, r + cubeSize, 8 / 64f, 2 * 8 / 64f, 200);
        vertex(buffer, pose, r + cubeSize, r, r, 0, 2 * 8 / 64f, 200);

        //right
        vertex(buffer, pose, r, r + cubeSize, r + cubeSize, 2 * 8 / 64f, 8 / 64f, 200);
        vertex(buffer, pose, r, r + cubeSize, r, 3 * 8 / 64f, 8 / 64f, 200);
        vertex(buffer, pose, r, r, r, 3 * 8 / 64f, 2 * 8 / 64f, 200);
        vertex(buffer, pose, r, r, r + cubeSize, 2 * 8 / 64f, 2 * 8 / 64f, 200);

        //bottom
        vertex(buffer, pose, r + cubeSize, r, r, 3 * 8 / 64f, 0, 200);
        vertex(buffer, pose, r + cubeSize, r, r + cubeSize, 3 * 8 / 64f, 8 / 64f, 200);
        vertex(buffer, pose, r, r, r + cubeSize, 2 * 8 / 64f, 8 / 64f, 200);
        vertex(buffer, pose, r, r, r, 2 * 8 / 64f, 0, 200);

        //top
        vertex(buffer, pose, r, r + cubeSize, r, 8 / 64f, 0, 200);
        vertex(buffer, pose, r, r + cubeSize, r + cubeSize, 8 / 64f, 8 / 64f, 200);
        vertex(buffer, pose, r + cubeSize, r + cubeSize, r + cubeSize, 2 * 8 / 64f, 8 / 64f, 200);
        vertex(buffer, pose, r + cubeSize, r + cubeSize, r, 2 * 8 / 64f, 0, 200);

        BufferUploader.drawWithShader(buffer.end());

        stack.popPose();
    }

    private void vertex(BufferBuilder buffer, Matrix4f pose, float x, float y, float z, float u, float v, int alpha) {
        buffer.vertex(pose, x, y, z).color(255, 255, 255, alpha).uv(u, v).uv2(240, 240).endVertex();
    }

    private class KeyframeComparator implements Comparator<Pair<Keyframe, Vector3f>> {
        private final Vector3f viewPos;

        public KeyframeComparator(Vector3f viewPos) {
            this.viewPos = viewPos;
        }

        @Override
        public int compare(Pair<Keyframe, Vector3f> o1, Pair<Keyframe, Vector3f> o2) {
            return -Double.compare(distanceSquared(o1.getRight(), viewPos), distanceSquared(o2.getRight(), viewPos));
        }
    }
}
