package net.patchedpixel.scenecraft.common.render.gui;

import com.google.common.collect.Iterables;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import net.patchedpixel.scenecraft.common.render.FFmpegWriter;
import net.patchedpixel.scenecraft.common.render.RenderSettings;
import net.patchedpixel.scenecraft.common.render.SceneCraftRender;
import net.patchedpixel.scenecraft.common.render.rendering.VideoRenderer;
import net.patchedpixel.scenecraft.common.render.utils.RenderJob;
import net.patchedpixel.scenecraft.common.replay.ReplayHandler;
import net.patchedpixel.scenecraft.common.replay.ReplaySender;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.path.Timeline;
import net.patchedpixel.scenecraft.common.scenestudio.replay.ReplayFile;
import net.patchedpixel.scenecraft.common.flamex.components.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.FlameXConstants;
import net.patchedpixel.scenecraft.common.flamex.widget.NineSlice;
import net.patchedpixel.scenecraft.common.flamex.widget.SelectableList;
import net.patchedpixel.scenecraft.common.utils.Result;
import net.patchedpixel.scenecraft.common.utils.Utils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static net.patchedpixel.scenecraft.common.render.SceneCraftRender.LOGGER;

public class GuiRenderQueue extends FlameableScreen {
    private final Screen container;
    private final ReplayHandler replayHandler;
    private final Supplier<Result<Timeline, String[]>> timelineSupplier;
    private final SceneCraftRender mod = SceneCraftRender.instance;
    private final List<RenderJob> jobs = mod.getRenderQueue();
    private final Set<JobEntry> selectedEntries = new LinkedHashSet<>();
    private boolean opened;

    private SelectableList<JobEntry> list;
    private Button addButton, editButton, removeButton, renderButton;

    public GuiRenderQueue(Screen container, ReplayHandler replayHandler, Supplier<Result<Timeline, String[]>> timelineSupplier) {
        super("scenecraft.gui.renderqueue.title", container);
        this.drawTitle = false;
        this.container = container;
        this.replayHandler = replayHandler;
        this.timelineSupplier = timelineSupplier;
        LOGGER.trace("Opening render queue popup");
        this.background = Background.NONE;
    }

    @Override
    protected void build(LinearLayout root) {}

    @Override
    protected void init() {
        selectedEntries.removeIf(e -> !jobs.contains(e.job));
        Set<JobEntry> keep = new LinkedHashSet<>(selectedEntries);

        int lineHeight = MCVer.getFontRenderer().lineHeight;
        int contentWidth = width - 40;
        int contentHeight = height - 20 - 45 - lineHeight;
        int contentX = 20;
        int contentY = (height - (contentHeight + 20)) / 2 + 10;
        popupX = contentX - 10;
        popupY = contentY - 10;
        popupWidth = contentWidth + 20;
        popupHeight = contentHeight + 20;
        titleY = contentY;

        list = new SelectableList<>(contentWidth, contentHeight - 45 - (lineHeight + 5) - 10, 305, 16, selectedEntries,
                e -> e.job.getName(), (entry, ctrl) -> {
            if(!ctrl) selectedEntries.clear();
            if(!selectedEntries.remove(entry)) selectedEntries.add(entry);
            updateButtons();
        }).setRowSpacing(3);
        for(RenderJob renderJob : jobs) {
            JobEntry entry = new JobEntry(renderJob);
            for(JobEntry old : keep) {
                if(old.job == renderJob) {
                    selectedEntries.remove(old);
                    selectedEntries.add(entry);
                }
            }
            list.add(entry);
        }

        addButton = FlameX.button("scenecraft.gui.renderqueue.add", 150, () ->
                addButtonClicked().ifErr(lines -> MCVer.getMinecraft().setScreen(FlameX.info(this::display, lines))));
        renderButton = FlameX.button("scenecraft.gui.renderqueue.renderall", 150, this::renderClicked);
        editButton = FlameX.button("scenecraft.gui.edit", 73, () -> {
            JobEntry job = selectedEntries.iterator().next();
            job.edit().open();
        });
        removeButton = FlameX.button("scenecraft.gui.remove", 73, () -> {
            for(JobEntry entry : selectedEntries) {
                LOGGER.trace("Remove button clicked for {}", entry.job);
                jobs.remove(entry.job);
            }
            selectedEntries.clear();
            mod.saveRenderQueue();
            rebuildWidgets();
        });
        Button closeButton = FlameX.button("scenecraft.gui.close", 150, this::close);

        int buttonsX = width / 2 - 305 / 2;
        int row1 = contentY + contentHeight - 45;
        int row2 = row1 + 25;
        addButton.setPosition(buttonsX, row1);
        renderButton.setPosition(buttonsX + 155, row1);
        editButton.setPosition(buttonsX, row2);
        removeButton.setPosition(buttonsX + 77, row2);
        closeButton.setPosition(buttonsX + 155, row2);
        list.setPosition(contentX, contentY + lineHeight + 5);
        addRenderableWidget(list);
        for(Button b : new Button[]{addButton, renderButton, editButton, removeButton, closeButton}) {
            addRenderableWidget(b);
        }
        updateButtons();
    }

