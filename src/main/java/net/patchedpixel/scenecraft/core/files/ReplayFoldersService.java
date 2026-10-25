package net.patchedpixel.scenecraft.core.files;

import com.google.common.net.PercentEscaper;
import net.patchedpixel.scenecraft.core.Setting;
import net.patchedpixel.scenecraft.core.SettingsRegistry;
import net.patchedpixel.scenecraft.core.versions.MCVer;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.patchedpixel.scenecraft.core.utils.Utils.ensureDirectoryExists;

public class ReplayFoldersService {
    private final Path mcDir = MCVer.getMinecraft().gameDirectory.toPath();
    private final SettingsRegistry settings;

    public ReplayFoldersService(SettingsRegistry settings) {
        this.settings = settings;
    }

    public Path getReplayFolder() throws IOException {
        return ensureDirectoryExists(mcDir.resolve(settings.get(Setting.RECORDING_PATH)));
    }

    /**
     * Folder into which replay backups are saved before the MarkerProcessor is unleashed.
     */
    public Path getRawReplayFolder() throws IOException {
        return ensureDirectoryExists(getReplayFolder().resolve("raw"));
    }

    /**
     * Folder into which replays are recorded.
     * Distinct from the main folder, so they cannot be opened while they are still saving.
     */
    public Path getRecordingFolder() throws IOException {
        return ensureDirectoryExists(getReplayFolder().resolve("record"));
    }

    /**
     * Folder in which replay cache files are stored.
     * Distinct from the record folder cause people kept confusing them with recordings.
     */
    public Path getCacheFolder() throws IOException {
        Path path = ensureDirectoryExists(mcDir.resolve(settings.get(Setting.CACHE_PATH)));
        try {
            Files.setAttribute(path, "dos:hidden", true);
        } catch (UnsupportedOperationException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }

        return path;
    }

    private static final PercentEscaper CACHE_FILE_NAME_ENCODER = new PercentEscaper("-_ ", false);

    public Path getCachePathForReplay(Path replay) throws IOException {
        Path replayFolder = getReplayFolder();
        Path cacheFolder = getCacheFolder();
        Path relative = replayFolder.toAbsolutePath().relativize(replay.toAbsolutePath());

        return cacheFolder.resolve(CACHE_FILE_NAME_ENCODER.escape(relative.toString()));
    }

    public Path getReplayPathForCache(Path cache) throws IOException {
        String relative = URLDecoder.decode(cache.getFileName().toString(), StandardCharsets.UTF_8);
        Path replayFolder = getReplayFolder();

        return replayFolder.resolve(relative);
    }
}
