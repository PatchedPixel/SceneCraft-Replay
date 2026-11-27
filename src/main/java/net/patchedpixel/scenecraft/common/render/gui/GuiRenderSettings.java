package net.patchedpixel.scenecraft.common.render.gui;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.render.FFmpegWriter;
import net.patchedpixel.scenecraft.common.render.RenderSettings;
import net.patchedpixel.scenecraft.common.render.SceneCraftRender;
import net.patchedpixel.scenecraft.common.render.rendering.VideoRenderer;
import net.patchedpixel.scenecraft.common.replay.ReplayHandler;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.path.Timeline;
import net.patchedpixel.scenecraft.common.flamex.FlameXTheme;
import net.patchedpixel.scenecraft.common.flamex.screen.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.util.color.Color;
import net.patchedpixel.scenecraft.common.flamex.widget.ColorPickerWidget;
import net.patchedpixel.scenecraft.common.flamex.widget.DropdownWidget;
import net.patchedpixel.scenecraft.common.flamex.widget.ScrollView;
import net.patchedpixel.scenecraft.common.flamex.widget.SelectableList;
import net.patchedpixel.scenecraft.common.flamex.widget.StepSlider;
import net.patchedpixel.scenecraft.common.flamex.widget.WrappedLabelWidget;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import static net.patchedpixel.scenecraft.common.render.SceneCraftRender.LOGGER;
import static net.patchedpixel.scenecraft.common.utils.Utils.error;

public class GuiRenderSettings extends FlameableScreen {
    public static final int MIN_SPHERICAL_FOV = 120;
    public static final int MAX_SPHERICAL_FOV = 360;
    public static final int SPHERICAL_FOV_STEP_SIZE = 5;

    private RenderSettings.RenderMethod renderMethod = RenderSettings.RenderMethod.DEFAULT;
    private RenderSettings.EncodingPreset encodingPreset;
    private int videoWidth = 1920, videoHeight = 1080;
    private int frameRate = 60;
    private int bitRateValue = 10, bitRateUnit = 2;
    private File outputFile;
    private boolean userDefinedOutputFileName;
    private boolean nametags, alpha, stabYaw, stabPitch, stabRoll;
    private boolean chromaOn;
    private Color chromaColor = new Color(0, 255, 0);
    private int sphericalFovStep;
    private boolean injectSphericalMetadata, depthMap, cameraPathExport;
    private RenderSettings.AntiAliasing antiAliasing = RenderSettings.AntiAliasing.NONE;
    private String exportCommand = "", exportArguments = "";

    private static final int VIDEO_COLUMN = 225;

    private ScrollView scrollView;
    private LinearLayout buttons;

    private DropdownWidget<RenderSettings.RenderMethod> renderMethodButton;
    private DropdownWidget<RenderSettings.EncodingPreset> presetButton;
    private EditBox widthBox, heightBox, bitRateBox, commandBox, argumentsBox;
    private DropdownWidget<String> bitRateUnitButton;
    private StepSlider frameRateSlider, sphericalFovSlider;
    private Button outputFileButton, resetButton;
    private Checkbox yawButton, pitchButton, rollButton, metadataButton, depthButton;
    private ColorPickerWidget chromaPicker;
    private DropdownWidget<RenderSettings.AntiAliasing> aaButton;
    protected Button queueButton, renderButton, cancelButton;

    private final ReplayHandler replayHandler;
    private final Timeline timeline;

    private boolean showRenderButton = true;
    private String queueButtonKey = "scenecraft.gui.rendersettings.addtoqueue";
    private boolean showQueueButton = true;
    private Consumer<Boolean> queueAction = ctrl -> {};
    private Runnable closeHook = () -> {};
    protected Consumer<Boolean> renderAction = this::defaultRender;

    public GuiRenderSettings(Screen parent, ReplayHandler replayHandler, Timeline timeline) {
        super("scenecraft.gui.rendersettings.video", parent, 6);
        this.background = Background.DIRT;
        this.replayHandler = replayHandler;
        this.timeline = timeline;

        String json = "{}";
        try {
            json = new String(Files.readAllBytes(getSettingsPath()), StandardCharsets.UTF_8);
        } catch(NoSuchFileException | FileNotFoundException ignored) {
        } catch(IOException e) {
            LOGGER.error("Reading render settings:", e);
        }
        RenderSettings settings = null;
        try {
            settings = new Gson().fromJson(json, RenderSettings.class);
        } catch(JsonSyntaxException e) {
            LOGGER.error("Parsing render settings:", e);
            LOGGER.error("Raw JSON: {}", json);
        }
        if(settings == null) {
            settings = new RenderSettings();
        }
        load(settings);
    }

