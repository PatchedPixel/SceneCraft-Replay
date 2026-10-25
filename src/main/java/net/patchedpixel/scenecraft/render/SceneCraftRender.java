package net.patchedpixel.scenecraft.render;

import net.patchedpixel.scenecraft.core.Module;
import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.platform.Utils;
import net.patchedpixel.scenecraft.render.utils.RenderJob;
import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.patchedpixel.scenecraft.replay.events.ReplayClosedCallback;
import net.patchedpixel.scenecraft.replay.events.ReplayOpenedCallback;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayFile;
import net.patchedpixel.scenecraft.gui.container.VanillaGuiScreen;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SceneCraftRender extends EventRegistrations implements Module {
    { instance = this; }
    public static SceneCraftRender instance;

    private SceneCraft core;

    public static Logger LOGGER = LogManager.getLogger();

    private ReplayFile replayFile;
    private final List<RenderJob> renderQueue = new ArrayList<>();

    public SceneCraftRender(SceneCraft core) {
        this.core = core;

        core.getSettingsRegistry().register(Setting.class);
    }

    public SceneCraft getCore() {
        return core;
    }

    @Override
    public void initClient() {
        register();
    }

    public File getVideoFolder() {
        String path = core.getSettingsRegistry().get(Setting.RENDER_PATH);
        File folder = new File(path.startsWith("./") ? core.getMinecraft().gameDirectory : null, path);
        try {
            FileUtils.forceMkdir(folder);
        } catch (IOException e) {
            throw new ReportedException(CrashReport.forThrowable(e, "Cannot create video folder."));
        }
        return folder;
    }

    public Path getRenderSettingsPath() {
        return core.getMinecraft().gameDirectory.toPath().resolve("config/scenecraft-rendersettings.json");
    }

    public List<RenderJob> getRenderQueue() {
        return renderQueue;
    }

    { on(ReplayOpenedCallback.EVENT, this::onReplayOpened); }
    private void onReplayOpened(ReplayHandler replayHandler) {
        replayFile = replayHandler.getReplayFile();
        try {
            renderQueue.addAll(RenderJob.readQueue(replayFile));
        } catch (IOException e) {
            throw new ReportedException(CrashReport.forThrowable(e, "Reading timeline"));
        }
    }

    { on(ReplayClosedCallback.EVENT, replayHandler -> onReplayClosed()); }
    private void onReplayClosed() {
        renderQueue.clear();
        replayFile = null;
    }

    public void saveRenderQueue() {
        try {
            RenderJob.writeQueue(replayFile, renderQueue);
        } catch (IOException e) {
            e.printStackTrace();
            VanillaGuiScreen screen = VanillaGuiScreen.wrap(getCore().getMinecraft().screen);
            CrashReport report = CrashReport.forThrowable(e, "Reading timeline");
            Utils.error(LOGGER, screen, report, () -> {});
        }
    }
}
