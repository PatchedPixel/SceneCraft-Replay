package net.patchedpixel.scenecraft.common.pathing.gui;

import com.google.common.util.concurrent.SettableFuture;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.CrashReport;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.render.gui.GuiRenderQueue;
import net.patchedpixel.scenecraft.common.render.gui.GuiRenderSettings;
import net.patchedpixel.scenecraft.common.replay.ReplayHandler;
import net.patchedpixel.scenecraft.common.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.PathingRegistry;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.path.Path;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.path.Timeline;
import net.patchedpixel.scenecraft.common.scenestudio.pathing.serialize.TimelineSerialization;
import net.patchedpixel.scenecraft.common.scenestudio.replay.ReplayFile;
import net.patchedpixel.scenecraft.common.flamex.components.FlameableScreen;
import net.patchedpixel.scenecraft.common.flamex.FlameX;
import net.patchedpixel.scenecraft.common.flamex.popup.PromptScreen;
import net.patchedpixel.scenecraft.common.utils.Utils;
import net.minecraft.client.gui.layouts.LinearLayout;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.patchedpixel.scenecraft.common.core.versions.MCVer;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static net.patchedpixel.scenecraft.common.core.versions.MCVer.getClipboardString;
import static net.patchedpixel.scenecraft.common.core.versions.MCVer.setClipboardString;

public class GuiKeyframeRepository extends FlameableScreen {
    private static final Logger LOGGER = LogManager.getLogger();

    private Button overwriteButton, saveAsButton, loadButton, renameButton, removeButton, copyButton, pasteButton, addToQueueButton;
    private NameList list;

    private final Map<String, Timeline> timelines = new LinkedHashMap<>();
    private final Timeline currentTimeline;
    private final SettableFuture<Timeline> future = SettableFuture.create();
    private final PathingRegistry registry;
    private final ReplayFile replayFile;

    private final Set<String> selected = new LinkedHashSet<>();

    public GuiKeyframeRepository(PathingRegistry registry, ReplayFile replayFile, Timeline currentTimeline) throws IOException {
        super("scenecraft.gui.keyframerepository.title", null);
        this.background = Background.NONE;
        this.registry = registry;
        this.replayFile = replayFile;
        this.currentTimeline = currentTimeline;

        timelines.putAll(replayFile.getTimelines(registry));
    }

    @Override
    protected void build(LinearLayout root) {}

