package net.patchedpixel.scenecraft.common.flamex.screen.popup;

import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.flamex.screen.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.widget.TextListWidget;

public class CrashReportScreen extends FlameableScreen {
    private final String crashReport;
    private final Runnable onClosed;

    public CrashReportScreen(Screen parent, String crashReport, Runnable onClosed) {
        super(Component.empty(), parent, 10);
        this.panel = true;
        this.dim = true;
        this.crashReport = crashReport;
        this.onClosed = onClosed;
    }

    @Override
    protected void build(LinearLayout root) {
        LinearLayout info = FlameX.column(2);
        info.addChild(FlameX.label("scenecraft.gui.unknownerror"));
        TextListWidget list = new TextListWidget(0, 0, width * 3 / 4, height * 3 / 4)
                .setSpacing(2).setPadding(0).setCenterContent(false).setTextColor(0xFF000000);
        for(String line : crashReport.replace("\t", "    ").split("\n")) {
            list.addLine(MCVer.literalText(line));
        }
        info.addChild(list);
        root.addChild(info);

        LinearLayout buttons = FlameX.row(5);
        buttons.addChild(FlameX.button("chat.copy", 150, () -> MCVer.setClipboardString(crashReport)));
        buttons.addChild(FlameX.button("gui.back", 150, this::close));
        root.addChild(buttons, root.newChildLayoutSettings().alignHorizontallyRight());
    }

    @Override
    public void close() {
        MCVer.getMinecraft().setScreen(parent);
        if(onClosed != null) {
            onClosed.run();
        }
    }
}
