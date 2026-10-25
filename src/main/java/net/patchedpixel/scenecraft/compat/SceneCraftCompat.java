package net.patchedpixel.scenecraft.compat;

import de.maxhenkel.voicechat.VoicechatClient;
import net.patchedpixel.scenecraft.core.Module;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;

public class SceneCraftCompat implements Module {
    public static Logger LOGGER = LogManager.getLogger();

    private static final String MARKER_NAME = "scenecraft-voicechat-hud.marker";

    private static volatile boolean minimapHide;
    private static boolean voicechatChanged;
    private static boolean voicechatOriginalHidden;
    private static boolean hooked;

    public static boolean shouldHide() {
        return minimapHide;
    }

    public static synchronized void hideModHud() {
        minimapHide = true;
        if (!isLoaded("voicechat") || voicechatChanged) {
            return;
        }
        try {
            voicechatOriginalHidden = VoicechatClient.CLIENT_CONFIG.hideIcons.get();
            if (voicechatOriginalHidden) {
                return; // user already hides them, nothing to change or restore
            }
            installHook();
            writeMarker(voicechatOriginalHidden);
            voicechatChanged = true;
            VoicechatClient.CLIENT_CONFIG.hideIcons.set(true);
        } catch (Throwable t) {
            LOGGER.warn("Could not hide Simple Voice Chat icons", t);
        }
    }

    public static synchronized void showModHud() {
        minimapHide = false;
        if (!voicechatChanged) {
            return;
        }
        try {
            VoicechatClient.CLIENT_CONFIG.hideIcons.set(voicechatOriginalHidden);;
            voicechatChanged = false;
            deleteMarker();
        } catch (Throwable t) {
            LOGGER.warn("Could not restore Simple Voice Chat icons", t);
        }
    }

    /**
     * Restores the voice chat option if a previous session ended without restoring it.
     * Safe to call repeatedly; does nothing in the common case.
     */
    public static synchronized void recoverStaleHud() {
        if (voicechatChanged || !isLoaded("voicechat")) {
            return;
        }
        try {
            if (Files.exists(markerPath())) {
                VoicechatClient.CLIENT_CONFIG.hideIcons.set(readMarker());
                deleteMarker();
                LOGGER.info("Restored Simple Voice Chat icons left hidden by an earlier replay session");
            }
        } catch (Throwable t) {
            LOGGER.debug("Voice chat not ready for HUD recovery yet", t);
        }
    }

    private static boolean isLoaded(String modId) {
        try {
            return ModList.get() != null && ModList.get().isLoaded(modId);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void installHook() {
        if (hooked) return;
        hooked = true;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                showModHud();
            } catch (Throwable ignored) {
            }
        }, "scenecraft-hud-restore"));
    }

    private static Path markerPath() {
        return FMLPaths.CONFIGDIR.get().resolve(MARKER_NAME);
    }

    private static void writeMarker(boolean originalHidden) {
        try {
            Files.writeString(markerPath(), "originalHidden=" + originalHidden + "\n");
        } catch (Exception e) {
            LOGGER.warn("Could not write HUD marker", e);
        }
    }

    private static boolean readMarker() {
        try {
            return Files.readString(markerPath()).contains("originalHidden=true");
        } catch (Exception e) {
            return false;
        }
    }

    private static void deleteMarker() {
        try {
            Files.deleteIfExists(markerPath());
        } catch (Exception e) {
            LOGGER.warn("Could not delete HUD marker", e);
        }
    }

    @Override
    public void initClient() {
        recoverStaleHud();
    }
}
