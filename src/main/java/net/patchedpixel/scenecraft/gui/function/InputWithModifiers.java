package net.patchedpixel.scenecraft.gui.function;

import net.minecraft.Util;
import org.lwjgl.glfw.GLFW;

public interface InputWithModifiers {
    int modifiers();

    default boolean hasCtrl() {
        return (modifiers() & _CTRL_MOD) != 0;
    }
    default boolean hasShift() {
        return (modifiers() & _SHIFT_MOD) != 0;
    }
    default boolean hasAlt() {
        return (modifiers() & _ALT_MOD) != 0;
    }

    int _CTRL_MOD = Util.getPlatform() == Util.OS.OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;
    int _SHIFT_MOD = GLFW.GLFW_MOD_SHIFT;
    int _ALT_MOD = GLFW.GLFW_MOD_ALT;
}
