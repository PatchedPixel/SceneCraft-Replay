package net.patchedpixel.scenecraft;

import net.patchedpixel.scenecraft.compat.SceneCraftCompat;
import net.patchedpixel.scenecraft.core.Module;
import net.patchedpixel.scenecraft.core.Setting;
import net.patchedpixel.scenecraft.core.SettingsRegistry;
import net.patchedpixel.scenecraft.core.files.ReplayFilesService;
import net.patchedpixel.scenecraft.core.files.ReplayFoldersService;
import net.patchedpixel.scenecraft.core.gui.GuiBackgroundProcesses;
import net.patchedpixel.scenecraft.core.gui.GuiReplaySettings;
import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.patchedpixel.scenecraft.core.versions.scheduler.Scheduler;
import net.patchedpixel.scenecraft.core.versions.scheduler.SchedulerImpl;
import net.patchedpixel.scenecraft.editor.SceneCraftEditor;
import net.patchedpixel.scenecraft.extras.SceneCraftExtras;
import net.patchedpixel.scenecraft.platform.KeyBindingRegistry;
import net.patchedpixel.scenecraft.platform.SceneCraftPlatform;
import net.patchedpixel.scenecraft.record.SceneCraftRecorder;
import net.patchedpixel.scenecraft.render.SceneCraftRender;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.scenestudio.studio.SceneStudio;
import net.patchedpixel.scenecraft.scenestudio.util.I18n;
import net.patchedpixel.scenecraft.simplepathing.SceneCraftSimplePathing;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static net.patchedpixel.scenecraft.core.versions.MCVer.identifier;

public class SceneCraft implements net.patchedpixel.scenecraft.core.Module, Scheduler {
    public static final String MOD_ID = "scenecraft";

    public static final ResourceLocation TEXTURE = identifier(SceneCraft.MOD_ID, "replay_gui.png");
    public static final int TEXTURE_SIZE = 256;

    private static final Minecraft mc = MCVer.getMinecraft();

    private final SceneCraftPlatform backend;
    private final SchedulerImpl scheduler = new SchedulerImpl();
    private final KeyBindingRegistry keyBindingRegistry = new KeyBindingRegistry();
    private final SettingsRegistry settingsRegistry = new SettingsRegistry();
    { settingsRegistry.register(Setting.class); }

    { instance = this; }
    public static SceneCraft instance;

    private final List<net.patchedpixel.scenecraft.core.Module> modules = new ArrayList<>();

    private final GuiBackgroundProcesses backgroundProcesses = new GuiBackgroundProcesses();
    public final ReplayFoldersService folders = new ReplayFoldersService(settingsRegistry);
    public final ReplayFilesService files = new ReplayFilesService(folders);

    public SceneCraft(SceneCraftPlatform backend) {
        this.backend = backend;

        I18n.setI18n(net.minecraft.client.resources.language.I18n::get);

        // Register all modules
        modules.add(this);
        modules.add(new SceneCraftRecorder(this));
        SceneCraftReplay replayModule = new SceneCraftReplay(this);
        modules.add(replayModule);
        modules.add(new SceneCraftRender(this));
        modules.add(new SceneCraftSimplePathing(this));
        modules.add(new SceneCraftEditor(this));
        modules.add(new SceneCraftExtras(this));
        modules.add(new SceneCraftCompat());

        settingsRegistry.register();
    }

    public KeyBindingRegistry getKeyBindingRegistry() {
        return keyBindingRegistry;
    }

    public SettingsRegistry getSettingsRegistry() {
        return settingsRegistry;
    }

    public void registerModuleKeyBindings() {
        modules.forEach(m -> m.registerKeyBindings(keyBindingRegistry));
    }

    public void initModules() {
        modules.forEach(net.patchedpixel.scenecraft.core.Module::initCommon);
        modules.forEach(Module::initClient);
    }

    @Override
    public void registerKeyBindings(KeyBindingRegistry registry) {
        registry.registerKeyBinding("scenecraft.input.settings", 0, () -> {
            new GuiReplaySettings(null, settingsRegistry).display();
        }, false);
    }

    @Override
    public void initClient() {
        backgroundProcesses.register();
        keyBindingRegistry.register();

        runPostStartup(() -> files.initialScan(this));
    }

    @Override
    public void runSync(Runnable runnable) throws InterruptedException, ExecutionException, TimeoutException {
        scheduler.runSync(runnable);
    }

    @Override
    public void runPostStartup(Runnable runnable) {
        scheduler.runPostStartup(runnable);
    }

    @Override
    public void runLaterWithoutLock(Runnable runnable) {
        scheduler.runLaterWithoutLock(runnable);
    }

    @Override
    public void runLater(Runnable runnable) {
        scheduler.runLater(runnable);
    }

    @Override
    public void runTasks() {
        scheduler.runTasks();
    }

    public String getVersion() {
        return backend.getVersion();
    }

    public String getMinecraftVersion() {
        return backend.getMinecraftVersion();
    }

    public Minecraft getMinecraft() {
        return mc;
    }

    public void printInfoToChat(String message, Object... args) {
        printToChat(false, message, args);
    }

    public void printWarningToChat(String message, Object... args) {
        printToChat(true, message, args);
    }

    private void printToChat(boolean warning, String message, Object... args) {
        if (!mc.isSameThread()) {
            runLater(() -> printToChat(warning, message, args));
            return;
        }
        if (getSettingsRegistry().get(Setting.NOTIFICATIONS)) {
            // Some nostalgia: "§8[§6Replay Mod§8]§r Your message goes here"
            Style coloredDarkGray = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
            Style coloredGold = Style.EMPTY.withColor(ChatFormatting.GOLD);
            Style alert = Style.EMPTY.withColor(warning ? ChatFormatting.RED : ChatFormatting.DARK_GREEN);
            Component text = Component.literal("[").setStyle(coloredDarkGray)
                    .append(Component.translatable("scenecraft.title").setStyle(coloredGold))
                    .append(Component.literal("] "))
                    .append(Component.translatable(message, args).setStyle(alert));
            // Send message to chat GUI
            // The in game GUI is initialized at startup, therefore this is possible before the client is connected
            mc.gui.getChat().addMessage(text);
        }
    }

    public GuiBackgroundProcesses getBackgroundProcesses() {
        return backgroundProcesses;
    }

    public static boolean isCompatible(int fileFormatVersion, int protocolVersion) {
        return new SceneStudio().isCompatible(fileFormatVersion, protocolVersion, MCVer.getProtocolVersion());
    }
}
