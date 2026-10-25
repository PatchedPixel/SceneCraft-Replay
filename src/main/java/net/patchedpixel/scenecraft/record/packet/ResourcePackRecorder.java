package net.patchedpixel.scenecraft.record.packet;

import com.google.common.hash.Hashing;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Records resource packs and handles incoming resource pack packets during record.
 */
public class ResourcePackRecorder {
    private static final Logger logger = LogManager.getLogger();

    private final ReplayFile replayFile;

    private int nextRequestId;

    public ResourcePackRecorder(ReplayFile replayFile) {
        this.replayFile = replayFile;
    }

    public void recordResourcePack(Path file, int requestId) {
        try {
            // Read in resource pack file
            byte[] bytes = Files.readAllBytes(file);
            // Check whether it is already known
            String hash = Hashing.sha1().hashBytes(bytes).toString();
            boolean doWrite = false; // Whether we are the first and have to write it
            synchronized (replayFile) { // Need to read, modify and write the resource pack index atomically
                Map<Integer, String> index = replayFile.getResourcePackIndex();
                if (index == null) {
                    index = new HashMap<>();
                }
                if (!index.containsValue(hash)) {
                    // Hash is unknown, we have to write the resource pack ourselves
                    doWrite = true;
                }
                // Save this request
                index.put(requestId, hash);
                replayFile.writeResourcePackIndex(index);
            }
            if (doWrite) {
                try (OutputStream out = replayFile.writeResourcePack(hash)) {
                    out.write(bytes);
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to save resource pack.", e);
        }
    }

    private final Map<String, Integer> urlToReplayId = new HashMap<>();

    public synchronized ClientboundResourcePackPacket handleResourcePack(ClientboundResourcePackPacket packet) {
        final int requestId = nextRequestId++;
        urlToReplayId.put(packet.getUrl(), requestId);
        return new ClientboundResourcePackPacket("replay://" + requestId, "", packet.isRequired(), packet.getPrompt());
    }

    public synchronized void recordResourcePack(Path file, String url) {
        Integer id = url == null ? null : urlToReplayId.get(url);
        if (id == null) {
            logger.warn("Got resource pack download for unexpected url " + url + " at " + file);
            return;
        }
        recordResourcePack(file, id);
    }
}