    public GuiRenderSettings hideRenderButton() {
        showRenderButton = false;
        return this;
    }

    public GuiRenderSettings setQueueButton(String labelKey, Consumer<Boolean> action) {
        this.queueButtonKey = labelKey;
        this.queueAction = action;
        return this;
    }

    public GuiRenderSettings hideQueueButton() {
        showQueueButton = false;
        return this;
    }

    public GuiRenderSettings setCloseHook(Runnable hook) {
        this.closeHook = hook;
        return this;
    }

    protected void setRenderAction(Consumer<Boolean> action) {
        this.renderAction = action;
    }

    public ReplayHandler getReplayHandler() {
        return replayHandler;
    }

    @Override
    public Minecraft getMinecraft() {
        return MCVer.getMinecraft();
    }

    protected boolean isScreenshotMode() {
        return false;
    }

    private void clearWidgetRefs() {
        renderMethodButton = null;
        presetButton = null;
        widthBox = heightBox = bitRateBox = commandBox = argumentsBox = null;
        chromaPicker = null;
        bitRateUnitButton = null;
        frameRateSlider = sphericalFovSlider = null;
        outputFileButton = resetButton = null;
        yawButton = pitchButton = rollButton = metadataButton = depthButton = null;
        aaButton = null;
        queueButton = renderButton = cancelButton = null;
    }

    @Override
    protected void build(LinearLayout root) {
        clearWidgetRefs();
        boolean shot = isScreenshotMode();
        this.background = Background.NONE;
        this.drawTitle = false;

        List<GridLayout> sections = new ArrayList<>();
        List<StringWidget> titles = new ArrayList<>();
        LinearLayout content = FlameX.column(10);

        titles.add(FlameX.label(shot ? "scenecraft.gui.advancedscreenshots.title" : "scenecraft.gui.rendersettings.video"));
        GridLayout video = sectionGrid();
        buildVideo(video, shot);
        sections.add(video);

        titles.add(FlameX.label("scenecraft.gui.rendersettings.advanced"));
        LinearLayout advanced = FlameX.column(15);
        GridLayout advancedGrid = new GridLayout();
        advancedGrid.columnSpacing(5).rowSpacing(15);
        buildAdvanced(advanced, advancedGrid, shot);
        advanced.addChild(advancedGrid, advanced.newChildLayoutSettings().alignHorizontallyLeft());

        LinearLayout command = null;
        if(!shot) {
            titles.add(FlameX.label("scenecraft.gui.rendersettings.commandline"));
            command = buildCommand();
        }

        video.arrangeElements();
        advanced.arrangeElements();
        int sectionWidth = Math.max(video.getWidth(), advanced.getWidth());
        if(command != null) {
            command.arrangeElements();
            sectionWidth = Math.max(sectionWidth, command.getWidth());
        }
        for(StringWidget title : titles) title.setWidth(sectionWidth);

        content.addChild(titles.get(0));
        content.addChild(video, content.newChildLayoutSettings().alignHorizontallyLeft());
        content.addChild(new SpacerElement(0, 0));
        content.addChild(titles.get(1));
        content.addChild(advanced, content.newChildLayoutSettings().alignHorizontallyLeft());
        if(command != null) {
            content.addChild(new SpacerElement(0, 0));
            content.addChild(titles.get(2));
            content.addChild(command, content.newChildLayoutSettings().alignHorizontallyLeft());
        }

        scrollView = new ScrollView(content, Math.max(100, width - 40), Math.max(60, height - 85));
        root.addChild(scrollView);

        buttons = FlameX.row(4);
        if(showQueueButton && !shot) {
            queueButton = FlameX.button(queueButtonKey, 100, () -> queueAction.accept(Screen.hasControlDown()));
            buttons.addChild(queueButton);
        }
        if(showRenderButton) {
            String key = shot ? "scenecraft.gui.advancedscreenshots.create" : "scenecraft.gui.render";
            renderButton = FlameX.button(key, 100, () -> renderAction.accept(Screen.hasControlDown()));
            buttons.addChild(renderButton);
        }
        cancelButton = FlameX.button("scenecraft.gui.cancel", 100, this::close);
        buttons.addChild(cancelButton);
        root.addChild(buttons);

        updateInputs();
    }

    private static GridLayout sectionGrid() {
        GridLayout grid = new GridLayout();
        grid.columnSpacing(5).rowSpacing(5);
        return grid;
    }

