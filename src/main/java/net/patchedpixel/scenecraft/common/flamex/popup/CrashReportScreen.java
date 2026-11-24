package net.patchedpixel.scenecraft.common.flamex.popup;

import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.flamex.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.widget.TextListWidget;

public class CrashReportScreen extends FlameableScreen {
    private final String crashReport;
    private final Runnable onClosed;

    public CrashReportScreen(Screen parent, String crashReport, Runnable onClosed) {
        super("scenecraft.gui.unknownerror", parent);
        this.crashReport = crashReport;
        this.onClosed = onClosed;
    }

    @Override
    protected void build(LinearLayout root) {}

    @Override
    protected void init() {
        LinearLayout buttons = FlameX.row(5);
        buttons.addChild(FlameX.button("chat.copy", 150, () -> MCVer.setClipboardString(crashReport)));
        buttons.addChild(FlameX.button("gui.back", 150, this::close));
        buttons.visitWidgets(this::addRenderableWidget);
        buttons.arrangeElements();
        buttons.setPosition(width / 2 - buttons.getWidth() / 2, height - 10 - buttons.getHeight());

        TextListWidget list = new TextListWidget(width * 3 / 4, buttons.getY() - 10 - 35, 35, buttons.getY() - 10);
        list.setLeftPos(width / 8);
        for(String line : crashReport.replace("\t", "    ").split("\n")) {
            list.addLine(MCVer.literalText(line));
        }

        addRenderableWidget(list);
    }

    @Override
    public void close() {
        MCVer.getMinecraft().setScreen(parent);
        if(onClosed != null) {
            onClosed.run();
        }
    }
}
