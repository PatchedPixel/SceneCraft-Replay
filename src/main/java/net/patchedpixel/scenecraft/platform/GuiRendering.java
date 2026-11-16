package net.patchedpixel.scenecraft.platform;

import net.minecraftforge.client.ForgeHooksClient;

public final class GuiRendering {
    private GuiRendering() {}

    /**
     * Far plane of the orthographic projection the gui is rendered with. Forge leaves room for layered screens.
     */
    public static float getGuiFarPlane() {
        return ForgeHooksClient.getGuiFarPlane();
    }
}
