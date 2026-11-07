package net.patchedpixel.scenecraft.common.record.packet;

import com.google.common.hash.Hashing;
import net.patchedpixel.scenecraft.common.scenestudio.replay.ReplayFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.Connection;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
            synchronized(replayFile) { // Need to read, modify and write the resource pack index atomically
                Map<Integer, String> index = replayFile.getResourcePackIndex();
                if(index == null) {
                    index = new HashMap<>();
                }
                if(!index.containsValue(hash)) {
                    // Hash is unknown, we have to write the resource pack ourselves
                    doWrite = true;
                }
                // Save this request
                index.put(requestId, hash);
                replayFile.writeResourcePackIndex(index);
            }
            if(doWrite) {
                try (OutputStream out = replayFile.writeResourcePack(hash)) {
                    out.write(bytes);
                }
            }
        } catch(IOException e) {
            logger.warn("Failed to save resource pack.", e);
        }
    }

    private final Map<UUID, Integer> mcIdToReplayId = new HashMap<>();

    public synchronized ClientboundResourcePackPushPacket handleResourcePack(Connection netManager, ClientboundResourcePackPushPacket packet) {
        final int requestId = nextRequestId++;
        mcIdToReplayId.put(packet.id(), requestId);
        return new ClientboundResourcePackPushPacket(packet.id(), "replay://" + requestId, "", packet.required(), packet.prompt());
    }

    public void recordResourcePack(Path file, UUID uuid) {
        Integer id = mcIdToReplayId.get(uuid);
        if(id == null) {
            logger.warn("Got resource pack download for unexpected uuid " + uuid + " at " + file);
            return;
        }
        recordResourcePack(file, id);
    }
}
