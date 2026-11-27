package net.patchedpixel.scenecraft.common.replay.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.core.SettingsRegistry;
import net.patchedpixel.scenecraft.common.core.gui.GuiReplaySettings;
import net.patchedpixel.scenecraft.common.render.gui.GuiRenderQueue;
import net.patchedpixel.scenecraft.common.render.rendering.VideoRenderer;
import net.patchedpixel.scenecraft.common.render.utils.RenderJob;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.common.replay.Setting;
import net.patchedpixel.scenecraft.common.scenestudio.replay.ReplayFile;
import net.patchedpixel.scenecraft.common.scenestudio.replay.ReplayMetaData;
import net.patchedpixel.scenecraft.common.flamex.screen.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.screen.popup.MessageScreen;
import net.patchedpixel.scenecraft.common.flamex.screen.popup.PopupScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.FlameXTheme;
import net.patchedpixel.scenecraft.common.flamex.util.Image;
import net.patchedpixel.scenecraft.common.flamex.widget.SpriteButton;
import net.patchedpixel.scenecraft.common.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;
import org.jetbrains.annotations.NotNull;

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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.identifier;
import static net.patchedpixel.scenecraft.common.replay.SceneCraftReplay.LOGGER;

public class GuiReplayViewer extends FlameableScreen {
    public record ExtraButton(String labelKey, BiConsumer<GuiReplayViewer, File> action) {}

    public static final List<ExtraButton> EXTRA_BUTTONS = new ArrayList<>();

    private final SceneCraftReplay mod;
    private Screen parentScreen;

    public final GuiReplayList list;
    private boolean loading = false;

    private Button loadButton;
    private Button renameButton;
    private Button deleteButton;
    private final List<Button> replaySpecificButtons = new ArrayList<>();

    public GuiReplayViewer(SceneCraftReplay mod) {
        super("scenecraft.gui.replayviewer", null);
        this.drawTitle = false;
        this.mod = mod;
        this.list = new GuiReplayList(this);
        this.list.onSelectionChanged = this::updateButtons;
        this.list.onDoubleClick = () -> {
            if(loadButton != null && loadButton.active) {
                loadButton.onPress();
            }
        };
        try {
            list.setFolder(mod.getCore().folders.getReplayFolder().toFile());
        } catch(IOException e) {
            throw new ReportedException(CrashReport.forThrowable(e, "Getting replay folder"));
        }
        list.load();
    }

    public SceneCraftReplay getMod() {
        return mod;
    }

    @Override
    public void display() {
        Screen currentScreen = MCVer.getMinecraft().screen;
        if(currentScreen != null && currentScreen != this) {
            parentScreen = currentScreen;
        }
        super.display();
    }

    @Override
    public void close() {
        MCVer.getMinecraft().setScreen(parentScreen);
    }

    @Override
    protected void build(LinearLayout root) {}

