package net.patchedpixel.scenecraft.extras.advancedscreenshots;

import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.render.RenderSettings;
import net.patchedpixel.scenecraft.render.gui.GuiRenderSettings;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.gui.container.GuiContainer;
import net.patchedpixel.scenecraft.gui.container.GuiPanel;
import net.patchedpixel.scenecraft.gui.container.GuiScreen;
import net.patchedpixel.scenecraft.gui.element.GuiLabel;
import net.patchedpixel.scenecraft.gui.function.Loadable;
import net.patchedpixel.scenecraft.gui.layout.GridLayout;
import net.patchedpixel.scenecraft.gui.layout.VerticalLayout;
import net.minecraft.CrashReport;

import java.io.File;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static net.patchedpixel.scenecraft.platform.Utils.error;
import static net.patchedpixel.scenecraft.render.SceneCraftRender.LOGGER;

public class GuiCreateScreenshot extends GuiRenderSettings implements Loadable {
    private final SceneCraft mod;

    public GuiCreateScreenshot(SceneCraft mod) {
        super(GuiRenderSettings.createBaseScreen(), null, null);

        this.mod = mod;

        resetChildren(settingsList.getListPanel()).addElements(new VerticalLayout.Data(0.5),
                new GuiLabel().setI18nText("scenecraft.gui.advancedscreenshots.title"), mainPanel, new GuiPanel(),
                new GuiLabel().setI18nText("scenecraft.gui.rendersettings.advanced"), advancedPanel, new GuiPanel());

        resetChildren(mainPanel).addElements(new GridLayout.Data(1, 0.5),
                new GuiLabel().setI18nText("scenecraft.gui.rendersettings.renderer"), renderMethodDropdown,
                new GuiLabel().setI18nText("scenecraft.gui.advancedscreenshots.resolution"), videoResolutionPanel,
                new GuiLabel().setI18nText("scenecraft.gui.rendersettings.outputfile"), outputFileButton);

        resetChildren(advancedPanel).addElements(null, nametagCheckbox, alphaCheckbox , new GuiPanel().setLayout(
                new GridLayout().setCellsEqualSize(false).setColumns(2).setSpacingX(5).setSpacingY(15))
                .addElements(new GridLayout.Data(0, 0.5),
                        new GuiLabel().setI18nText("scenecraft.gui.rendersettings.stabilizecamera"), stabilizePanel,
                        chromaKeyingCheckbox, chromaKeyingColor));

        exportArguments.setText(""); // To disable any preset-based checks
        buttonPanel.removeElement(queueButton);
        renderButton.setI18nLabel("scenecraft.gui.advancedscreenshots.create").onClick(click -> {
            // Closing this GUI ensures that settings are saved
            close();

            mod.runLater(() -> {
                try {
                    RenderSettings settings = save(false, click.hasCtrl());

                    boolean success = new ScreenshotRenderer(settings).renderScreenshot();
                    if (success) {
                        GuiScreen screen = createBaseScreen();
                        new GuiUploadScreenshot(screen, mod, settings).open();
                        screen.display();
                    }

                } catch (Throwable t) {
                    error(LOGGER, GuiCreateScreenshot.this, CrashReport.forThrowable(t, "Rendering video"), () -> {});
                    getScreen().display(); // Re-show the render settings gui and the new error popup
                }
            });
        });
    }

    private <T extends GuiContainer<?>> T resetChildren(T container) {
        new ArrayList<>(container.getChildren()).forEach(container::removeElement);
        return container;
    }

    @Override
    public void open() {
        super.open();
        getScreen().display();
    }

    @Override
    public void close() {
        super.close();
        getMinecraft().gui.setScreen(null);
    }

    @Override
    public void load() {
        // pause replay when opening this gui
        SceneCraftReplay.instance.getReplayHandler().getReplaySender().setReplaySpeed(0);
    }

    @Override
    protected File generateOutputFile(RenderSettings.EncodingPreset encodingPreset) {
        DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
        File screenshotFolder = new File(getMinecraft().gameDirectory, "screenshots");
        screenshotFolder.mkdirs();
        String baseName = DATE_FORMAT.format(new Date());
        for (int i = 1; ; i++) {
            File screenshotFile = new File(screenshotFolder, baseName + (i == 1 ? "" : "_" + i) + ".png");
            if (!screenshotFile.exists()) {
                return screenshotFile;
            }
        }
    }

    @Override
    public void load(RenderSettings settings) {
        super.load(settings.withEncodingPreset(RenderSettings.EncodingPreset.PNG));
    }

    @Override
    protected Path getSettingsPath() {
        return getMinecraft().gameDirectory.toPath().resolve("config/scenecraft-screenshotsettings.json");
    }
}