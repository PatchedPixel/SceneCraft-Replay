package net.patchedpixel.scenecraft.replay.gui.screen;

import net.minecraft.util.Mth;
import net.patchedpixel.scenecraft.core.SceneCraft;
import net.patchedpixel.scenecraft.render.gui.GuiRenderQueue;
import net.patchedpixel.scenecraft.render.rendering.VideoRenderer;
import net.patchedpixel.scenecraft.render.utils.RenderJob;
import net.patchedpixel.scenecraft.gui.GuiRenderer;
import net.patchedpixel.scenecraft.gui.RenderInfo;
import net.patchedpixel.scenecraft.gui.function.CharHandler;
import net.patchedpixel.scenecraft.gui.function.CharInput;
import net.patchedpixel.scenecraft.gui.function.Click;
import net.patchedpixel.scenecraft.gui.function.KeyHandler;
import net.patchedpixel.scenecraft.gui.function.KeyInput;
import net.patchedpixel.scenecraft.gui.versions.Image;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.minecraft.client.gui.Font;
import net.minecraft.ChatFormatting;
import net.patchedpixel.scenecraft.core.SettingsRegistry;
import net.patchedpixel.scenecraft.core.gui.GuiReplaySettings;
import net.patchedpixel.scenecraft.core.utils.Utils;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import com.mojang.blaze3d.platform.InputConstants;
import net.patchedpixel.scenecraft.replay.Setting;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayFile;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayMetaData;
import net.patchedpixel.scenecraft.gui.container.AbstractGuiContainer;
import net.patchedpixel.scenecraft.gui.container.GuiContainer;
import net.patchedpixel.scenecraft.gui.container.GuiPanel;
import net.patchedpixel.scenecraft.gui.container.GuiScreen;
import net.patchedpixel.scenecraft.gui.element.*;
import net.patchedpixel.scenecraft.gui.element.advanced.AbstractGuiResourceLoadingList;
import net.patchedpixel.scenecraft.gui.layout.CustomLayout;
import net.patchedpixel.scenecraft.gui.layout.HorizontalLayout;
import net.patchedpixel.scenecraft.gui.layout.VerticalLayout;
import net.patchedpixel.scenecraft.gui.popup.GuiYesNoPopup;
import net.patchedpixel.scenecraft.gui.utils.Colors;
import net.patchedpixel.scenecraft.gui.utils.Consumer;
import net.patchedpixel.scenecraft.gui.utils.geometry.Dimension;
import net.patchedpixel.scenecraft.gui.utils.geometry.ReadableDimension;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.client.gui.screens.Screen;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static net.patchedpixel.scenecraft.replay.SceneCraftReplay.LOGGER;
import static net.patchedpixel.scenecraft.core.versions.MCVer.getFontRenderer;

public class GuiReplayViewer extends GuiScreen implements KeyHandler {
    private final SceneCraftReplay mod;
    private Screen parentScreen;

    public final GuiReplayList list = new GuiReplayList(this).onSelectionChanged(this::updateButtons).onSelectionDoubleClicked(() -> {
        if (this.loadButton.isEnabled()) {
            this.loadButton.onClick(new Click(-1, -1, 0, 0));
        }
    });

    @Override
    public void display() {
        Screen currentScreen = getMinecraft().screen;

        if (currentScreen != null && currentScreen != toMinecraft()) {
            parentScreen = currentScreen;
        }

        super.display();
    }

    @Override
    protected boolean handleClose() {
        close();
        return true;
    }

    private void close() {
        getMinecraft().setScreen(parentScreen);
    }

