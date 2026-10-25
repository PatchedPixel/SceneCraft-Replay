package net.patchedpixel.scenecraft.core.versions;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.glfw.GLFW;
import net.patchedpixel.scenecraft.scenestudio.protocol.State;
import net.patchedpixel.scenecraft.scenestudio.protocol.CurrentProtocol;
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketTypeRegistry;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.CrashReportCategory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.Font;
import java.util.concurrent.Callable;
import java.util.Objects;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.network.chat.Component;
import net.minecraft.client.particle.Particle;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.File;
import java.net.URI;
import java.util.Optional;

/**
 * Abstraction over things that have changed between different MC versions.
 */
public class MCVer {
    public static int getProtocolVersion() {
        return CurrentProtocol.get();
    }

    public static State fromMc(ConnectionProtocol mcState) {
        return switch (mcState) {
            case HANDSHAKING -> State.HANDSHAKE;
            case STATUS -> State.STATUS;
            case LOGIN -> State.LOGIN;
            case PLAY -> State.PLAY;
        };
    }

    public static PacketTypeRegistry getPacketTypeRegistry(ConnectionProtocol state) {
        return getPacketTypeRegistry(fromMc(state));
    }

    public static PacketTypeRegistry getPacketTypeRegistry(State state) {
        return PacketTypeRegistry.get(state);
    }

    public static PacketTypeRegistry getPacketTypeRegistry(boolean loginPhase) {
        return PacketTypeRegistry.get(loginPhase ? State.LOGIN : State.PLAY);
    }

    public static void resizeMainWindow(Minecraft mc, int width, int height) {
        Window window = mc.getWindow();
        window.framebufferWidth = width;
        window.framebufferHeight = height;
        window.width = width;
        window.height = height;
        mc.resizeDisplay();
    }

    public static int currentModifiers() {
        long handle = getMinecraft().getWindow().getWindow();
        int modifiers = 0;
        if (InputConstants.isKeyDown(handle, InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(handle, InputConstants.KEY_RSHIFT)) {
            modifiers |= GLFW.GLFW_MOD_SHIFT;
        }
        if (InputConstants.isKeyDown(handle, InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(handle, InputConstants.KEY_RCONTROL)) {
            modifiers |= GLFW.GLFW_MOD_CONTROL;
        }
        if (InputConstants.isKeyDown(handle, InputConstants.KEY_LALT) || InputConstants.isKeyDown(handle, InputConstants.KEY_RALT)) {
            modifiers |= GLFW.GLFW_MOD_ALT;
        }
        if (InputConstants.isKeyDown(handle, InputConstants.KEY_LWIN) || InputConstants.isKeyDown(handle, InputConstants.KEY_RWIN)) {
            modifiers |= GLFW.GLFW_MOD_SUPER;
        }
        return modifiers;
    }

    public static Minecraft getMinecraft() {
        return Minecraft.getInstance();
    }

    public static void addButton(
            Screen screen,
            Button button
    ) {
        screen.addRenderableWidget(button);
    }

    public static class ScissorBounds {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        public ScissorBounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ScissorBounds that = (ScissorBounds) o;
            return x == that.x && y == that.y && width == that.width && height == that.height;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y, width, height);
        }
    }

    public static Window newScaledResolution(Minecraft mc) {
        return mc.getWindow();
    }

    public static void addDetail(CrashReportCategory category, String name, Callable<String> callable) {
        category.setDetail(name, callable::call);
    }

    public static Font getFontRenderer() {
        return getMinecraft().font;
    }

    public static void setClipboardString(String text) {
        getMinecraft().keyboardHandler.setClipboard(text);
    }

    public static String getClipboardString() {
        return getMinecraft().keyboardHandler.getClipboard();
    }

    public static Component literalText(String str) {
        return Component.literal(str);
    }

    public static ResourceLocation identifier(String id) {
        return ResourceLocation.parse(id);
    }

    public static ResourceLocation identifier(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    public static Optional<AbstractWidget> findButton(Iterable<? extends GuiEventListener> buttonList, @SuppressWarnings("unused") String text, @SuppressWarnings("unused") int id) {
        final Component message = Component.translatable(text);
        for (GuiEventListener e : buttonList) {
            if (e instanceof ContainerEventHandler) {
                Optional<AbstractWidget> button = findButton(((ContainerEventHandler) e).children(), text, id);
                if (button.isPresent()) {
                    return button;
                }
            }
            if (!(e instanceof AbstractWidget)) {
                continue;
            }
            AbstractWidget b = (AbstractWidget) e;
            if (message.equals(b.getMessage())) {
                return Optional.of(b);
            }
            // Fuzzy match (copy does not include children)
            if (b.getMessage() != null && b.getMessage().plainCopy().equals(message)) {
                return Optional.of(b);
            }
        }
        return Optional.empty();
    }

    public static void pollEvents() {
        GLFW.glfwPollEvents();
    }

    public static void pumpEvents() {
        GLFW.glfwPollEvents();
    }

    public static void processKeyBinds() {
        ((MinecraftMethodAccessor) getMinecraft()).sceneCraftProcessKeyBinds();
    }

    public interface MinecraftMethodAccessor {
        void sceneCraftProcessKeyBinds();
        void sceneCraftExecuteTaskQueue();
    }

    public static long milliTime() {
        return Util.getMillis();
    }

    public static Vec3 getPosition(Particle particle, float partialTicks) {
        double x = particle.xo + (particle.x - particle.xo) * partialTicks;
        double y = particle.yo + (particle.y - particle.yo) * partialTicks;
        double z = particle.zo + (particle.z - particle.zo) * partialTicks;
        return new Vec3(x, y, z);
    }

    public static void openFile(File file) {
        Util.getPlatform().openFile(file);
    }

    public static void openURL(URI uri) {
        Util.getPlatform().openUri(uri);
    }

    public static void pushMatrix() {
        RenderSystem.getModelViewStack().pushPose();
    }

    public static void popMatrix() {
        RenderSystem.getModelViewStack().popPose();
    }

    public static org.joml.Quaternionf quaternion(float angle, Vector3f axis) {
        return new org.joml.Quaternionf().fromAxisAngleDeg(axis.x, axis.y, axis.z, angle);
    }

    public static void emitLine(PoseStack.Pose matrixStack, VertexConsumer buffer, Vector2f p1, Vector2f p2, int color, float lineWidth) {
        emitLine(matrixStack, buffer, new Vector3f(p1.x, p1.y, 0), new Vector3f(p2.x, p2.y, 0), color, lineWidth);
    }

    public static void emitLine(PoseStack.Pose matrixStack, VertexConsumer buffer, Vector3f p1, Vector3f p2, int color, float lineWidth) {
        RenderSystem.lineWidth(lineWidth);
        int r = color >> 24 & 0xff;
        int g = color >> 16 & 0xff;
        int b = color >> 8 & 0xff;
        int a = color & 0xff;
        Vector3f n = new Vector3f(p2).sub(p1);
        buffer.vertex(matrixStack.pose(), p1.x, p1.y, p1.z)
                .color(r, g, b, a)
                .normal(matrixStack.normal(), n.x, n.y, n.z)
                .endVertex();
        buffer.getClass();
        buffer.vertex(matrixStack.pose(), p2.x, p2.y, p2.z)
                .color(r, g, b, a)
                .normal(matrixStack.normal(), n.x, n.y, n.z)
                .endVertex();
        buffer.getClass();
    }
}
