package net.patchedpixel.scenecraft.common.render.hooks;

import net.patchedpixel.scenecraft.common.render.gui.progress.VirtualWindow;
import net.minecraft.client.Minecraft;

public interface WindowDelegateHolder {
    void setWindowDelegate(VirtualWindow window);

    static WindowDelegateHolder get(Minecraft mc) {
        return (WindowDelegateHolder) mc;
    }
}
