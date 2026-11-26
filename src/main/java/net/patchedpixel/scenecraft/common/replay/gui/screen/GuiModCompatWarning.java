package net.patchedpixel.scenecraft.common.replay.gui.screen;

import net.minecraft.client.gui.layouts.LinearLayout;
import net.patchedpixel.scenecraft.common.scenestudio.data.ModInfo;
import net.patchedpixel.scenecraft.common.flamex.components.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.widget.TextListWidget;
import net.patchedpixel.scenecraft.common.utils.ModCompat;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.minecraft.client.resources.language.I18n;

import java.util.Map;

public class GuiModCompatWarning extends FlameableScreen {
    private final ModCompat.ModInfoDifference difference;
    private final Runnable onLoad;

    public GuiModCompatWarning(ModCompat.ModInfoDifference difference, Runnable onLoad) {
        super("scenecraft.gui.modwarning.title", null);
        this.difference = difference;
        this.onLoad = onLoad;
    }

    @Override
    protected void build(LinearLayout root) {}

    @Override
    protected void init() {
        LinearLayout buttons = FlameX.row(5);
        buttons.addChild(FlameX.button("scenecraft.gui.load", 200, onLoad));
        buttons.addChild(FlameX.button("gui.cancel", 200, () -> MCVer.getMinecraft().setScreen(null)));
        buttons.visitWidgets(this::addRenderableWidget);
        buttons.arrangeElements();
        buttons.setPosition(width / 2 - buttons.getWidth() / 2, height - 10 - buttons.getHeight());

        TextListWidget content = new TextListWidget(width - 20, buttons.getY() - 10 - 35, 35, buttons.getY() - 10);
        content.setLeftPos(10);
        content.addLine(FlameX.t("scenecraft.gui.modwarning.message1"), true);
        content.addLine(FlameX.t("scenecraft.gui.modwarning.message2"), true);
        content.addLine(FlameX.t("scenecraft.gui.modwarning.message3"), true);
        if(!difference.getMissing().isEmpty()) {
            content.addBlank();
            content.addLine(FlameX.t("scenecraft.gui.modwarning.missing"), true);
            for(ModInfo modInfo : difference.getMissing()) {
                addModInfo(content, modInfo, I18n.get("scenecraft.gui.modwarning.version.expected") + ": " + modInfo.getVersion());
            }
        }
        if(!difference.getDiffering().isEmpty()) {
            content.addBlank();
            content.addLine(FlameX.t("scenecraft.gui.modwarning.version"), true);
            for(Map.Entry<ModInfo, String> entry : difference.getDiffering().entrySet()) {
                addModInfo(content, entry.getKey(), I18n.get("scenecraft.gui.modwarning.version.expected")
                        + ": " + entry.getKey().getVersion()
                        + ", " + I18n.get("scenecraft.gui.modwarning.version.found")
                        + ": " + entry.getValue());
            }
        }
        addRenderableWidget(content);
    }

    private static void addModInfo(TextListWidget content, ModInfo modInfo, String versionLine) {
        content.addBlank();
        content.addLine(MCVer.literalText(I18n.get("scenecraft.gui.modwarning.name") + " " + modInfo.getName()), true);
        content.addLine(MCVer.literalText(I18n.get("scenecraft.gui.modwarning.id") + " " + modInfo.getId()), true);
        content.addLine(MCVer.literalText(versionLine), true);
    }
}
