package net.patchedpixel.scenecraft.common.extras.advancedscreenshots;

import net.patchedpixel.scenecraft.common.SceneCraft;
import net.patchedpixel.scenecraft.common.extras.Extra;

public class AdvancedScreenshots implements Extra {
    private SceneCraft mod;

    @Override
    public void register(SceneCraft mod) {
        this.mod = mod;
    }

    private static AdvancedScreenshots instance; { instance = this; }
    public static void take() {
        if(instance != null) {
            instance.takeScreenshot();
        }
    }

    private void takeScreenshot() {
        SceneCraft.instance.runLater(() -> new GuiCreateScreenshot(mod).open());
    }
}
