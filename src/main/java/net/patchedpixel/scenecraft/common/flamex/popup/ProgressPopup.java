package net.patchedpixel.scenecraft.common.flamex.popup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.flamex.components.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.widget.ProgressBarWidget;

import javax.annotation.Nullable;

public class ProgressPopup {
    private final ProgressBarWidget progressBar;
    private final Screen screen;
    @Nullable private Screen previous;
    private boolean open;

    public ProgressPopup(@Nullable String labelKey, @Nullable Runnable onCancel) {
        this.progressBar = new ProgressBarWidget(300, 20);
        if(labelKey != null) {
            progressBar.setLabel(net.minecraft.client.resources.language.I18n.get(labelKey));
        }
        this.screen = new FlameableScreen(Component.empty(), null, 10) {
            { this.panel = true; }

            @Override
            protected void build(LinearLayout root) {
                root.addChild(progressBar);
                if(onCancel != null) {
                    root.addChild(FlameX.button("gui.cancel", 200, onCancel));
                }
            }

            @Override
            public boolean shouldCloseOnEsc() {
                return false;
            }

            @Override
            public void onClose() {}
        };
    }

    public ProgressPopup(@Nullable String labelKey) {
        this(labelKey, null);
    }

    public ProgressBarWidget getProgressBar() {
        return progressBar;
    }

    public void setProgress(float progress) {
        progressBar.setProgress(progress);
    }

    public void open() {
        Minecraft mc = MCVer.getMinecraft();
        if(open) return;
        open = true;
        previous = mc.screen;
        mc.setScreen(screen);
    }

    public void close() {
        Minecraft mc = MCVer.getMinecraft();
        if(!open) return;
        open = false;
        if(mc.screen == screen) {
            mc.setScreen(previous);
        }
    }
}