    private int popupX, popupY, popupWidth, popupHeight, titleY;

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if(parent != null) {
            parent.render(graphics, -1, -1, partialTicks);
        } else {
            renderBackground(graphics);
        }
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 10);
        graphics.fill(0, 0, width, height, FlameXConstants.DIM);
        NineSlice.panel(graphics, popupX, popupY, popupWidth, popupHeight);
        graphics.drawString(font, title, width / 2 - font.width(title) / 2, titleY, FlameXConstants.TEXT_ON_PANEL | 0xFF000000, false);
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.pose().popPose();
    }

    private void renderClicked() {
        LOGGER.trace("Render button clicked");
        List<RenderJob> renderQueue = new ArrayList<>();
        if(selectedEntries.isEmpty()) {
            renderQueue.addAll(jobs);
        } else {
            Set<RenderJob> selectedJobs = selectedEntries.stream().map(it -> it.job).collect(Collectors.toSet());
            for(RenderJob job : jobs) {
                if(selectedJobs.contains(job)) {
                    renderQueue.add(job);
                }
            }
        }
        SceneCraft.instance.runLaterWithoutLock(() -> processQueue(container, replayHandler, renderQueue, () -> {}));
    }

    private static void processQueue(Screen container, ReplayHandler replayHandler, Iterable<RenderJob> queue, Runnable done) {
        Minecraft mc = MCVer.getMinecraft();

        // Close all GUIs (so settings in GuiRenderSettings are saved)
        mc.setScreen(null);
        // Start rendering
        int jobsDone = 0;
        for(RenderJob renderJob : queue) {
            LOGGER.info("Starting render job {}", renderJob);
            try {
                VideoRenderer videoRenderer = new VideoRenderer(renderJob.getSettings(), replayHandler, renderJob.getTimeline());
                videoRenderer.renderVideo();
            } catch(FFmpegWriter.NoFFmpegException e) {
                LOGGER.error("Rendering video:", e);
                mc.setScreen(new GuiNoFfmpeg(() -> mc.setScreen(container)));
                return;
            } catch(FFmpegWriter.FFmpegStartupException e) {
                int jobsToSkip = jobsDone;
                GuiExportFailed.tryToRecover(e, newSettings -> {
                    // Update current job with fixed ffmpeg arguments
                    renderJob.setSettings(newSettings);
                    // Restart queue, skipping the already completed jobs
                    SceneCraft.instance.runLaterWithoutLock(() -> processQueue(container, replayHandler, Iterables.skip(queue, jobsToSkip), done));
                });
                return;
            } catch(Throwable t) {
                Utils.error(LOGGER, CrashReport.forThrowable(t, "Rendering video"), () -> {});
                mc.setScreen(container); // Re-show the queue and the new error popup
                return;
            }
            jobsDone++;
        }
        done.run();
    }

    public static void processMultipleReplays(
            Screen container,
            SceneCraftReplay mod,
            Iterator<Pair<File, List<RenderJob>>> queue,
            Runnable done
    ) {
        if(!queue.hasNext()) {
            done.run();
            return;
        }
        Pair<File, List<RenderJob>> next = queue.next();

        LOGGER.info("Opening replay {} for {} render jobs", next.getKey(), next.getValue().size());
        ReplayHandler replayHandler;
        ReplayFile replayFile = null;
        try {
            replayFile = mod.getCore().files.open(next.getKey().toPath());
            replayHandler = mod.startReplay(replayFile, false, false);
        } catch(IOException e) {
            Utils.error(LOGGER, CrashReport.forThrowable(e, "Opening replay"), () -> {});
            MCVer.getMinecraft().setScreen(container);
            IOUtils.closeQuietly(replayFile);
            return;
        }
        if(replayHandler == null) {
            LOGGER.warn("Replay failed to open (missing mods?), skipping..");
            IOUtils.closeQuietly(replayFile);
            processMultipleReplays(container, mod, queue, done);
            return;
        }
        ReplaySender replaySender = replayHandler.getReplaySender();

        Minecraft mc = mod.getCore().getMinecraft();
        int jumpTo = 1000;
        while(mc.level == null && jumpTo < replayHandler.getReplayDuration()) {
            replaySender.sendPacketsTill(jumpTo);
            jumpTo += 1000;
        }
        if(mc.level == null) {
            LOGGER.warn("Replay failed to load world (corrupted?), skipping..");
            IOUtils.closeQuietly(replayFile);
            processMultipleReplays(container, mod, queue, done);
            return;
        }

        processQueue(container, replayHandler, next.getValue(), () -> {
            try {
                replayHandler.endReplay();
            } catch(IOException e) {
                Utils.error(LOGGER, CrashReport.forThrowable(e, "Closing replay"), () -> {});
                mc.setScreen(container);
                return;
            }
            processMultipleReplays(container, mod, queue, done);
        });
    }

    private Result<GuiRenderSettings, String[]> addButtonClicked() {
        return timelineSupplier.get().mapOk(timeline -> {
            GuiRenderSettings popup = addJob(timeline);
            popup.open();
            return popup;
        });
    }

    public GuiRenderSettings addJob(Timeline timeline) {
        return addJob(timeline, null);
    }

    public GuiRenderSettings addJob(Timeline timeline, Runnable afterAdd) {
        Screen settingsParent = opened ? this : container;
        GuiRenderSettings settings = new GuiRenderSettings(settingsParent, replayHandler, timeline);
        if(!jobs.isEmpty()) {
            settings.hideRenderButton();
        }
        settings.setQueueButton("scenecraft.gui.rendersettings.addtoqueue", ctrl -> {
            RenderSettings s = settings.save(false, ctrl);

            RenderJob newJob = new RenderJob();
            newJob.setSettings(s);
            newJob.setTimeline(timeline);
            LOGGER.trace("Adding new job: {}", newJob);
            jobs.add(newJob);
            mod.saveRenderQueue();

            settings.saveSettings();
            if(!opened) {
                display();
                opened = true;
            } else {
                settings.close();
            }
            if(afterAdd != null) {
                afterAdd.run();
            }
        });
        return settings;
    }

    public void open() {
        if(jobs.isEmpty() && timelineSupplier != null) {
            addButtonClicked().ifErr(lines ->
                    MCVer.getMinecraft().setScreen(FlameX.info(() -> MCVer.getMinecraft().setScreen(container), lines)));
            return;
        }
        display();
        opened = true;
    }

    @Override
    public void close() {
        opened = false;
        super.close();
    }

    public void updateButtons() {
        if(addButton == null) return;
        int selected = selectedEntries.size();
        addButton.active = timelineSupplier != null;
        editButton.active = selected == 1;
        removeButton.active = selected >= 1;
        renderButton.active = jobs.size() > 0;
        renderButton.setMessage(Component.translatable("scenecraft.gui.renderqueue.render" + (selected > 0 ? "selected" : "all")));
        renderButton.setTooltip(null);

        String[] compatError = VideoRenderer.checkCompat(jobs.stream().map(RenderJob::getSettings));
        if(compatError != null) {
            renderButton.active = false;
            renderButton.setTooltip(Tooltip.create(MCVer.literalText(String.join("\n", compatError))));
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(Screen.hasControlDown() && keyCode == InputConstants.KEY_A) {
            if(selectedEntries.size() < list.items().size()) {
                selectedEntries.addAll(list.items());
            } else {
                selectedEntries.clear();
            }
            updateButtons();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public class JobEntry {
        public final RenderJob job;

        public JobEntry(RenderJob job) {
            this.job = job;
        }

        public GuiRenderSettings edit() {
            GuiRenderSettings gui = new GuiRenderSettings(GuiRenderQueue.this, replayHandler, job.getTimeline());
            gui.hideRenderButton();
            gui.setQueueButton("scenecraft.gui.done", ctrl -> {
                job.setSettings(gui.save(false, ctrl));
                mod.saveRenderQueue();
                gui.close();
            });
            gui.load(job.getSettings());
            gui.setCloseHook(GuiRenderQueue.this::rebuildWidgets);
            return gui;
        }
    }
}