    @Override
    protected void init() {
        selected.retainAll(timelines.keySet());
        list = new NameList();
        for(String name : timelines.keySet()) {
            if(name.isEmpty()) continue;
            list.add(new NameEntry(name));
        }

        overwriteButton = FlameX.button("scenecraft.gui.overwrite", 75, this::overwrite);
        saveAsButton = FlameX.button("scenecraft.gui.saveas", 75, () ->
                MCVer.getMinecraft().setScreen(new PromptScreen(this, "scenecraft.gui.saveas", "",
                        "scenecraft.gui.save", "scenecraft.gui.cancel",
                        n -> !n.isEmpty() && !timelines.containsKey(n),
                        name -> {
                            timelines.put(name, currentTimeline);
                            save();
                            rebuildWidgets();
                        })));
        loadButton = FlameX.button("scenecraft.gui.load", 75, this::load);
        renameButton = FlameX.button("scenecraft.gui.rename", 75, this::rename);
        removeButton = FlameX.button("scenecraft.gui.remove", 75, () ->
                MCVer.getMinecraft().setScreen(FlameX.confirm(this, "scenecraft.gui.keyframerepo.delete",
                        "scenecraft.gui.delete", "scenecraft.gui.cancel", () -> {
                            for(String name : selected) {
                                timelines.remove(name);
                            }
                            selected.clear();
                            save();
                            rebuildWidgets();
                        })));
        copyButton = FlameX.button("scenecraft.gui.copy", 75, this::copy);
        pasteButton = FlameX.button("scenecraft.gui.paste", 75, this::paste);
        addToQueueButton = FlameX.button("scenecraft.gui.rendersettings.addtoqueue", 75, this::addToQueue);

        GridLayout grid = new GridLayout();
        grid.defaultCellSetting().padding(2);
        Button[] buttons = {overwriteButton, saveAsButton, renameButton, removeButton,
                loadButton, addToQueueButton, copyButton, pasteButton};
        for(int i = 0; i < buttons.length; i++) {
            grid.addChild(buttons[i], i / 4, i % 4);
        }
        grid.arrangeElements();
        grid.setPosition(width / 2 - grid.getWidth() / 2, height - 10 - grid.getHeight());

        list.updateSize(Math.min(width - 10, 400), grid.getY() - 10 - 35, 35, grid.getY() - 10);
        list.setLeftPos(width / 2 - list.getWidth() / 2);
        addRenderableWidget(list);
        grid.visitWidgets(this::addRenderableWidget);
        updateButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(5, 5, width - 5, height - 5, 0xC0000000);
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void overwrite() {
        MCVer.getMinecraft().setScreen(FlameX.confirm(this, "scenecraft.gui.keyframerepo.overwrite", "gui.yes", "gui.no", () -> {
            for(String name : selected) {
                timelines.put(name, currentTimeline);
            }
            overwriteButton.active = false;
            save();
        }));
    }

    private void load() {
        MCVer.getMinecraft().setScreen(null);
        try {
            Timeline timeline = timelines.get(selected.iterator().next());
            for(Path path : timeline.getPaths()) {
                path.updateAll();
            }
            future.set(timeline);
        } catch(Throwable t) {
            future.setException(t);
        }
    }

    private void rename() {
        String old = selected.iterator().next();
        MCVer.getMinecraft().setScreen(new PromptScreen(this, "scenecraft.gui.rename", old,
                "scenecraft.gui.done", "scenecraft.gui.cancel",
                n -> !n.isEmpty() && !timelines.containsKey(n),
                name -> {
                    Map<String, Timeline> reordered = new LinkedHashMap<>();
                    for(Map.Entry<String, Timeline> e : timelines.entrySet()) {
                        reordered.put(e.getKey().equals(old) ? name : e.getKey(), e.getValue());
                    }
                    timelines.clear();
                    timelines.putAll(reordered);
                    selected.clear();
                    selected.add(name);
                    save();
                    rebuildWidgets();
                }));
    }

    private void copy() {
        Map<String, Timeline> toBeSerialized = new HashMap<>();
        for(String name : selected) {
            toBeSerialized.put(name, timelines.get(name));
        }
        try {
            TimelineSerialization serialization = new TimelineSerialization(registry, null);
            setClipboardString(serialization.serialize(toBeSerialized));
        } catch(Throwable t) {
            t.printStackTrace();
            CrashReport report = CrashReport.forThrowable(t, "Copying timeline(s)");
            Utils.error(LOGGER, report, () -> {});
        }
    }

    private void paste() {
        try {
            TimelineSerialization serialization = new TimelineSerialization(registry, null);
            for(Map.Entry<String, Timeline> entry : serialization.deserialize(getClipboardString()).entrySet()) {
                String name = entry.getKey();
                while(timelines.containsKey(name)) {
                    name += " (Copy)";
                }
                timelines.put(name, entry.getValue());
            }
            save();
            rebuildWidgets();
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }

    private void addToQueue() {
        ReplayHandler replayHandler = SceneCraftReplay.instance.getReplayHandler();
        GuiRenderQueue queue = new GuiRenderQueue(this, replayHandler, null);
        queue.open();

        ArrayDeque<Map.Entry<String, Timeline>> toBeAdded = new ArrayDeque<>();
        for(Map.Entry<String, Timeline> entry : timelines.entrySet()) {
            if(selected.contains(entry.getKey())) {
                toBeAdded.offerLast(entry);
            }
        }
        new Runnable() {
            @Override
            public void run() {
                Map.Entry<String, Timeline> entry = toBeAdded.pollFirst();
                if(entry == null) {
                    return;
                }
                GuiRenderSettings settingsGui = queue.addJob(entry.getValue(), this);
                settingsGui.hideRenderButton();
                settingsGui.setOutputFileBaseName(entry.getKey());
                settingsGui.open();
            }
        }.run();
    }

    private void updateButtons() {
        if(loadButton == null) return;
        int count = selected.size();

        overwriteButton.active = count >= 1;
        loadButton.active = count == 1;
        renameButton.active = count == 1;
        removeButton.active = count >= 1;
        copyButton.active = count >= 1;
        addToQueueButton.active = count >= 1;
    }

    @Override
    public void display() {
        super.display();
        SceneCraftReplay.instance.getReplayHandler().getOverlay().setVisible(false);
    }

    @Override
    public void removed() {
        SceneCraftReplay.instance.getReplayHandler().getOverlay().setVisible(true);
    }

    public SettableFuture<Timeline> getFuture() {
        return future;
    }

    public void save() {
        try {
            replayFile.writeTimelines(registry, timelines);
        } catch(IOException e) {
            e.printStackTrace();
            SceneCraft.instance.printWarningToChat("Error saving timelines: " + e.getMessage());
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(Screen.hasControlDown()) {
            switch(keyCode) {
                case InputConstants.KEY_A:
                    if(selected.size() < timelines.size()) {
                        for(String name : timelines.keySet()) {
                            if(!name.isEmpty()) selected.add(name);
                        }
                    } else {
                        selected.clear();
                    }
                    updateButtons();
                    return true;
                case InputConstants.KEY_C:
                    if(copyButton.active) copy();
                    return true;
                case InputConstants.KEY_V:
                    paste();
                    return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private class NameList extends ObjectSelectionList<NameEntry> {
        NameList() {
            super(MCVer.getMinecraft(), 100, 100, 35, 100, 18);
            setRenderBackground(false);
            setRenderTopAndBottom(false);
        }

        void add(NameEntry e) {
            addEntry(e);
        }

        @Override
        public int getRowWidth() {
            return Math.min(getWidth() - 10, 380);
        }

        @Override
        protected boolean isSelectedItem(int index) {
            return index >= 0 && index < children().size() && selected.contains(children().get(index).name);
        }
    }

    public class NameEntry extends ObjectSelectionList.Entry<NameEntry> {
        private final String name;

        public NameEntry(String name) {
            this.name = name;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            if(selected.contains(name)) {
                graphics.fill(left, top, left + width, top + height, 0xFF000000);
                graphics.fill(left, top, left + 2, top + height, 0xFFFFFFFF);
            }
            graphics.drawString(MCVer.getFontRenderer(), name, left + 5, top + 4, 0xFFFFFFFF, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if(button != 0) return false;
            if(!Screen.hasControlDown()) {
                selected.clear();
            }
            if(!selected.remove(name)) {
                selected.add(name);
            }
            updateButtons();
            return true;
        }

        @Override
        public Component getNarration() {
            return MCVer.literalText(name);
        }
    }
}
