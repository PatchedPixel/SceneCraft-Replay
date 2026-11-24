package net.patchedpixel.scenecraft.common.flamex.util.color;

/** Field names are part of the saved render settings JSON; do not rename them. */
public record Color(int red, int green, int blue, int alpha) {
    public Color {
        red = clamp(red);
        green = clamp(green);
        blue = clamp(blue);
        alpha = clamp(alpha);
    }

    public Color(int red, int green, int blue) {
        this(red, green, blue, 255);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(value, 255));
    }

    public int toARGB() {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }
}
