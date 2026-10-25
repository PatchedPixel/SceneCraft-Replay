package net.patchedpixel.scenecraft.render.gui;

import net.patchedpixel.scenecraft.core.SettingsRegistry;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.render.RenderSettings;
import net.patchedpixel.scenecraft.render.SceneCraftRender;
import net.patchedpixel.scenecraft.render.Setting;
import net.patchedpixel.scenecraft.gui.container.GuiPanel;
import net.patchedpixel.scenecraft.gui.container.GuiScreen;
import net.patchedpixel.scenecraft.gui.element.GuiButton;
import net.patchedpixel.scenecraft.gui.element.GuiCheckbox;
import net.patchedpixel.scenecraft.gui.element.GuiLabel;
import net.patchedpixel.scenecraft.gui.layout.CustomLayout;
import net.patchedpixel.scenecraft.gui.layout.HorizontalLayout;
import net.patchedpixel.scenecraft.gui.layout.VerticalLayout;

import java.io.File;

public class GuiRenderingDone extends GuiScreen {
    public final SceneCraftRender mod;
    public final File videoFile;
    public final int videoFrames;
    public final RenderSettings settings;

    public final GuiLabel infoLine1 = new GuiLabel().setI18nText("scenecraft.gui.renderdone1");
    public final GuiLabel infoLine2 = new GuiLabel().setI18nText("scenecraft.gui.renderdone2");

    public final GuiButton openFolder = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            MCVer.openFile(videoFile.getParentFile());
        }
    }).setSize(200, 20).setI18nLabel("scenecraft.gui.openfolder");

    public final GuiPanel actionsPanel = new GuiPanel().setLayout(new VerticalLayout().setSpacing(10))
            .addElements(null, openFolder);

    public final GuiPanel mainPanel = new GuiPanel(this).setLayout(new VerticalLayout().setSpacing(10))
            .addElements(new VerticalLayout.Data(0.5),
                    new GuiPanel().setLayout(new VerticalLayout().setSpacing(4)).addElements(null, infoLine1, infoLine2),
                    actionsPanel);

    public final GuiButton closeButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            if (neverOpenCheckbox.isChecked()) {
                SettingsRegistry settingsRegistry = mod.getCore().getSettingsRegistry();
                settingsRegistry.set(Setting.SKIP_POST_RENDER_GUI, true);
                settingsRegistry.save();
            }
            getMinecraft().setScreen(null);
        }
    }).setSize(100, 20).setI18nLabel("scenecraft.gui.close");

    public final GuiCheckbox neverOpenCheckbox = new GuiCheckbox().setI18nLabel("scenecraft.gui.notagain");

    public final GuiPanel closePanel = new GuiPanel(this)
            .setLayout(new HorizontalLayout(HorizontalLayout.Alignment.RIGHT).setSpacing(5))
            .addElements(new HorizontalLayout.Data(0.5), neverOpenCheckbox, closeButton);

    {
        setLayout(new CustomLayout<GuiScreen>() {
            @Override
            protected void layout(GuiScreen container, int width, int height) {
                pos(mainPanel, width / 2 - width(mainPanel) / 2, height / 3 - height(mainPanel) / 2);
                pos(closePanel, width - 10 - width(closePanel), height - 10 - height(closePanel));
            }
        });
        setTitle(new GuiLabel().setI18nText("scenecraft.gui.renderdonetitle"));
        setBackground(Background.DIRT);
    }

    public GuiRenderingDone(SceneCraftRender mod, File videoFile, int videoFrames, RenderSettings settings) {
        this.mod = mod;
        this.videoFile = videoFile;
        this.videoFrames = videoFrames;
        this.settings = settings;
    }

    @Override
    public void display() {
        if (mod.getCore().getSettingsRegistry().get(Setting.SKIP_POST_RENDER_GUI)) {
            return;
        }
        super.display();
    }
}
