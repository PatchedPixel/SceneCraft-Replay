package net.patchedpixel.scenecraft.common.flamex.screen.popup;

import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.flamex.screen.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;

public class PleaseWaitScreen extends FlameableScreen {
    public PleaseWaitScreen() {
        super(Component.empty(), null);
        this.background = Background.DIRT;
    }

    @Override
    protected void build(LinearLayout root) {
        root.addChild(FlameX.label("scenecraft.gui.pleasewait"));
    }
}