    @Override
    protected void afterLayout() {
        scrollView.setPosition(20, 25);
        buttons.setPosition(width / 2 - buttons.getWidth() / 2, 25 + scrollView.getHeight() + 10);
        scrollView.place();
        for(AbstractWidget w : scrollView.widgets()) addWidget(w);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if(parent != null) {
            parent.render(graphics, -1, -1, partialTicks);
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 10);
        graphics.fill(20, 20, width - 20, height - 20, FlameXTheme.Overlay.SCRIM);
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return guarded(mouseX, mouseY, () -> super.mouseClicked(mouseX, mouseY, button));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return guarded(mouseX, mouseY, () -> super.mouseReleased(mouseX, mouseY, button));
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        return guarded(mouseX, mouseY, () -> super.mouseDragged(mouseX, mouseY, button, dx, dy));
    }

    private boolean guarded(double mouseX, double mouseY, java.util.function.BooleanSupplier action) {
        boolean block = !scrollView.contains(mouseX, mouseY) && !scrollView.isDragging();
        if(block) scrollView.setInputEnabled(false);
        try {
            return action.getAsBoolean();
        } finally {
            if(block) scrollView.setInputEnabled(true);
        }
    }

    private void row(GridLayout grid, int row, String labelKey, LayoutElement widget) {
        grid.addChild(FlameX.label(labelKey), row, 0, grid.newCellSettings().alignHorizontallyRight().alignVerticallyMiddle());
        grid.addChild(widget, row, 1, grid.newCellSettings().alignVerticallyMiddle());
    }

    private void buildVideo(GridLayout grid, boolean shot) {
        int r = 0;
        List<RenderSettings.RenderMethod> methods = List.of(RenderSettings.RenderMethod.getSupported());
        renderMethodButton = FlameX.dropdown(methods, renderMethod, RenderSettings.RenderMethod::toString, VIDEO_COLUMN, v -> {
            renderMethod = v;
            updateInputs();
        });
        renderMethodButton.setEntryTooltip(RenderSettings.RenderMethod::getDescription);
        row(grid, r++, "scenecraft.gui.rendersettings.renderer", renderMethodButton);

        if(!shot) {
            List<RenderSettings.EncodingPreset> presets = List.of(RenderSettings.EncodingPreset.getSupported());
            presetButton = FlameX.dropdown(presets, encodingPreset, RenderSettings.EncodingPreset::toString, VIDEO_COLUMN, v -> {
                encodingPreset = v;
                // Update export arguments to match new Preset
                exportArguments = v.getValue();
                if(argumentsBox != null) argumentsBox.setValue(exportArguments);
                // Update output file ending
                if(outputFile != null) {
                    outputFile = conformExtension(outputFile, v);
                    outputFileButton.setMessage(MCVer.literalText(outputFile.getName()));
                }
                updateInputs();
            });
            row(grid, r++, "scenecraft.gui.rendersettings.presets", presetButton);
        }

        widthBox = FlameX.integerField(50, videoWidth, v -> {
            videoWidth = v;
            updateInputs();
        });
        widthBox.setWidth(50);
        heightBox = FlameX.integerField(50, videoHeight, v -> {
            videoHeight = v;
            updateInputs();
        });
        heightBox.setWidth(50);
        LinearLayout res = FlameX.row(2);
        StringWidget times = FlameX.label(MCVer.literalText("*"));
        int used = 50 + 2 + times.getWidth() + 2 + 50;
        res.addChild(new SpacerElement(Math.max(0, VIDEO_COLUMN - used), 1));
        res.addChild(widthBox);
        res.addChild(times);
        res.addChild(heightBox);
        row(grid, r++, shot ? "scenecraft.gui.advancedscreenshots.resolution" : "scenecraft.gui.rendersettings.customresolution", res);

        if(!shot) {
            String[] units = {"bps", "kbps", "mbps"};
            LinearLayout bitrate = FlameX.row(3);
            LinearLayout rate = FlameX.row(0);
            bitRateBox = FlameX.integerField(50, bitRateValue, v -> bitRateValue = v);
            bitRateBox.setWidth(50);
            bitRateUnitButton = FlameX.dropdown(List.of(units), units[bitRateUnit], u -> u, 50,
                    v -> bitRateUnit = List.of(units).indexOf(v));
            rate.addChild(bitRateBox);
            rate.addChild(bitRateUnitButton);
            frameRateSlider = new StepSlider(122, 20, 110).setText(v ->
                            MCVer.literalText(I18n.get("scenecraft.gui.rendersettings.framerate") + ": " + (v + 10)))
                    .onValueChanged(() -> {});
            frameRateSlider.setValue(frameRate - 10);
            frameRateSlider.onValueChanged(() -> frameRate = frameRateSlider.getValue() + 10);
            bitrate.addChild(rate);
            bitrate.addChild(frameRateSlider);
            row(grid, r++, "scenecraft.gui.rendersettings.bitrate", bitrate);
        }

        outputFileButton = Button.builder(MCVer.literalText(outputFile.getName()), b ->
                getMinecraft().setScreen(new OutputFileScreen())).width(VIDEO_COLUMN).build();
        row(grid, r, "scenecraft.gui.rendersettings.outputfile", outputFileButton);
    }

