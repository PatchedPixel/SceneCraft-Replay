package net.patchedpixel.scenecraft.common.editor.handler;

import net.minecraft.CrashReport;
import net.patchedpixel.scenecraft.common.editor.SceneCraftEditor;
import net.patchedpixel.scenecraft.common.editor.gui.GuiEditReplay;
import net.patchedpixel.scenecraft.common.replay.gui.screen.GuiReplayViewer;
import net.patchedpixel.scenecraft.common.flamex.event.EventRegistrations;
import net.patchedpixel.scenecraft.common.utils.Utils;

import java.io.IOException;

public class GuiHandler extends EventRegistrations {
    private static GuiReplayViewer.ExtraButton button;

    @Override
    public void register() {
        super.register();
        // Add the Edit button to the replay viewer
        if(button == null) {
            button = new GuiReplayViewer.ExtraButton("scenecraft.gui.edit", (replayViewer, file) -> {
                try {
                    new GuiEditReplay(replayViewer, file.toPath()) {
                        @Override
                        public void close() {
                            super.close();
                            replayViewer.list.load();
                        }
                    }.display();
                } catch(IOException e) {
                    Utils.error(SceneCraftEditor.LOGGER, CrashReport.forThrowable(e, "Opening replay editor"), () -> {});
                }
            });
            GuiReplayViewer.EXTRA_BUTTONS.add(button);
        }
    }

    @Override
    public void unregister() {
        super.unregister();
        GuiReplayViewer.EXTRA_BUTTONS.remove(button);
        button = null;
    }
}
