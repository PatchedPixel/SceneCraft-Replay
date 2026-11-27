package net.patchedpixel.scenecraft.common.render.gui;

import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.render.RenderSettings;
import net.patchedpixel.scenecraft.common.render.FFmpegWriter;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;

import java.util.function.Consumer;

import static net.patchedpixel.scenecraft.common.render.SceneCraftRender.LOGGER;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.flamex.components.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.widget.TextListWidget;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

public class GuiExportFailed extends FlameableScreen {
    public static GuiExportFailed tryToRecover(FFmpegWriter.FFmpegStartupException e, Consumer<RenderSettings> doRestart) {
        // Always log the error first
        LOGGER.error("Rendering video:", e);

        RenderSettings settings = e.getSettings();
        // Check whether the user has configured some custom ffmpeg arguments
        if(settings.getEncodingPreset().getValue().equals(settings.getExportArguments())) {
            // If they haven't, then this is probably a faulty ffmpeg installation and there's nothing we can do
            CrashReport crashReport = CrashReport.forThrowable(e, "Exporting video");
            CrashReportCategory details = crashReport.addCategory("Export details");
            MCVer.addDetail(details, "Settings", settings::toString);
            MCVer.addDetail(details, "FFmpeg log", e::getLog);
            throw new ReportedException(crashReport);
        } else {
            // If they have, ask them whether it was intentional
            GuiExportFailed gui = new GuiExportFailed(e, doRestart);
            gui.display();
            return gui;
        }
    }

    private final FFmpegWriter.FFmpegStartupException exception;
    private final Consumer<RenderSettings> doRestart;
    private TextListWidget logList;

    public GuiExportFailed(FFmpegWriter.FFmpegStartupException e, Consumer<RenderSettings> doRestart) {
        super("scenecraft.gui.rendering.error.title", null);
        this.exception = e;
        this.doRestart = doRestart;
        this.background = Background.DIRT;
    }

    private void restart() {
        FFmpegWriter.FFmpegStartupException e = exception;
        SceneCraft.instance.runLater(() -> {
            RenderSettings oldSettings = e.getSettings();
            doRestart.accept(new RenderSettings(
                    oldSettings.getRenderMethod(),
                    oldSettings.getEncodingPreset(),
                    oldSettings.getVideoWidth(),
                    oldSettings.getVideoHeight(),
                    oldSettings.getFramesPerSecond(),
                    oldSettings.getBitRate(),
                    oldSettings.getOutputFile(),
                    oldSettings.isRenderNameTags(),
                    oldSettings.isIncludeAlphaChannel(),
                    oldSettings.isStabilizeYaw(),
                    oldSettings.isStabilizePitch(),
                    oldSettings.isStabilizeRoll(),
                    oldSettings.getChromaKeyingColor(),
                    oldSettings.getSphericalFovX(),
                    oldSettings.getSphericalFovY(),
                    oldSettings.isInjectSphericalMetadata(),
                    oldSettings.isDepthMap(),
                    oldSettings.isCameraPathExport(),
                    oldSettings.getAntiAliasing(),
                    oldSettings.getExportCommand(),
                    oldSettings.getEncodingPreset().getValue(),
                    oldSettings.isHighPerformance()
            ));
        });
    }

    @Override
    protected void build(LinearLayout root) {}

    @Override
    protected void init() {
        // Info block and buttons centered in the upper half, ffmpeg log below
        LinearLayout info = FlameX.column(4);
        info.addChild(FlameX.label("scenecraft.gui.rendering.error.ffmpegargs.1"));
        info.addChild(FlameX.label("scenecraft.gui.rendering.error.ffmpegargs.2"));
        info.addChild(FlameX.label(Component.empty()));
        LinearLayout buttons = FlameX.row(5);
        buttons.addChild(FlameX.button("gui.yes", 100, this::restart));
        buttons.addChild(FlameX.button("gui.no", 100, () -> MCVer.getMinecraft().setScreen(null)));
        info.addChild(buttons);
        info.visitWidgets(this::addRenderableWidget);
        info.arrangeElements();
        info.setPosition(width / 2 - info.getWidth() / 2, (height / 2 - info.getHeight() - 30) / 2 + 30);

        var logLabel = FlameX.label("scenecraft.gui.rendering.error.ffmpeglog");
        addRenderableWidget(logLabel);
        logLabel.setPosition(width / 2 - logLabel.getWidth() / 2, height / 2 + 4);
        int top = logLabel.getY() + logLabel.getHeight() + 4;
        logList = new TextListWidget(10, top, width - 20, height - 10 - top).setShadow(true);
        for(String line : exception.getLog().replace("\t", "    ").split("\n")) {
            logList.addLine(MCVer.literalText(line));
        }
        addRenderableWidget(logList);
    }
}