    private void buildAdvanced(LinearLayout advanced, GridLayout grid, boolean shot) {
        LayoutSettings left = grid.newCellSettings().alignHorizontallyLeft().alignVerticallyMiddle();
        advanced.addChild(FlameX.checkbox("scenecraft.gui.rendersettings.nametags", nametags, v -> nametags = v),
                advanced.newChildLayoutSettings().alignHorizontallyLeft());
        advanced.addChild(FlameX.checkbox("scenecraft.gui.rendersettings.includealpha", alpha, v -> alpha = v),
                advanced.newChildLayoutSettings().alignHorizontallyLeft());

        int r = 0;
        LinearLayout stab = FlameX.row(10);
        yawButton = FlameX.checkbox("scenecraft.gui.yaw", stabYaw, v -> stabYaw = v);
        pitchButton = FlameX.checkbox("scenecraft.gui.pitch", stabPitch, v -> stabPitch = v);
        rollButton = FlameX.checkbox("scenecraft.gui.roll", stabRoll, v -> stabRoll = v);
        stab.addChild(yawButton);
        stab.addChild(pitchButton);
        stab.addChild(rollButton);
        grid.addChild(FlameX.label("scenecraft.gui.rendersettings.stabilizecamera"), r, 0, left);
        grid.addChild(stab, r++, 1, left);

        chromaPicker = new ColorPickerWidget(30, 15, chromaColor, c -> chromaColor = c);
        grid.addChild(FlameX.checkbox("scenecraft.gui.rendersettings.chromakey", chromaOn, v -> chromaOn = v), r, 0, left);
        grid.addChild(chromaPicker, r++, 1, left);

        if(!shot) {
            metadataButton = FlameX.checkbox("scenecraft.gui.rendersettings.sphericalmetadata",
                    injectSphericalMetadata, v -> injectSphericalMetadata = v);
            grid.addChild(metadataButton, r, 0, left);

            sphericalFovSlider = new StepSlider(200, 20, (MAX_SPHERICAL_FOV - MIN_SPHERICAL_FOV) / SPHERICAL_FOV_STEP_SIZE)
                    .setText(v -> MCVer.literalText(I18n.get("scenecraft.gui.rendersettings.sphericalFov")
                            + ": " + (MIN_SPHERICAL_FOV + v * SPHERICAL_FOV_STEP_SIZE) + "\u00b0"));
            sphericalFovSlider.setValue(sphericalFovStep);
            sphericalFovSlider.onValueChanged(() -> {
                sphericalFovStep = sphericalFovSlider.getValue();
                updateInputs();
            });
            grid.addChild(sphericalFovSlider, r++, 1, left);

            depthButton = FlameX.checkbox("scenecraft.gui.rendersettings.depthmap", depthMap, v -> depthMap = v);
            grid.addChild(depthButton, r++, 0, left);
            grid.addChild(FlameX.checkbox("scenecraft.gui.rendersettings.camerapath", cameraPathExport,
                    v -> cameraPathExport = v), r++, 0, left);
        }

        aaButton = FlameX.dropdown(List.of(RenderSettings.AntiAliasing.values()), antiAliasing,
                RenderSettings.AntiAliasing::toString, 200, v -> antiAliasing = v);
        grid.addChild(FlameX.label("scenecraft.gui.rendersettings.antialiasing"), r, 0, left);
        grid.addChild(aaButton, r, 1, left);
    }

