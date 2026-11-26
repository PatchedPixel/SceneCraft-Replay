package net.patchedpixel.scenecraft.common.render.gui;

import net.minecraft.client.gui.layouts.LinearLayout;
import net.patchedpixel.scenecraft.common.flamex.components.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import java.net.URI;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.openURL;
import static net.patchedpixel.scenecraft.common.core.versions.MCVer.setClipboardString;

public class GuiNoFfmpeg extends FlameableScreen {
    private static final String LINK = "https://www.replaymod.com/docs/#installing-ffmpeg";

    private final Runnable goBack;

    public GuiNoFfmpeg(Runnable goBack) {
        super("scenecraft.gui.rendering.error.title", null, 30);
        this.goBack = goBack;
        this.background = Background.DIRT;
    }

    @Override
    protected void build(LinearLayout root) {
        root.addChild(FlameX.label("scenecraft.gui.rendering.error.message"));
        root.addChild(FlameX.label(MCVer.literalText(LINK)));
        LinearLayout buttons = FlameX.row(4);
        buttons.addChild(FlameX.button("chat.link.open", 100, () -> openURL(URI.create(LINK))));
        buttons.addChild(FlameX.button("chat.copy", 100, () -> setClipboardString(LINK)));
        buttons.addChild(FlameX.button("gui.back", 100, goBack));
        root.addChild(buttons);
    }

    @Override
    public void onClose() {
        goBack.run();
    }
}
