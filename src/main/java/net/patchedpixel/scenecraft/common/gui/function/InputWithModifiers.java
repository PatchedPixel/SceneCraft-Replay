package net.patchedpixel.scenecraft.common.gui.function;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.Util;

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

    int _CTRL_MOD = Util.getPlatform() == Util.OS.OSX ? InputConstants.MOD_SUPER : InputConstants.MOD_CONTROL;
    int _SHIFT_MOD = InputConstants.MOD_SHIFT;
    int _ALT_MOD = InputConstants.MOD_ALT;
}
