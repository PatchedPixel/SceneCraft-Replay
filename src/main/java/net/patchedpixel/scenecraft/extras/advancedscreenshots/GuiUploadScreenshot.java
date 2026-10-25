package net.patchedpixel.scenecraft.extras.advancedscreenshots;

import net.patchedpixel.scenecraft.core.SceneCraft;
import net.patchedpixel.scenecraft.core.SettingsRegistry;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.extras.Setting;
import net.patchedpixel.scenecraft.gui.utils.color.ReadableColor;
import net.patchedpixel.scenecraft.render.RenderSettings;
import net.patchedpixel.scenecraft.gui.container.GuiContainer;
import net.patchedpixel.scenecraft.gui.container.GuiPanel;
import net.patchedpixel.scenecraft.gui.element.GuiButton;
import net.patchedpixel.scenecraft.gui.element.GuiCheckbox;
import net.patchedpixel.scenecraft.gui.element.GuiLabel;
import net.patchedpixel.scenecraft.gui.layout.HorizontalLayout;
import net.patchedpixel.scenecraft.gui.layout.VerticalLayout;
import net.patchedpixel.scenecraft.gui.popup.AbstractGuiPopup;

import java.net.URI;

public class GuiUploadScreenshot extends AbstractGuiPopup<GuiUploadScreenshot> {
    public final SceneCraft mod;

    public final RenderSettings renderSettings;

    public final GuiLabel successLabel = new GuiLabel()
            .setI18nText("scenecraft.gui.advancedscreenshots.finished.description")
            .setColor(ReadableColor.BLACK);

    public final GuiLabel veerLabel = new GuiLabel()
            .setI18nText("scenecraft.gui.advancedscreenshots.finished.description.veer")
            .setColor(ReadableColor.BLACK);

    public final GuiButton veerUploadButton = new GuiButton()
            .setSize(150, 20)
            .setI18nLabel("scenecraft.gui.advancedscreenshots.finished.upload.veer");

    public final GuiButton showOnDiskButton = new GuiButton()
            .setSize(150, 20)
            .setI18nLabel("scenecraft.gui.advancedscreenshots.finished.showfile");

    public final GuiButton closeButton = new GuiButton()
            .setSize(150, 20)
            .setI18nLabel("scenecraft.gui.close");

    public final GuiCheckbox neverOpenCheckbox = new GuiCheckbox();

    public final GuiLabel neverOpenLabel = new GuiLabel()
            .setI18nText("scenecraft.gui.notagain")
            .setColor(ReadableColor.BLACK);

    public final GuiPanel checkboxPanel = GuiPanel.builder()
            .layout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(5))
            .with(neverOpenCheckbox, new HorizontalLayout.Data(0.5))
            .with(neverOpenLabel, new HorizontalLayout.Data(0.5))
            .build();

    public GuiUploadScreenshot(GuiContainer container, SceneCraft mod, RenderSettings renderSettings) {
        super(container);
        this.mod = mod;
        this.renderSettings = renderSettings;

        boolean veer = renderSettings.getRenderMethod() == RenderSettings.RenderMethod.EQUIRECTANGULAR;

        if (renderSettings.getRenderMethod() == RenderSettings.RenderMethod.EQUIRECTANGULAR) {
            successLabel.setI18nText("scenecraft.gui.advancedscreenshots.finished.description.360");
        }

        if (veer) {
            veerUploadButton.onClick(() -> MCVer.openURL(URI.create("https://veer.tv/upload")));
        }

        showOnDiskButton.onClick(() -> MCVer.openFile(renderSettings.getOutputFile().getParentFile()));

        closeButton.onClick(() -> {
            if (neverOpenCheckbox.isChecked()) {
                SettingsRegistry settingsRegistry = mod.getSettingsRegistry();
                settingsRegistry.set(Setting.SKIP_POST_SCREENSHOT_GUI, true);
                settingsRegistry.save();
            }
            close();
        });

        popup.addElements(new VerticalLayout.Data(0.5), successLabel);

        if (veer) {
            popup.addElements(new VerticalLayout.Data(0.5),
                    veerLabel,
                    veerUploadButton);
        }

        popup.addElements(new VerticalLayout.Data(0.5),
                successLabel,
                showOnDiskButton,
                closeButton);

        popup.addElements(new VerticalLayout.Data(1),
                checkboxPanel);

        popup.setLayout(new VerticalLayout().setSpacing(5));
    }

    @Override
    protected void open() {
        if (mod.getSettingsRegistry().get(Setting.SKIP_POST_SCREENSHOT_GUI)) {
            return;
        }
        super.open();
    }

    @Override
    protected GuiUploadScreenshot getThis() {
        return this;
    }
}
