package net.patchedpixel.scenecraft.replay.gui.screen;

import net.patchedpixel.scenecraft.core.utils.ModCompat;
import net.patchedpixel.scenecraft.scenestudio.data.ModInfo;
import net.patchedpixel.scenecraft.scenestudio.util.I18n;
import net.patchedpixel.scenecraft.gui.container.AbstractGuiScreen;
import net.patchedpixel.scenecraft.gui.container.GuiPanel;
import net.patchedpixel.scenecraft.gui.container.GuiVerticalList;
import net.patchedpixel.scenecraft.gui.element.GuiButton;
import net.patchedpixel.scenecraft.gui.element.GuiLabel;
import net.patchedpixel.scenecraft.gui.layout.CustomLayout;
import net.patchedpixel.scenecraft.gui.layout.HorizontalLayout;
import net.patchedpixel.scenecraft.gui.layout.VerticalLayout;

import java.util.Map;

public class GuiModCompatWarning extends AbstractGuiScreen<GuiModCompatWarning> {
    public final GuiVerticalList content = new GuiVerticalList(this).setDrawShadow(true).setDrawSlider(true);
    public final GuiButton loadButton = new GuiButton().setI18nLabel("scenecraft.gui.load").setSize(200, 20);
    public final GuiButton cancelButton = new GuiButton().setI18nLabel("gui.cancel").setSize(200, 20);
    public final GuiPanel closeButtons = new GuiPanel(this).setLayout(new HorizontalLayout().setSpacing(5))
            .addElements(null, loadButton, cancelButton);

    {
        setTitle(new GuiLabel().setI18nText("scenecraft.gui.modwarning.title"));
        setLayout(new CustomLayout<GuiModCompatWarning>() {
            @Override
            protected void layout(GuiModCompatWarning container, int width, int height) {
                pos(content, 10, 35);
                pos(closeButtons, width / 2 - width(closeButtons) / 2, height - 10 - height(closeButtons));
                size(content, width - 20, y(closeButtons) - 10 - y(content));
            }
        });

        content.getListLayout().setSpacing(8);
    }

    public GuiModCompatWarning(ModCompat.ModInfoDifference difference) {
        VerticalLayout.Data data = new VerticalLayout.Data(0.5);
        GuiPanel content = this.content.getListPanel();
        content.addElements(data, new GuiLabel().setI18nText("scenecraft.gui.modwarning.message1"));
        content.addElements(data, new GuiLabel().setI18nText("scenecraft.gui.modwarning.message2"));
        content.addElements(data, new GuiLabel().setI18nText("scenecraft.gui.modwarning.message3"));

        if (!difference.getMissing().isEmpty()) {
            content.addElements(data, new GuiLabel());
            content.addElements(data, new GuiLabel().setI18nText("scenecraft.gui.modwarning.missing"));
            for (ModInfo modInfo : difference.getMissing()) {
                content.addElements(data, new GuiPanel().setLayout(new VerticalLayout().setSpacing(3)).addElements(null,
                        GuiPanel.builder().layout(new HorizontalLayout().setSpacing(3))
                                .with(new GuiLabel().setI18nText("scenecraft.gui.modwarning.name"), null)
                                .with(new GuiLabel().setText(modInfo.getName()), null).build(),
                        GuiPanel.builder().layout(new HorizontalLayout().setSpacing(3))
                                .with(new GuiLabel().setI18nText("scenecraft.gui.modwarning.id"), null)
                                .with(new GuiLabel().setText(modInfo.getId()), null).build(),
                        new GuiLabel().setText(I18n.format("scenecraft.gui.modwarning.version.expected")
                                + ": " + modInfo.getVersion())
                ));
            }
        }

        if (!difference.getDiffering().isEmpty()) {
            content.addElements(data, new GuiLabel());
            content.addElements(data, new GuiLabel().setI18nText("scenecraft.gui.modwarning.version"));
            for (Map.Entry<ModInfo, String> entry : difference.getDiffering().entrySet()) {
                content.addElements(data, new GuiPanel().setLayout(new VerticalLayout().setSpacing(3)).addElements(null,
                        GuiPanel.builder().layout(new HorizontalLayout().setSpacing(3))
                                .with(new GuiLabel().setI18nText("scenecraft.gui.modwarning.name"), null)
                                .with(new GuiLabel().setText(entry.getKey().getName()), null).build(),
                        GuiPanel.builder().layout(new HorizontalLayout().setSpacing(3))
                                .with(new GuiLabel().setI18nText("scenecraft.gui.modwarning.id"), null)
                                .with(new GuiLabel().setText(entry.getKey().getId()), null).build(),
                        new GuiLabel().setText(I18n.format("scenecraft.gui.modwarning.version.expected")
                                + ": " + entry.getKey().getVersion()
                                + ", " + I18n.format("scenecraft.gui.modwarning.version.found")
                                + ": " + entry.getValue())
                ));
            }
        }

        // FIXME remap bug: openScreen pattern doesn't apply properly without {}
        cancelButton.onClick(() -> {
            getMinecraft().setScreen(null);
        });
    }

    @Override
    protected GuiModCompatWarning getThis() {
        return this;
    }
}
