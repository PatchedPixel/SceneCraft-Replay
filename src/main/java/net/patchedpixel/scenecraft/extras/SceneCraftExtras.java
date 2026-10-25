package net.patchedpixel.scenecraft.extras;

import net.patchedpixel.scenecraft.core.Module;
import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.extras.advancedscreenshots.AdvancedScreenshots;
import net.patchedpixel.scenecraft.extras.playeroverview.PlayerOverview;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SceneCraftExtras implements Module {
    { instance = this; }
    public static SceneCraftExtras instance;

    private static final List<Class<? extends Extra>> builtin = Arrays.asList(
            AdvancedScreenshots.class,
            PlayerOverview.class,
            FullBrightness.class,
            QuickMode.class,
            HotkeyButtons.class
    );

    private final Map<Class<? extends Extra>, Extra> instances = new HashMap<>();

    public static Logger LOGGER = LogManager.getLogger();

    public SceneCraftExtras(SceneCraft core) {
        core.getSettingsRegistry().register(Setting.class);
    }

    @Override
    public void initClient() {
        for (Class<? extends Extra> cls : builtin) {
            try {
                Extra extra = cls.newInstance();
                extra.register(SceneCraft.instance);
                instances.put(cls, extra);
            } catch (Throwable t) {
                LOGGER.warn("Failed to load extra " + cls.getName() + ": ", t);
            }
        }
    }

    public <T extends Extra> Optional<T> get(Class<T> cls) {
        return Optional.ofNullable(instances.get(cls)).map(cls::cast);
    }
}
