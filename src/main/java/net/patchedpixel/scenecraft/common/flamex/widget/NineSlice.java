package net.patchedpixel.scenecraft.common.flamex.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.patchedpixel.scenecraft.common.flamex.FlameXTheme;

public final class NineSlice {
    private NineSlice() {}

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        ResourceLocation tex = FlameXTheme.GUI_TEXTURE;

        int b = FlameXTheme.PANEL_BORDER;
        int u = FlameXTheme.PANEL_U;
        int v = FlameXTheme.PANEL_V;
        int far = FlameXTheme.PANEL_SIZE - b;
        int mid = FlameXTheme.PANEL_SIZE / 2;
        int innerW = width - 2 * b;
        int innerH = height - 2 * b;

        graphics.blit(tex, x, y, u, v, b, b);
        graphics.blit(tex, x + width - b, y, u + far, v, b, b);
        graphics.blit(tex, x, y + height - b, u, v + far, b, b);
        graphics.blit(tex, x + width - b, y + height - b, u + far, v + far, b, b);
        graphics.blit(tex, x + b, y, innerW, b, u + mid, v, 1, b, 256, 256);
        graphics.blit(tex, x + b, y + height - b, innerW, b, u + mid, v + far, 1, b, 256, 256);
        graphics.blit(tex, x, y + b, b, innerH, u, v + mid, b, 1, 256, 256);
        graphics.blit(tex, x + width - b, y + b, b, innerH, u + far, v + mid, b, 1, 256, 256);
        graphics.blit(tex, x + b, y + b, innerW, innerH, u + mid, v + mid, 1, 1, 256, 256);
    }

    public static void draw(GuiGraphics graphics, int ox, int oy, int width, int height, int u, int v, int uWidth, int vHeight,
                            int topBorder, int bottomBorder, int leftBorder, int rightBorder) {
        draw(graphics, FlameXTheme.GUI_TEXTURE, ox, oy, width, height, u, v, uWidth, vHeight, topBorder, bottomBorder, leftBorder, rightBorder);
    }

    private static void draw(GuiGraphics graphics, ResourceLocation tex, int ox, int oy, int width, int height, int u, int v, int uWidth, int vHeight,
                             int topBorder, int bottomBorder, int leftBorder, int rightBorder) {
        int bodyH = vHeight - topBorder - bottomBorder;
        int bodyW = uWidth - leftBorder - rightBorder;
        for(int pass = 0; pass < 2; pass++) {
            int x = pass == 0 ? 0 : width - rightBorder;
            int tx = pass == 0 ? u : u + uWidth - rightBorder;

            for(int y = topBorder; y < height - bottomBorder; y += bodyH) {
                int segH = Math.min(bodyH, height - bottomBorder - y);
                blit(graphics, tex, ox + x, oy + y, tx, v + topBorder, leftBorder, segH);
            }

            blit(graphics, tex, ox + x, oy, tx, v, leftBorder, topBorder);
            blit(graphics, tex, ox + x, oy + height - bottomBorder, tx, v + vHeight - bottomBorder, leftBorder, bottomBorder);
        }
        for(int x = leftBorder; x < width - rightBorder; x += bodyW) {
            int segW = Math.min(bodyW, width - rightBorder - x);
            int tx = u + leftBorder;

            for(int y = topBorder; y < height - bottomBorder; y += bodyH) {
                int segH = Math.min(bodyH, height - bottomBorder - y);
                blit(graphics, tex, ox + x, oy + y, tx, v + topBorder, segW, segH);
            }

            blit(graphics, tex, ox + x, oy, tx, v, segW, topBorder);
            blit(graphics, tex, ox + x, oy + height - bottomBorder, tx, v + vHeight - bottomBorder, segW, bottomBorder);
        }
    }

    private static void blit(GuiGraphics graphics, ResourceLocation tex, int x, int y, int u, int v, int w, int h) {
        graphics.blit(tex, x, y, u, v, w, h);
    }
}