    private LinearLayout buildCommand() {
        LinearLayout command = FlameX.column(10);
        commandBox = FlameX.textField(55, exportCommand, null, s -> {
            exportCommand = s;
            updateInputs();
        });
        commandBox.setWidth(55);
        commandBox.setMaxLength(100);
        commandBox.setHint(Component.translatable("scenecraft.gui.rendersettings.command"));
        argumentsBox = FlameX.textField(245, exportArguments, null, s -> {
            exportArguments = s;
            updateInputs();
        });
        argumentsBox.setWidth(245);
        argumentsBox.setMaxLength(500);
        argumentsBox.setHint(Component.translatable("scenecraft.gui.rendersettings.arguments"));
        resetButton = Button.builder(MCVer.literalText("X"), b -> {
            exportCommand = "";
            exportArguments = encodingPreset.getValue();
            commandBox.setValue(exportCommand);
            argumentsBox.setValue(exportArguments);
            updateInputs();
        }).width(20).build();

        LinearLayout fields = FlameX.row(5);
        fields.addChild(commandBox);
        fields.addChild(argumentsBox);
        fields.addChild(resetButton);
        command.addChild(fields, command.newChildLayoutSettings().alignHorizontallyLeft());
        command.addChild(new WrappedLabelWidget(Component.translatable("scenecraft.gui.rendersettings.ffmpeg.description"),
                330, 50, 0xFFFFFF), command.newChildLayoutSettings().alignHorizontallyLeft());
        return command;
    }

    private static void setTip(net.minecraft.client.gui.components.AbstractWidget w, Component tip) {
        if(w != null) w.setTooltip(tip == null ? null : Tooltip.create(tip));
    }

