package net.patchedpixel.scenecraft.editor;

import net.patchedpixel.scenecraft.core.Module;
import net.patchedpixel.scenecraft.core.SceneCraft;
import net.patchedpixel.scenecraft.editor.handler.GuiHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SceneCraftEditor implements Module {
    { instance = this; }
    public static SceneCraftEditor instance;

    private SceneCraft core;

    public static Logger LOGGER = LogManager.getLogger();

    public SceneCraftEditor(SceneCraft core) {
        this.core = core;
    }

    @Override
    public void initClient() {
        new GuiHandler().register();
    }

    public SceneCraft getCore() {
        return core;
    }
}
