package net.patchedpixel.scenecraft.common.flamex;

import net.minecraft.resources.ResourceLocation;
import net.patchedpixel.scenecraft.common.SceneCraft;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.identifier;

public final class FlameXTheme {
    private FlameXTheme() {}

    public static final ResourceLocation GUI_TEXTURE = identifier(SceneCraft.MOD_ID, "gui.png");

    /** Nine-slice panel in the GUI texture. */
    public static final class Panel {
        private Panel() {}

        public static final int U = 0;
        public static final int V = 39;
        public static final int SIZE = 17;
        public static final int BORDER = 5;
        public static final int PADDING = 10;
    }

    /** Full-screen backdrops, ARGB. */
    public static final class Text {
        private Text() {}

        public static final int TITLE_COLOR = 0xFFFFFF;
        public static final int TITLE_Y = 10;
        public static final int ON_PANEL = 0x000000;
    }

    /** Full-screen backdrops, ARGB. */
    public static final class Overlay {
        private Overlay() {}

        public static final int DIM = 0xC0000000;
        public static final int SCRIM = 0xE6000000;
    }

    /** Slider colors, ARGB. */
    public static final class Slider {
        private Slider() {}

        public static final int TRACK = 0xFF000000;
        public static final int THUMB = 0xFFC0C0C0;
        public static final int SHADOW = 0xFF808080;
    }
}
