package net.patchedpixel.scenecraft.common.core.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.flamex.event.EventRegistrations;
import net.patchedpixel.scenecraft.common.flamex.event.callbacks.PostRenderScreenCallback;
import net.patchedpixel.scenecraft.common.flamex.widget.ProgressBarWidget;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GuiBackgroundProcesses extends EventRegistrations {
    public static class Process {
        private final String titleKey;
        private final ProgressBarWidget progressBar = new ProgressBarWidget(150, 14);

        public Process(String titleKey) {
            this.titleKey = titleKey;
        }

        public ProgressBarWidget getProgressBar() {
            return progressBar;
        }
    }

    private final List<Process> processes = new CopyOnWriteArrayList<>();

    { on(PostRenderScreenCallback.EVENT, this::render); }

    private void render(GuiGraphics graphics, float partialTicks) {
        if(processes.isEmpty()) return;
        Minecraft mc = MCVer.getMinecraft();
        if(mc.screen == null) return;
        Font font = MCVer.getFontRenderer();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int y = 5;
        for(Process process : new ArrayList<>(processes)) {
            String title = I18n.get(process.titleKey);
            int innerWidth = Math.max(font.width(title), process.progressBar.getWidth());
            int width = innerWidth + 20;
            int height = font.lineHeight + 2 + process.progressBar.getHeight() + 20;
            int x = screenWidth - 5 - width;
            panel(graphics, x, y, width, height);
            graphics.drawString(font, title, x + 10 + (innerWidth - font.width(title)) / 2, y + 10, 0xFF000000, false);
            process.progressBar.setPosition(x + 10, y + 10 + font.lineHeight + 2);
            process.progressBar.setWidth(innerWidth);
            process.progressBar.render(graphics, -1, -1, partialTicks);
            y += height + 10;
        }
    }

    private static void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        final int u0 = 0, v0 = 39;
        graphics.blit(SceneCraft.TEXTURE, x, y, u0, v0, 5, 5);
        graphics.blit(SceneCraft.TEXTURE, x + w - 5, y, u0 + 12, v0, 5, 5);
        graphics.blit(SceneCraft.TEXTURE, x, y + h - 5, u0, v0 + 12, 5, 5);
        graphics.blit(SceneCraft.TEXTURE, x + w - 5, y + h - 5, u0 + 12, v0 + 12, 5, 5);
        for(int px = 5; px < w - 5; px += 5) {
            int rx = Math.min(5, w - 5 - px);
            graphics.blit(SceneCraft.TEXTURE, x + px, y, u0 + 6, v0, rx, 5);
            graphics.blit(SceneCraft.TEXTURE, x + px, y + h - 5, u0 + 6, v0 + 12, rx, 5);
        }
        for(int py = 5; py < h - 5; py += 5) {
            int ry = Math.min(5, h - 5 - py);
            graphics.blit(SceneCraft.TEXTURE, x, y + py, u0, v0 + 6, 5, ry);
            graphics.blit(SceneCraft.TEXTURE, x + w - 5, y + py, u0 + 12, v0 + 6, 5, ry);
        }
        for(int px = 5; px < w - 5; px += 5) {
            for(int py = 5; py < h - 5; py += 5) {
                int rx = Math.min(5, w - 5 - px);
                int ry = Math.min(5, h - 5 - py);
                graphics.blit(SceneCraft.TEXTURE, x + px, y + py, u0 + 6, v0 + 6, rx, ry);
            }
        }
    }

    public void addProcess(Process process) {
        processes.add(process);
    }

    public void removeProcess(Process process) {
        processes.remove(process);
    }
}
