package net.patchedpixel.scenecraft.common.flamex;

import net.minecraft.resources.ResourceLocation;
import net.patchedpixel.scenecraft.common.SceneCraft;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.identifier;

public final class FlameXTheme {
    private FlameXTheme() {}

    public static final ResourceLocation GUI_TEXTURE = identifier(SceneCraft.MOD_ID, "gui.png");
    public static final int PANEL_U = 0;
    public static final int PANEL_V = 39;
    public static final int PANEL_SIZE = 17;
    public static final int PANEL_BORDER = 5;
    public static final int PANEL_PADDING = 10;

    public static final int TITLE_COLOR = 0xFFFFFF;
    public static final int TITLE_Y = 10;
    public static final int TEXT_ON_PANEL = 0x000000;
    public static final int SCRIM = 0xE6000000;
    public static final int SLIDER_TRACK = 0xFF000000;
    public static final int SLIDER_THUMB = 0xFFC0C0C0;
    public static final int SLIDER_SHADOW = 0xFF808080;
}