    public final GuiButton loadButton = new GuiButton().onClick(new Runnable() {
        private boolean loading = false;

        @Override
        public void run() {
            // Prevent the player from opening the replay twice at the same time
            if (loading) {
                return;
            }
            loading = true;
            loadButton.setDisabled(); // visual hint

            List<GuiReplayEntry> selected = list.getSelected();
            if (selected.size() == 1) {
                File file = selected.get(0).file;
                LOGGER.info("Opening replay in viewer: " + file);
                try {
                    mod.startReplay(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Iterator<Pair<File, List<RenderJob>>> replays = selected.stream()
                        .filter(it -> !it.renderQueue.isEmpty())
                        .<Pair<File, List<RenderJob>>>map(it -> Pair.of(it.file, it.renderQueue))
                        .iterator();
                GuiRenderQueue.processMultipleReplays(GuiReplayViewer.this, mod, replays, () -> {
                    loading = false;
                    updateButtons();
                    display();
                });
            }
        }
    }).setSize(150, 20);

    public final GuiButton folderButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            try {
                File folder = mod.getCore().folders.getReplayFolder().toFile();

                MCVer.openFile(folder);
            } catch (IOException e) {
                mod.getLogger().error("Cannot open file", e);
            }
        }
    }).setSize(150, 20).setI18nLabel("scenecraft.gui.viewer.replayfolder");

    public final GuiButton renameButton = new GuiButton().onClick(new Runnable() {
        @Override
        public void run() {
            final Path path = list.getSelected().get(0).file.toPath();
            String name = Utils.fileNameToReplayName(path.getFileName().toString());
            final GuiTextField nameField = new GuiTextField().setSize(200, 20).setFocused(true).setText(name);
            final GuiYesNoPopup popup = GuiYesNoPopup.open(GuiReplayViewer.this,
                    new GuiLabel().setI18nText("scenecraft.gui.viewer.rename.name").setColor(Colors.BLACK),
                    nameField
            ).setYesI18nLabel("scenecraft.gui.rename").setNoI18nLabel("scenecraft.gui.cancel");
            ((VerticalLayout) popup.getInfo().getLayout()).setSpacing(7);
            nameField.onEnter(new Runnable() {
                @Override
                public void run() {
                    if (popup.getYesButton().isEnabled()) {
                        popup.getYesButton().onClick(new Click(-1, -1, 0, 0));
                    }
                }
            }).onTextChanged(obj -> {
                popup.getYesButton().setEnabled(!nameField.getText().isEmpty()
                        && Files.notExists(Utils.replayNameToPath(path.getParent(), nameField.getText())));
            });
            popup.onAccept(() -> {
                // Sanitize their input
                String newName = nameField.getText().trim();
                // This file is what they want
                Path targetPath = Utils.replayNameToPath(path.getParent(), newName);
                try {
                    // Finally, try to move it
                    Files.move(path, targetPath);
                } catch (IOException e) {
                    // We failed (might also be their OS)
                    e.printStackTrace();
                    getMinecraft().setScreen(new AlertScreen(
                            GuiReplayViewer.this::display,
                            net.minecraft.network.chat.Component.translatable("scenecraft.gui.viewer.delete.failed1"),
                            net.minecraft.network.chat.Component.translatable("scenecraft.gui.viewer.delete.failed2")
                    ));

                    return;
                }
                list.load();
            });
        }
    }).setSize(73, 20).setI18nLabel("scenecraft.gui.rename").setDisabled();
    public final GuiButton deleteButton = new GuiButton().onClick(() -> {
        for (GuiReplayEntry entry : list.getSelected()) {
            String name = entry.name.getText();
            GuiYesNoPopup.open(GuiReplayViewer.this,
                    new GuiLabel().setI18nText("scenecraft.gui.viewer.delete.linea").setColor(Colors.BLACK),
                    new GuiLabel().setI18nText("scenecraft.gui.viewer.delete.lineb", name + ChatFormatting.RESET).setColor(Colors.BLACK)
            ).setYesI18nLabel("scenecraft.gui.delete").setNoI18nLabel("scenecraft.gui.cancel").onAccept(() -> {
                try {
                    FileUtils.forceDelete(entry.file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                list.load();
            });
        }
    }).setSize(73, 20).setI18nLabel("scenecraft.gui.delete").setDisabled();

    public final GuiButton settingsButton = new GuiButton(this)
            .setSize(20, 20)
            .setTexture(SceneCraft.TEXTURE, SceneCraft.TEXTURE_SIZE).setSpriteUV(20, 0)
            .setTooltip(new GuiTooltip().setI18nText("scenecraft.gui.settings"))
            .onClick(() -> new GuiReplaySettings(toMinecraft(), getMod().getCore().getSettingsRegistry()).display());

    public final GuiButton cancelButton = new GuiButton()
            .onClick(this::close)
            .setSize(73, 20)
            .setI18nLabel("scenecraft.gui.cancel");

    public final List<GuiButton> replaySpecificButtons = new ArrayList<>();
    { replaySpecificButtons.addAll(Arrays.asList(renameButton)); }
    public final GuiPanel editorButton = new GuiPanel();

    public final GuiPanel upperButtonPanel = new GuiPanel().setLayout(new HorizontalLayout().setSpacing(5))
            .addElements(null, loadButton);
    public final GuiPanel lowerButtonPanel = new GuiPanel().setLayout(new HorizontalLayout().setSpacing(5))
            .addElements(null, renameButton, deleteButton, editorButton, cancelButton);
    public final GuiPanel buttonPanel = new GuiPanel(this).setLayout(new VerticalLayout().setSpacing(5))
            .addElements(null, upperButtonPanel, lowerButtonPanel);

    public GuiReplayViewer(SceneCraftReplay mod) {
        this.mod = mod;

        try {
            list.setFolder(mod.getCore().folders.getReplayFolder().toFile());
        } catch (IOException e) {
            throw new ReportedException(CrashReport.forThrowable(e, "Getting replay folder"));
        }

        setTitle(new GuiLabel().setI18nText("scenecraft.gui.replayviewer"));

        setLayout(new CustomLayout<GuiScreen>() {
            @Override
            protected void layout(GuiScreen container, int width, int height) {
                pos(buttonPanel, width / 2 - width(buttonPanel) / 2, height - 10 - height(buttonPanel));

                pos(list, 0, 30);
                size(list, width, y(buttonPanel) - 10 - y(list));

                pos(settingsButton, width - width(settingsButton) - 5, 5);
            }
        });

        updateButtons();
    }

    public SceneCraftReplay getMod() {
        return mod;
    }

    private void updateButtons() {
        List<GuiReplayEntry> selected = list.getSelected();
        int count = selected.size();

        replaySpecificButtons.forEach(b -> b.setEnabled(count == 1));
        deleteButton.setEnabled(count > 0);
        if (count > 1) {
            Set<RenderJob> jobs = selected.stream().flatMap(entry -> entry.renderQueue.stream()).collect(Collectors.toSet());
            String[] tooltipLines = jobs.stream().map(RenderJob::getName).toArray(String[]::new);
            loadButton.setI18nLabel("scenecraft.gui.viewer.bulkrender", jobs.size());
            loadButton.setTooltip(new GuiTooltip().setText(tooltipLines));
            loadButton.setEnabled(!jobs.isEmpty());

            String[] compatError = VideoRenderer.checkCompat(jobs.stream().map(RenderJob::getSettings));
            if (compatError != null) {
                loadButton.setDisabled().setTooltip(new GuiTooltip().setText(compatError));
            }
        } else {
            loadButton.setI18nLabel("scenecraft.gui.load");
            loadButton.setTooltip(null);
            loadButton.setEnabled(count == 1 && !selected.get(0).incompatible);
        }
    }

    private static final GuiImage DEFAULT_THUMBNAIL = new GuiImage().setTexture(Utils.DEFAULT_THUMBNAIL);

    @Override
    public boolean handleKey(KeyInput keyInput) {
        if (keyInput.key == InputConstants.KEY_ESCAPE) {
            close();
            return true;
        }

        return false;
    }

    public static class GuiReplayList extends AbstractGuiResourceLoadingList<GuiReplayList, GuiReplayEntry> implements KeyHandler, CharHandler {
        private File folder = null;

        // Not actually a child of this element, we just use it for text manipulation
        private final GuiTextField filterTextField = new GuiTextField()
                .setFocused(true);

        public GuiReplayList(GuiContainer container) {
            super(container);
        }

        {
            onLoad((Consumer<Supplier<GuiReplayEntry>> results) -> {
                File[] files = folder.listFiles((FileFilter) new SuffixFileFilter(".mcpr", IOCase.INSENSITIVE));
                if (files == null) {
                    LOGGER.warn("Failed to list files in {}", folder);
                    return;
                }
                if (files.length == 0) {
                    return;
                }
                Map<File, Long> lastModified = new HashMap<>();
                Arrays.sort(files, Comparator.<File>comparingLong(f -> lastModified.computeIfAbsent(f, File::lastModified)).reversed());

                int threads = Mth.clamp(files.length, 1, Math.min(8, Runtime.getRuntime().availableProcessors()));
                AtomicInteger counter = new AtomicInteger();
                ExecutorService pool = Executors.newFixedThreadPool(threads, runnable -> {
                    Thread thread = new Thread(runnable, "SceneCraft-ReplayLoader-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                });

                try {
                    List<Future<?>> futures = new ArrayList<>(files.length);
                    for (final File file : files) {
                        futures.add(pool.submit(() -> loadEntry(file, results)));
                    }
                    for (Future<?> future : futures) {
                        try {
                            future.get();
                        } catch (ExecutionException e) {
                            LOGGER.error("Could not load Replay File", e.getCause());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    pool.shutdownNow();
                    boolean interrupted = Thread.interrupted();
                    try {
                        pool.awaitTermination(30, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        interrupted = true;
                    } finally {
                        if (interrupted) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }).setDrawShadow(true).setDrawSlider(true);
        }

        private void loadEntry(File file, Consumer<Supplier<GuiReplayEntry>> results) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            try (ReplayFile replayFile = SceneCraft.instance.files.open(file.toPath())) {
                final boolean[] converted = {false};
                final Image thumb = replayFile.getThumbBytes().flatMap(stream -> {
                    try {
                        return Optional.of(Image.read(replayFile, stream, converted));
                    } catch (IOException e) {
                        if (!Thread.currentThread().isInterrupted()) {
                            e.printStackTrace();
                        }
                        return Optional.empty();
                    }
                }).orElse(null);
                if (converted[0]) {
                    replayFile.save();
                }
                final ReplayMetaData metaData = replayFile.getMetaData();
                List<RenderJob> renderQueue = RenderJob.readQueue(replayFile);

                if (Thread.currentThread().isInterrupted()) {
                    if (thumb != null) {
                        thumb.close();
                    }
                    return;
                }

                if (metaData != null) {
                    results.consume(() -> new GuiReplayEntry(file, metaData, thumb, renderQueue) {
                        @Override
                        public ReadableDimension calcMinSize() {
                            if (isFiltered(this)) {
                                return new Dimension(-4, -4);
                            }
                            return super.calcMinSize();
                        }
                    });
                } else if (thumb != null) {
                    thumb.close();
                }
            } catch (Exception e) {
                LOGGER.error("Could not load Replay File {}", file.getName(), e);
            }
        }

        public void setFolder(File folder) {
            this.folder = folder;
        }

        private boolean isFiltered(GuiReplayEntry entry) {
            String filter = filterTextField.getText().toLowerCase();
            if (filter.isEmpty()) {
                return false;
            }
            return !entry.name.getText().toLowerCase().contains(filter);
        }

        @Override
        public boolean handleKey(KeyInput keyInput) {
            if (keyInput.key == InputConstants.KEY_F1) {
                SettingsRegistry reg = SceneCraft.instance.getSettingsRegistry();
                reg.set(Setting.SHOW_SERVER_IPS, !reg.get(Setting.SHOW_SERVER_IPS));
                reg.save();
                load();
            }

            boolean filterHasPriority = !filterTextField.getText().isEmpty();
            if (filterHasPriority && filterTextField.handleKey(keyInput)) {
                scrollY(0); // ensure we scroll to top if most entries are filtered
                return true;
            }

            if (super.handleKey(keyInput)) {
                return true;
            }

            if (!filterHasPriority && filterTextField.handleKey(keyInput)) {
                scrollY(0); // ensure we scroll to top if most entries are filtered
                return true;
            }

            return false;
        }

        @Override
        public boolean handleChar(CharInput charInput) {
            if (filterTextField.handleChar(charInput)) {
                scrollY(0); // ensure we scroll to top if most entries are filtered
                return true;
            }

            return false;
        }

        @Override
        public void draw(GuiRenderer renderer, ReadableDimension size, RenderInfo renderInfo) {
            super.draw(renderer, size, renderInfo);

            String filter = filterTextField.getText();
            if (!filter.isEmpty()) {
                boolean anyMatches = getListPanel().calcMinSize().getHeight() > 0;

                Font fontRenderer = getFontRenderer();
                int filterTextWidth = fontRenderer.width(filter);
                int filterTextHeight = fontRenderer.lineHeight;
                renderer.drawRect(
                        size.getWidth() - 3 - 2 - filterTextWidth - 2,
                        size.getHeight() - 3 - 2 - filterTextHeight - 2,
                        2 + filterTextWidth + 2,
                        2 + filterTextHeight + 2,
                        Colors.WHITE
                );
                renderer.drawString(
                        size.getWidth() - 3 - 2 - filterTextWidth,
                        size.getHeight() - 3 - 2 - filterTextHeight,
                        anyMatches ? Colors.BLACK : Colors.DARK_RED,
                        filter
                );
            }
        }

        @Override
        protected GuiReplayList getThis() {
            return this;
        }
    }

    public static class GuiReplayEntry extends AbstractGuiContainer<GuiReplayEntry> implements Comparable<GuiReplayEntry> {
        public final File file;
        public final GuiLabel name = new GuiLabel();
        public final GuiLabel server = new GuiLabel().setColor(Colors.LIGHT_GRAY);
        public final GuiLabel date = new GuiLabel().setColor(Colors.LIGHT_GRAY);
        public final GuiPanel infoPanel = new GuiPanel(this).setLayout(new VerticalLayout().setSpacing(2))
                .addElements(null, name, server, date);
        public final GuiLabel version = new GuiLabel(this).setColor(Colors.RED);
        public final GuiImage thumbnail;
        public final GuiLabel duration = new GuiLabel();
        public final GuiPanel durationPanel = new GuiPanel().setBackgroundColor(Colors.HALF_TRANSPARENT)
                .addElements(null, duration).setLayout(new CustomLayout<GuiPanel>() {
                    @Override
                    protected void layout(GuiPanel container, int width, int height) {
                        pos(duration, 2, 2);
                    }

                    @Override
                    public ReadableDimension calcMinSize(GuiContainer<?> container) {
                        ReadableDimension dimension = duration.calcMinSize();
                        return new Dimension(dimension.getWidth() + 2, dimension.getHeight() + 2);
                    }
                });
        public final GuiImage renderQueueIcon = new GuiImage()
                .setSize(10, 10)
                .setTexture(SceneCraft.TEXTURE, 40, 0, 20, 20);

        private final long dateMillis;
        private final boolean incompatible;
        private final List<RenderJob> renderQueue;

        public GuiReplayEntry(File file, ReplayMetaData metaData, Image thumbImage, List<RenderJob> renderQueue) {
            this.file = file;
            this.renderQueue = renderQueue;

            name.setText(ChatFormatting.UNDERLINE + Utils.fileNameToReplayName(file.getName()));
            if (!StringUtils.isEmpty(metaData.getCustomServerName())) {
                server.setText(metaData.getCustomServerName());
            } else if (StringUtils.isEmpty(metaData.getServerName())
                    || !SceneCraft.instance.getSettingsRegistry().get(Setting.SHOW_SERVER_IPS)) {
                server.setI18nText("scenecraft.gui.iphidden").setColor(Colors.DARK_RED);
            } else {
                server.setText(metaData.getServerName());
            }
            incompatible = !SceneCraft.isCompatible(metaData.getFileFormatVersion(), metaData.getRawProtocolVersionOr0());
            if (incompatible) {
                version.setText("Minecraft " + metaData.getMcVersion());
            }
            dateMillis = metaData.getDate();
            date.setText(new SimpleDateFormat().format(new Date(dateMillis)));
            if (thumbImage == null) {
                thumbnail = new GuiImage(DEFAULT_THUMBNAIL).setSize(30 * 16 / 9, 30);
                addElements(null, thumbnail);
            } else {
                thumbnail = new GuiImage(this).setTexture(thumbImage).setSize(30 * 16 / 9, 30);
            }
            duration.setText(Utils.convertSecondsToShortString(metaData.getDuration() / 1000));
            addElements(null, durationPanel);

            if (!renderQueue.isEmpty()) {
                renderQueueIcon.setTooltip(new GuiTooltip()
                        .setText(renderQueue.stream().map(RenderJob::getName).toArray(String[]::new)));
                addElements(null, renderQueueIcon);
            }

            setLayout(new CustomLayout<GuiReplayEntry>() {
                @Override
                protected void layout(GuiReplayEntry container, int width, int height) {
                    pos(thumbnail, 0, 0);
                    x(durationPanel, width(thumbnail) - width(durationPanel));
                    y(durationPanel, height(thumbnail) - height(durationPanel));

                    pos(infoPanel, width(thumbnail) + 5, 0);
                    pos(version, width - width(version), 0);

                    if (renderQueueIcon.getContainer() != null) {
                        pos(renderQueueIcon, width(thumbnail) - width(renderQueueIcon), 0);
                    }
                }

                @Override
                public ReadableDimension calcMinSize(GuiContainer<?> container) {
                    return new Dimension(300, thumbnail.getMinSize().getHeight());
                }
            });
        }

        @Override
        protected GuiReplayEntry getThis() {
            return this;
        }

        @Override
        public int compareTo(GuiReplayEntry o) {
            return Long.compare(o.dateMillis, dateMillis);
        }
    }
}
