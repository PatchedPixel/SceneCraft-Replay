package net.patchedpixel.scenecraft.common.flamex.popup;

import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import javax.annotation.Nullable;

public class MessageScreen extends PopupScreen {
    private final Runnable onAccept;
    private final String yesKey;
    private final String noKey;
    private final Component[] lines;
    private Runnable onDecline;

    public MessageScreen(@Nullable Screen parent, Runnable onAccept, @Nullable String yesKey, @Nullable String noKey, Component... lines) {
        super(parent, 7);
        this.dim = true;
        this.onAccept = onAccept;
        this.yesKey = yesKey;
        this.noKey = noKey;
        this.lines = lines;
    }

    public MessageScreen onDecline(Runnable onDecline) {
        this.onDecline = onDecline;
        return this;
    }

    @Override
    public void close() {
        super.close();
        if(onDecline != null) onDecline.run();
    }

    @Override
    protected void build(LinearLayout root) {
        for(Component line : lines) root.addChild(label(line));
        LinearLayout buttons = FlameX.row(5);

        if(yesKey == null) {
            buttons.addChild(FlameX.button("gui.back", 200, this::accept));
        } else {
            buttons.addChild(FlameX.button(yesKey, 98, this::accept));
            buttons.addChild(FlameX.button(noKey, 98, this::close));
        }

        root.addChild(buttons);
    }

    private void accept() {
        MCVer.getMinecraft().setScreen(parent);
        onAccept.run();
    }
}