    @Override
    protected void init() {
        replaySpecificButtons.clear();

        int listBottom = height - 10 - 20 - 25 - 10;
        list.updateSize(width, listBottom - 30, 30, listBottom);
        addRenderableWidget(list);

        loadButton = FlameX.button(Component.translatable("scenecraft.gui.load"), 150, this::loadSelected);
        renameButton = FlameX.button("scenecraft.gui.rename", 73, this::rename);
        deleteButton = FlameX.button("scenecraft.gui.delete", 73, this::deleteSelected);
        Button cancelButton = FlameX.button("scenecraft.gui.cancel", 73, this::close);
        replaySpecificButtons.add(renameButton);

        List<Button> lower = new ArrayList<>(List.of(renameButton, deleteButton));
        for(ExtraButton extra : EXTRA_BUTTONS) {
            Button b = FlameX.button(extra.labelKey, 73, () -> {
                List<GuiReplayEntry> selected = list.getSelectedEntries();
                if(selected.size() == 1) {
                    extra.action.accept(this, selected.get(0).file);
                }
            });
            replaySpecificButtons.add(b);
            lower.add(b);
        }
        lower.add(cancelButton);

        int lowerWidth = lower.size() * 73 + (lower.size() - 1) * 5;
        int bottom = height - 10;
        int lowerY = bottom - 20;
        int upperY = lowerY - 25;
        int x = width / 2 - lowerWidth / 2;
        int inset = (lowerWidth - 150) / 2;
        loadButton.setWidth(lowerWidth - inset * 2);
        loadButton.setPosition(x + inset, upperY);
        for(Button b : lower) {
            b.setPosition(x, lowerY);
            x += 78;
        }
        addRenderableWidget(loadButton);
        lower.forEach(this::addRenderableWidget);

        SpriteButton settingsButton = new SpriteButton(20, 20, SceneCraft.TEXTURE, SceneCraft.TEXTURE_SIZE, 20,
                () -> new GuiReplaySettings(this, getMod().getCore().getSettingsRegistry()).display()).uv(20, 0);
        settingsButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable("scenecraft.gui.settings")));
        settingsButton.setPosition(width - 20 - 5, 5);
        addRenderableWidget(settingsButton);

        updateButtons();
    }

    private void updateButtons() {
        if(loadButton == null) return;
        List<GuiReplayEntry> selected = list.getSelectedEntries();
        int count = selected.size();
        replaySpecificButtons.forEach(b -> b.active = count == 1);
        deleteButton.active = count > 0;
        if(count > 1) {
            Set<RenderJob> jobs = selected.stream().flatMap(entry -> entry.renderQueue.stream()).collect(Collectors.toSet());
            String tooltipLines = jobs.stream().map(RenderJob::getName).collect(Collectors.joining("\n"));
            loadButton.setMessage(Component.translatable("scenecraft.gui.viewer.bulkrender", jobs.size()));
            loadButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(MCVer.literalText(tooltipLines)));
            loadButton.active = !jobs.isEmpty();
            String[] compatError = VideoRenderer.checkCompat(jobs.stream().map(RenderJob::getSettings));
            if(compatError != null) {
                loadButton.active = false;
                loadButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(MCVer.literalText(String.join("\n", compatError))));
            }
        } else {
            loadButton.setMessage(Component.translatable("scenecraft.gui.load"));
            loadButton.setTooltip(null);
            loadButton.active = count == 1 && !selected.get(0).incompatible;
        }
    }

    private void loadSelected() {
        // Prevent the player from opening the replay twice at the same time
        if(loading) {
            return;
        }
        loading = true;
        loadButton.active = false;

        List<GuiReplayEntry> selected = list.getSelectedEntries();
        if(selected.size() == 1) {
            File file = selected.get(0).file;
            LOGGER.info("Opening replay in viewer: {}", file);
            try {
                mod.startReplay(file);
            } catch(IOException e) {
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

    private void rename() {
        final Path path = list.getSelectedEntries().get(0).file.toPath();
        String name = Utils.fileNameToReplayName(path.getFileName().toString());
        MCVer.getMinecraft().setScreen(new RenameScreen(this, path, name));
    }

    private class RenameScreen extends PopupScreen {
        private final Path path;
        private final String initialName;
        private EditBox nameField;
        private Button renameButton;

        RenameScreen(Screen parent, Path path, String initialName) {
            super(parent, 7);
            this.dim = true;
            this.path = path;
            this.initialName = initialName;
        }

        @Override
        protected void build(LinearLayout root) {
            root.addChild(PopupScreen.label(Component.translatable("scenecraft.gui.viewer.rename.name")));
            nameField = FlameX.textField(200, initialName, null, s -> updateState());
            root.addChild(nameField);
            LinearLayout buttons = FlameX.row(5);
            renameButton = FlameX.button("scenecraft.gui.rename", 98, this::accept);
            buttons.addChild(renameButton);
            buttons.addChild(FlameX.button("scenecraft.gui.cancel", 98, this::close));
            root.addChild(buttons);
            updateState();
        }

        @Override
        protected void afterLayout() {
            setInitialFocus(nameField);
        }

        private void updateState() {
            if(renameButton == null) return;
            renameButton.active = !nameField.getValue().isEmpty()
                    && Files.notExists(Utils.replayNameToPath(path.getParent(), nameField.getValue()));
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if((keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) && renameButton.active) {
                accept();
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        private void accept() {
            // Sanitize their input
            String newName = nameField.getValue().trim();
            // This file is what they want
            Path targetPath = Utils.replayNameToPath(path.getParent(), newName);
            try {
                // Finally, try to move it
                Files.move(path, targetPath);
            } catch(IOException e) {
                // We failed (might also be their OS)
                e.printStackTrace();
                MCVer.getMinecraft().setScreen(new MessageScreen(
                        GuiReplayViewer.this, GuiReplayViewer.this::display, null, null,
                        Component.translatable("scenecraft.gui.viewer.delete.failed1"),
                        Component.translatable("scenecraft.gui.viewer.delete.failed2")
                ));
                return;
            }
            list.load();
            MCVer.getMinecraft().setScreen(GuiReplayViewer.this);
        }
    }

    private void deleteSelected() {
        List<GuiReplayEntry> selected = new ArrayList<>(list.getSelectedEntries());
        confirmDelete(selected, 0);
    }

    private void confirmDelete(List<GuiReplayEntry> entries, int index) {
        if(index >= entries.size()) {
            list.load();
            MCVer.getMinecraft().setScreen(this);
            return;
        }
        GuiReplayEntry entry = entries.get(index);
        MCVer.getMinecraft().setScreen(new MessageScreen(this, () -> {
            try {
                FileUtils.forceDelete(entry.file);
            } catch(IOException e) {
                e.printStackTrace();
            }
            confirmDelete(entries, index + 1);
        }, "scenecraft.gui.delete", "scenecraft.gui.cancel",
                Component.translatable("scenecraft.gui.viewer.delete.linea"),
                Component.translatable("scenecraft.gui.viewer.delete.lineb", entry.getName() + ChatFormatting.RESET))
                .onDecline(() -> confirmDelete(entries, index + 1)));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(keyCode == InputConstants.KEY_F1) {
            SettingsRegistry reg = SceneCraft.instance.getSettingsRegistry();
            reg.set(Setting.SHOW_SERVER_IPS, !reg.get(Setting.SHOW_SERVER_IPS));
            reg.save();
            list.load();
            return true;
        }
        if(keyCode == InputConstants.KEY_BACKSPACE && !list.filter.isEmpty()) {
            list.setFilter(list.filter.substring(0, list.filter.length() - 1));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if(!Character.isISOControl(codePoint)) {
            list.setFilter(list.filter + codePoint);
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawString(font, title, width / 2 - font.width(title) / 2, FlameXTheme.Text.TITLE_Y, FlameXTheme.Text.TITLE_COLOR | 0xFF000000, false);
        String filter = list.filter;
        if(!filter.isEmpty()) {
            var font = MCVer.getFontRenderer();
            boolean anyMatches = !list.children().isEmpty();
            int filterTextWidth = font.width(filter);
            int filterTextHeight = font.lineHeight;
            int right = width - 3;
            int bottom = list.getBottom() - 3;
            graphics.fill(right - 2 - filterTextWidth - 2, bottom - 2 - filterTextHeight - 2, right, bottom, 0xFFFFFFFF);
            graphics.drawString(font, filter, right - 2 - filterTextWidth, bottom - 2 - filterTextHeight,
                    anyMatches ? 0xFF000000 : 0xFFAA0000, false);
        }
    }

    private static DynamicTexture defaultThumbnail;
    private static ResourceLocation defaultThumbnailLocation;
    private static final AtomicInteger TEXTURE_COUNTER = new AtomicInteger();

    private static ResourceLocation defaultThumbnail() {
        if(defaultThumbnailLocation == null) {
            defaultThumbnail = Utils.DEFAULT_THUMBNAIL.toTexture();
            defaultThumbnailLocation = identifier(SceneCraft.MOD_ID, "viewer/default_thumbnail");
            MCVer.getMinecraft().getTextureManager().register(defaultThumbnailLocation, defaultThumbnail);
        }
        return defaultThumbnailLocation;
    }

    public static class GuiReplayList extends ObjectSelectionList<GuiReplayEntry> {
        private File folder = null;
        private final List<GuiReplayEntry> all = new ArrayList<>();
        private final Set<GuiReplayEntry> selection = new LinkedHashSet<>();
        private GuiReplayEntry anchor;
        private long lastClickTime;
        private GuiReplayEntry lastClicked;
        private int generation;
        String filter = "";
        Runnable onSelectionChanged = () -> {};
        Runnable onDoubleClick = () -> {};

        GuiReplayList(GuiReplayViewer screen) {
            super(MCVer.getMinecraft(), 100, 100, 30, 100, 34);
            setRenderBackground(true);
            setRenderTopAndBottom(true);
        }

        public void setFolder(File folder) {
            this.folder = folder;
        }

        public List<GuiReplayEntry> getSelectedEntries() {
            return new ArrayList<>(selection);
        }

        @Override
        public int getRowWidth() {
            return 304;
        }

        @Override
        protected int getScrollbarPosition() {
            return width / 2 + getRowWidth() / 2 + 3;
        }

        @Override
        protected void renderSelection(@NotNull GuiGraphics graphics, int top, int width, int height, int outerColor, int innerColor) {
            super.renderSelection(graphics, top, width, height, 0xFFC0C0C0, 0xFF000000);
        }

        @Override
        protected boolean isSelectedItem(int index) {
            return index >= 0 && index < children().size() && selection.contains(children().get(index));
        }

        @Override
        public void setSelected(GuiReplayEntry entry) {
            selection.clear();
            if(entry != null) selection.add(entry);
            anchor = entry;
            onSelectionChanged.run();
        }

        void entryClicked(GuiReplayEntry entry) {
            long now = System.currentTimeMillis();
            boolean doubleClick = entry == lastClicked && now - lastClickTime < 250;
            lastClicked = entry;
            lastClickTime = now;
            if(Screen.hasControlDown()) {
                if(!selection.remove(entry)) selection.add(entry);
                anchor = entry;
            } else if(Screen.hasShiftDown() && anchor != null && children().contains(anchor)) {
                int a = children().indexOf(anchor);
                int b = children().indexOf(entry);
                selection.clear();
                for(int i = Math.min(a, b); i <= Math.max(a, b); i++) {
                    selection.add(children().get(i));
                }
            } else {
                selection.clear();
                selection.add(entry);
                anchor = entry;
            }
            onSelectionChanged.run();
            if(doubleClick && selection.size() == 1) {
                onDoubleClick.run();
            }
        }

        void setFilter(String filter) {
            this.filter = filter;
            applyFilter();
            setScrollAmount(0); // ensure we scroll to top if most entries are filtered
        }

        private void applyFilter() {
            String f = filter.toLowerCase();
            List<GuiReplayEntry> visible = all.stream()
                    .filter(e -> f.isEmpty() || e.getName().toLowerCase().contains(f))
                    .collect(Collectors.toList());
            replaceEntries(visible);
            selection.retainAll(visible);
            onSelectionChanged.run();
        }

        public void load() {
            final int gen = ++generation;
            for(GuiReplayEntry entry : all) entry.release();
            all.clear();
            selection.clear();
            replaceEntries(new ArrayList<>());
            onSelectionChanged.run();
            if(folder == null) return;
            Thread loader = new Thread(() -> loadAll(gen), "SceneCraft-ReplayList-Loader");
            loader.setDaemon(true);
            loader.start();
        }

        private void loadAll(int gen) {
            File[] files = folder.listFiles((FileFilter) new SuffixFileFilter(".mcpr", IOCase.INSENSITIVE));
            if(files == null) {
                LOGGER.warn("Failed to list files in {}", folder);
                return;
            }
            if(files.length == 0) {
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
                for(final File file : files) {
                    futures.add(pool.submit(() -> loadEntry(file, gen)));
                }
                for(Future<?> future : futures) {
                    try {
                        future.get();
                    } catch(ExecutionException e) {
                        LOGGER.error("Could not load Replay File", e.getCause());
                    }
                }
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                pool.shutdownNow();
            }
        }

        private void loadEntry(File file, int gen) {
            if(Thread.currentThread().isInterrupted() || gen != generation) {
                return;
            }
            try (ReplayFile replayFile = SceneCraft.instance.files.open(file.toPath())) {
                final boolean[] converted = {false};
                final Image thumb = replayFile.getThumbBytes().flatMap(stream -> {
                    try {
                        return Optional.of(Image.read(replayFile, stream, converted));
                    } catch(IOException e) {
                        if(!Thread.currentThread().isInterrupted()) {
                            e.printStackTrace();
                        }
                        return Optional.empty();
                    }
                }).orElse(null);
                if(converted[0]) {
                    replayFile.save();
                }

                final ReplayMetaData metaData = replayFile.getMetaData();
                List<RenderJob> renderQueue = RenderJob.readQueue(replayFile);

                if(Thread.currentThread().isInterrupted() || gen != generation) {
                    if(thumb != null) {
                        thumb.close();
                    }
                    return;
                }

                if(metaData != null) {
                    // Texture creation must happen on the main thread
                    MCVer.getMinecraft().execute(() -> {
                        if(gen != generation) {
                            if(thumb != null) thumb.close();
                            return;
                        }
                        GuiReplayEntry entry = new GuiReplayEntry(this, file, metaData, thumb, renderQueue);
                        all.add(entry);
                        all.sort(Comparator.naturalOrder());
                        applyFilter();
                    });
                } else if(thumb != null) {
                    thumb.close();
                }
            } catch(Exception e) {
                LOGGER.error("Could not load Replay File {}", file.getName(), e);
            }
        }
    }

    public static class GuiReplayEntry extends ObjectSelectionList.Entry<GuiReplayEntry> implements Comparable<GuiReplayEntry> {
        private static final int THUMB_H = 30;
        private static final int THUMB_W = THUMB_H * 16 / 9;

        public final File file;
        private final GuiReplayList list;
        private final String name;
        private final String server;
        private final int serverColor;
        private final String date;
        private final String version;
        private final String duration;
        private final long dateMillis;
        public final boolean incompatible;
        public final List<RenderJob> renderQueue;
        private final ResourceLocation thumbnailLocation;
        private final int thumbW;
        private final int thumbH;
        private boolean ownsThumbnail;

        public GuiReplayEntry(GuiReplayList list, File file, ReplayMetaData metaData, Image thumbImage, List<RenderJob> renderQueue) {
            this.list = list;
            this.file = file;
            this.renderQueue = renderQueue;
            this.name = Utils.fileNameToReplayName(file.getName());
            int color = 0xFFC0C0C0;
            if(!StringUtils.isEmpty(metaData.getCustomServerName())) {
                server = metaData.getCustomServerName();
            } else if(StringUtils.isEmpty(metaData.getServerName())
                    || !SceneCraft.instance.getSettingsRegistry().get(Setting.SHOW_SERVER_IPS)) {
                server = net.minecraft.client.resources.language.I18n.get("scenecraft.gui.iphidden");
                color = 0xFFAA0000;
            } else {
                server = metaData.getServerName();
            }
            serverColor = color;
            incompatible = !SceneCraft.isCompatible(metaData.getFileFormatVersion(), metaData.getRawProtocolVersionOr0());
            version = incompatible ? "Minecraft " + metaData.getMcVersion() : "";
            dateMillis = metaData.getDate();
            date = new SimpleDateFormat().format(new Date(dateMillis));
            duration = Utils.convertSecondsToShortString(metaData.getDuration() / 1000);
            if(thumbImage == null) {
                thumbnailLocation = defaultThumbnail();
                thumbW = Utils.DEFAULT_THUMBNAIL.getWidth();
                thumbH = Utils.DEFAULT_THUMBNAIL.getHeight();
            } else {
                DynamicTexture texture = thumbImage.toTexture();
                thumbW = thumbImage.getWidth();
                thumbH = thumbImage.getHeight();
                thumbnailLocation = identifier(SceneCraft.MOD_ID, "viewer/thumb_" + TEXTURE_COUNTER.incrementAndGet());
                MCVer.getMinecraft().getTextureManager().register(thumbnailLocation, texture);
                ownsThumbnail = true;
            }
        }

        public String getName() {
            return name;
        }

        void release() {
            if(ownsThumbnail && thumbnailLocation != null) {
                MCVer.getMinecraft().getTextureManager().release(thumbnailLocation);
                ownsThumbnail = false;
            }
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            var font = MCVer.getFontRenderer();
            graphics.blit(thumbnailLocation, left, top, THUMB_W, THUMB_H, 0f, 0f, thumbW, thumbH, thumbW, thumbH);
            int durationWidth = font.width(duration);
            graphics.fill(left + THUMB_W - durationWidth - 2, top + THUMB_H - font.lineHeight - 2, left + THUMB_W, top + THUMB_H, 0x80000000);
            graphics.drawString(font, duration, left + THUMB_W - durationWidth, top + THUMB_H - font.lineHeight, 0xFFFFFFFF, false);
            int x = left + THUMB_W + 5;
            graphics.drawString(font, ChatFormatting.UNDERLINE + name, x, top, 0xFFFFFFFF, false);
            graphics.drawString(font, server, x, top + font.lineHeight + 2, serverColor, false);
            graphics.drawString(font, date, x, top + 2 * (font.lineHeight + 2), 0xFFC0C0C0, false);
            if(!version.isEmpty()) {
                graphics.drawString(font, version, left + width - font.width(version), top, 0xFFFF0000, false);
            }
            if(!renderQueue.isEmpty()) {
                int ix = left + THUMB_W - 10;
                graphics.blit(SceneCraft.TEXTURE, ix, top, 10, 10, 40f, 0f, 20, 20, SceneCraft.TEXTURE_SIZE, SceneCraft.TEXTURE_SIZE);
                if(mouseX >= ix && mouseX < ix + 10 && mouseY >= top && mouseY < top + 10) {
                    Screen screen = MCVer.getMinecraft().screen;
                    if(screen != null) {
                        String text = renderQueue.stream().map(RenderJob::getName).collect(Collectors.joining("\n"));
                        screen.setTooltipForNextRenderPass(font.split(MCVer.literalText(text), 250));
                    }
                }
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if(button == 0) {
                list.entryClicked(this);
                return true;
            }
            return false;
        }

        @Override
        public @NotNull Component getNarration() {
            return MCVer.literalText(name);
        }

        @Override
        public int compareTo(GuiReplayEntry o) {
            return Long.compare(o.dateMillis, dateMillis);
        }
    }
}