    protected void updateInputs() {
        // Validate video width and height
        String resolutionError = updateResolution();
        Component resolutionTip = resolutionError == null ? null : Component.translatable(resolutionError);

        if(widthBox != null) {
            widthBox.setEditable(!renderMethod.hasFixedAspectRatio());
            int color = resolutionError == null ? 0xE0E0E0 : 0xFF5555;
            widthBox.setTextColor(color);
            heightBox.setTextColor(color);
        }

        if(queueButton != null) {
            queueButton.active = resolutionError == null;
            setTip(queueButton, resolutionTip);
        }

        if(renderButton != null) {
            String[] compatError = VideoRenderer.checkCompat(save(false, false));
            if(resolutionError != null) {
                renderButton.active = false;
                setTip(renderButton, resolutionTip);
            } else if(compatError != null) {
                renderButton.active = false;
                setTip(renderButton, MCVer.literalText(String.join("\n", compatError)));
            } else {
                renderButton.active = true;
                setTip(renderButton, null);
            }
        }

        // Enable/Disable bitrate input field and dropdown
        boolean hasBitrate = encodingPreset.hasBitrateSetting();
        if(bitRateBox != null) {
            bitRateBox.setEditable(hasBitrate);
            bitRateUnitButton.active = hasBitrate;
        }

        // Enable/Disable camera stabilization toggles
        boolean stabilizable = switch(renderMethod) {
            case CUBIC, EQUIRECTANGULAR, ODS -> true;
            default -> false;
        };
        if(yawButton != null) {
            yawButton.active = stabilizable;
            pitchButton.active = stabilizable;
            rollButton.active = stabilizable;
        }

        // Spherical FOV slider
        if(sphericalFovSlider != null) {
            sphericalFovSlider.active = renderMethod.isSpherical();
        }

        // Inject metadata toggle
        if(metadataButton != null) {
            if(encodingPreset.getFileExtension().equals("mp4") && renderMethod.isSpherical()) {
                metadataButton.active = true;
                setTip(metadataButton, null);
            } else {
                metadataButton.active = false;
                setTip(metadataButton, Component.translatable("scenecraft.gui.rendersettings.sphericalmetadata.error")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }

        boolean isEXR = encodingPreset == RenderSettings.EncodingPreset.EXR;
        boolean isPNG = encodingPreset == RenderSettings.EncodingPreset.PNG;
        boolean isFFmpeg = !isEXR && !isPNG;
        if(commandBox != null) {
            commandBox.setEditable(isFFmpeg);
            argumentsBox.setEditable(isFFmpeg);
            boolean commandChanged = !exportCommand.isEmpty();
            boolean argsChanged = !encodingPreset.getValue().equals(exportArguments);
            resetButton.active = commandChanged || argsChanged;
        }
        if(aaButton != null) {
            aaButton.active = isFFmpeg;
        }
        if(depthButton != null) {
            if(isEXR || isPNG) {
                depthButton.active = true;
                setTip(depthButton, null);
            } else {
                depthButton.active = false;
                setTip(depthButton, Component.translatable("scenecraft.gui.rendersettings.depthmap.only_exr_or_png")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            }
        }
    }

    protected String updateResolution() {
        RenderSettings.EncodingPreset preset = encodingPreset;
        RenderSettings.RenderMethod method = renderMethod;

        int videoHeight = this.videoHeight;
        int videoWidth;
        if(method.hasFixedAspectRatio()) {
            // cubic rendering requires an aspect ratio of 4:3,
            // therefore the height must be divisible by 3
            if(method == RenderSettings.RenderMethod.CUBIC && videoHeight % 3 != 0) {
                return "scenecraft.gui.rendersettings.customresolution.warning.cubic.height";
            }

            int sphericalFov = MIN_SPHERICAL_FOV + sphericalFovStep * SPHERICAL_FOV_STEP_SIZE;
            videoWidth = videoWidthForHeight(method, videoHeight, sphericalFov, sphericalFov);

            this.videoWidth = videoWidth;
            if(widthBox != null && !widthBox.getValue().equals(String.valueOf(videoWidth))) {
                widthBox.setValue(String.valueOf(videoWidth));
            }
        } else {
            videoWidth = this.videoWidth;
        }

        // Make sure the export arguments haven't been changed manually
        if(exportArguments.equals(preset.getValue())) {
            // Yuv420 requires both dimensions to be even
            if(preset.isYuv420()
                    && (videoWidth % 2 != 0 || videoHeight % 2 != 0)) {

                if(method == RenderSettings.RenderMethod.CUBIC) {
                    // cubic yuv rendering has the special case that the height must be
                    // divisible by both 3 and 2 - tell the user about it with a special message
                    return "scenecraft.gui.rendersettings.customresolution.warning.yuv420.cubic";
                }

                return "scenecraft.gui.rendersettings.customresolution.warning.yuv420";
            }
        }

        return null;
    }

    protected int videoWidthForHeight(RenderSettings.RenderMethod method, int height,
                                      int sphericalFovX, int sphericalFovY) {
        if(method.isSpherical()) {
            if(sphericalFovY < 180) {
                // calculate the non-cropped height of the video
                height = Math.round(height * 180 / (float) sphericalFovY);
            }

            int width = height * 2;

            if(sphericalFovX < 360) {
                // crop the resulting width
                width = Math.round(width * (float) sphericalFovX / 360);
            }

            if(method == RenderSettings.RenderMethod.ODS) {
                width = Math.round(width / 2f);
            }

            return width;

        } else if(method == RenderSettings.RenderMethod.CUBIC) {
            Preconditions.checkArgument(height % 3 == 0);
            return height / 3 * 4;
        }

        throw new IllegalArgumentException();
    }

    public void load(RenderSettings settings) {
        if(settings.getRenderMethod().isSupported()) {
            renderMethod = settings.getRenderMethod();
        }
        RenderSettings.EncodingPreset preset = settings.getEncodingPreset();
        /* encodingPreset can be null from a previously supported and later removed preset */
        boolean invalidEncodingPreset = preset == null || !preset.isSupported();
        if(invalidEncodingPreset) {
            preset = new RenderSettings().getEncodingPreset();
        }
        encodingPreset = preset;
        videoWidth = settings.getTargetVideoWidth();
        videoHeight = settings.getTargetVideoHeight();
        frameRate = settings.getFramesPerSecond();
        if(settings.getBitRate() % (1 << 20) == 0) {
            bitRateValue = settings.getBitRate() >> 20;
            bitRateUnit = 2;
        } else if(settings.getBitRate() % (1 << 10) == 0) {
            bitRateValue = settings.getBitRate() >> 10;
            bitRateUnit = 1;
        } else {
            bitRateValue = settings.getBitRate();
            bitRateUnit = 0;
        }
        File savedOutputFile = settings.getOutputFile();
        if(savedOutputFile == null || !getParentFile(savedOutputFile).exists()) {
            this.outputFile = generateOutputFile(preset);
            userDefinedOutputFileName = false;
        } else if(savedOutputFile.exists()) {
            String name = generateOutputFile(preset).getName();
            boolean isFolder = savedOutputFile.isDirectory() && !savedOutputFile.getName().endsWith(".exr");
            this.outputFile = new File(isFolder ? savedOutputFile : getParentFile(savedOutputFile), name);
            userDefinedOutputFileName = false;
        } else {
            this.outputFile = conformExtension(savedOutputFile, preset);
            userDefinedOutputFileName = true;
        }
        nametags = settings.isRenderNameTags();
        alpha = settings.isIncludeAlphaChannel();
        stabYaw = settings.isStabilizeYaw();
        stabPitch = settings.isStabilizePitch();
        stabRoll = settings.isStabilizeRoll();
        if(settings.getChromaKeyingColor() == null) {
            chromaOn = false;
            chromaColor = new Color(0, 255, 0);
        } else {
            chromaOn = true;
            chromaColor = settings.getChromaKeyingColor();
        }
        sphericalFovStep = (settings.getSphericalFovX() - MIN_SPHERICAL_FOV) / SPHERICAL_FOV_STEP_SIZE;
        injectSphericalMetadata = settings.isInjectSphericalMetadata();
        depthMap = settings.isDepthMap();
        cameraPathExport = settings.isCameraPathExport();
        antiAliasing = settings.getAntiAliasing();
        exportCommand = settings.getExportCommand() == null ? "" : settings.getExportCommand();
        String args = settings.getExportArguments();
        if(args == null || settings.getEncodingPreset() == null || invalidEncodingPreset) {
            // backwards compat, see RenderSettings#exportArguments
            args = preset.getValue();
        }
        exportArguments = args;

        if(minecraft != null && width > 0) {
            rebuildWidgets();
        }
    }

    public RenderSettings save(boolean serialize, boolean highPerformance) {
        int sphericalFov = MIN_SPHERICAL_FOV + sphericalFovStep * SPHERICAL_FOV_STEP_SIZE;
        boolean stabilizable = switch(renderMethod) {
            case CUBIC, EQUIRECTANGULAR, ODS -> true;
            default -> false;
        };
        boolean metaEnabled = encodingPreset.getFileExtension().equals("mp4") && renderMethod.isSpherical();
        boolean depthEnabled = encodingPreset == RenderSettings.EncodingPreset.EXR
                || encodingPreset == RenderSettings.EncodingPreset.PNG;
        boolean aaEnabled = depthEnabled ? false : true;

        return new RenderSettings(
                renderMethod,
                encodingPreset,
                videoWidth,
                videoHeight,
                frameRate,
                bitRateValue << (10 * bitRateUnit),
                serialize && !userDefinedOutputFileName ? getParentFile(outputFile) : outputFile,
                nametags,
                alpha,
                stabYaw && (serialize || stabilizable),
                stabPitch && (serialize || stabilizable),
                stabRoll && (serialize || stabilizable),
                chromaOn ? chromaColor : null,
                sphericalFov, Math.min(180, sphericalFov),
                injectSphericalMetadata && (serialize || metaEnabled),
                depthMap && (serialize || depthEnabled),
                cameraPathExport,
                serialize || aaEnabled ? antiAliasing : RenderSettings.AntiAliasing.NONE,
                exportCommand,
                exportArguments,
                highPerformance
        );
    }

    protected File generateOutputFile(RenderSettings.EncodingPreset encodingPreset) {
        String fileName = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
        File folder = SceneCraftRender.instance.getVideoFolder();
        return new File(folder, fileName + "." + encodingPreset.getFileExtension());
    }

    public void setOutputFileBaseName(String base) {
        File file = new File(getParentFile(outputFile), base + "." + encodingPreset.getFileExtension());
        // Ensure the file name is valid
        try {
            //noinspection ResultOfMethodCallIgnored
            file.toPath();
            outputFile = file;
            if(outputFileButton != null) {
                outputFileButton.setMessage(MCVer.literalText(file.getName()));
            }
        } catch(InvalidPathException ignored) {
            setOutputFileBaseName("filename_invalid_" + base.hashCode());
        }
    }

    protected File conformExtension(File file, RenderSettings.EncodingPreset preset) {
        String name = file.getName();
        if(name.contains(".")) {
            name = name.substring(0, name.lastIndexOf('.'));
        }
        return new File(getParentFile(file), name + "." + preset.getFileExtension());
    }

    protected Path getSettingsPath() {
        return SceneCraftRender.instance.getRenderSettingsPath();
    }

    public void saveSettings() {
        RenderSettings settings = save(true, false);
        String json = new Gson().toJson(settings);
        try {
            Files.write(getSettingsPath(), json.getBytes(StandardCharsets.UTF_8));
        } catch(IOException e) {
            LOGGER.error("Saving render settings:", e);
        }
    }

    public void open() {
        display();
    }

    @Override
    public void close() {
        saveSettings();
        super.close();
        closeHook.run();
    }

    private void defaultRender(boolean highPerformance) {
        SceneCraft.instance.runLaterWithoutLock(() -> {
            // Closing this GUI ensures that settings are saved
            close();
            try {
                VideoRenderer videoRenderer = new VideoRenderer(save(false, highPerformance), replayHandler, timeline);
                videoRenderer.renderVideo();
            } catch(FFmpegWriter.NoFFmpegException e) {
                LOGGER.error("Rendering video:", e);
                getMinecraft().setScreen(new GuiNoFfmpeg(this::display));
            } catch(FFmpegWriter.FFmpegStartupException e) {
                GuiExportFailed.tryToRecover(e, newSettings -> {
                    // Update settings with fixed ffmpeg arguments
                    exportArguments = newSettings.getExportArguments();
                    // Restart rendering, this will also save the changed ffmpeg arguments
                    defaultRender(highPerformance);
                });
            } catch(Throwable t) {
                error(LOGGER, CrashReport.forThrowable(t, "Rendering video"), () -> {});
                display(); // Re-show the render settings gui and the new error popup
            }
        });
    }

    private static File getParentFile(File file) {
        File parent = file.getParentFile();
        // parent this can be null if file is just a name (i.e. in CWD)
        return parent == null ? new File(".") : parent;
    }

    private class OutputFileScreen extends FlameableScreen {
        private final java.util.Set<File> selected = new java.util.HashSet<>();
        private final String extension;
        private File dir;
        private EditBox nameBox;
        private Button saveButton;
        private Button upButton;
        private StringWidget pathLabel;
        private SelectableList<File> list;

        OutputFileScreen() {
            super("scenecraft.gui.save", GuiRenderSettings.this, 5);
            String name = outputFile.getName();
            extension = name.contains(".") ? name.substring(name.lastIndexOf('.')) : "";
            dir = getParentFile(outputFile).getAbsoluteFile();
            panel = true;
            dim = true;
        }

        @Override
        protected void build(LinearLayout root) {
            pathLabel = FlameX.label(Component.empty());
            root.addChild(pathLabel);
            list = new SelectableList<>(300, 130, 280, 16, selected, f -> f.isDirectory() ? f.getName() + "/" : f.getName(), (f, ctrl) -> {
                if(f.isDirectory()) {
                    navigate(f);
                } else {
                    selected.clear();
                    selected.add(f);
                    nameBox.setValue(f.getName());
                }
            });
            root.addChild(list);
            LinearLayout bottom = FlameX.row(4);
            upButton = FlameX.button(Component.literal(".."), 20, () -> {
                File parent = dir.getParentFile();
                if(parent != null) navigate(parent);
            });
            bottom.addChild(upButton);
            nameBox = FlameX.textField(180, outputFile.getName(), null, s -> update());
            nameBox.setWidth(180);
            bottom.addChild(nameBox);
            saveButton = FlameX.button("scenecraft.gui.save", 45, this::accept);
            bottom.addChild(saveButton);
            bottom.addChild(FlameX.button("scenecraft.gui.cancel", 45, this::close));
            root.addChild(bottom);
            populate();
            update();
        }

        private void navigate(File target) {
            dir = target.getAbsoluteFile();
            selected.clear();
            populate();
            update();
        }

        private void populate() {
            list.items().clear();
            File[] files = dir.listFiles();
            if(files != null) {
                java.util.Arrays.sort(files, (a, b) -> a.isDirectory() != b.isDirectory()
                        ? (a.isDirectory() ? -1 : 1) : a.getName().compareToIgnoreCase(b.getName()));
                for(File f : files) {
                    if(f.isHidden()) continue;
                    if(f.isDirectory() || extension.isEmpty() || f.getName().toLowerCase().endsWith(extension.toLowerCase())) {
                        list.add(f);
                    }
                }
            }
            String path = dir.getPath();
            Font font = MCVer.getFontRenderer();
            while(path.length() > 4 && font.width(path) > 296) path = path.substring(1);
            pathLabel.setMessage(Component.literal(path));
            pathLabel.setWidth(300);
            if(upButton != null) upButton.active = dir.getParentFile() != null;
        }

        private void update() {
            if(saveButton == null) return;
            boolean valid = !nameBox.getValue().isEmpty();
            try {
                new File(dir, nameBox.getValue()).toPath();
            } catch(InvalidPathException e) {
                valid = false;
            }
            saveButton.active = valid;
        }

        private void accept() {
            File file = new File(dir, nameBox.getValue());
            if(!file.getName().equals(outputFile.getName())) {
                userDefinedOutputFileName = true;
            }
            outputFile = file;
            close();
        }

        @Override
        public void close() {
            getMinecraft().setScreen(GuiRenderSettings.this);
        }
    }
}
