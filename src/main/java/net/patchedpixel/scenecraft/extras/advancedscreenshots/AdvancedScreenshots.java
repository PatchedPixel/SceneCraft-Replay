package net.patchedpixel.scenecraft.extras.advancedscreenshots;

import net.patchedpixel.scenecraft.core.SceneCraft;
import net.patchedpixel.scenecraft.extras.Extra;

public class AdvancedScreenshots implements Extra {
    private SceneCraft mod;

    @Override
    public void register(SceneCraft mod) {
        this.mod = mod;
    }

    private static AdvancedScreenshots instance; { instance = this; }
    public static void take() {
        if (instance != null) {
            instance.takeScreenshot();
        }
    }

    private void takeScreenshot() {
        SceneCraft.instance.runLater(() -> new GuiCreateScreenshot(mod).open());
    }
}
