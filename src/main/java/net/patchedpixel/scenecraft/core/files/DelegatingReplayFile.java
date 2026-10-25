package net.patchedpixel.scenecraft.core.files;

import net.patchedpixel.scenecraft.scenestudio.data.Marker;
import net.patchedpixel.scenecraft.scenestudio.data.ModInfo;
import net.patchedpixel.scenecraft.scenestudio.data.ReplayAssetEntry;
import net.patchedpixel.scenecraft.scenestudio.io.ReplayInputStream;
import net.patchedpixel.scenecraft.scenestudio.io.ReplayOutputStream;
import net.patchedpixel.scenecraft.scenestudio.pathing.PathingRegistry;
import net.patchedpixel.scenecraft.scenestudio.pathing.path.Timeline;
import net.patchedpixel.scenecraft.scenestudio.protocol.PacketTypeRegistry;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayFile;
import net.patchedpixel.scenecraft.scenestudio.replay.ReplayMetaData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;

public class DelegatingReplayFile implements ReplayFile {
    private final ReplayFile delegate;

    public DelegatingReplayFile(ReplayFile delegate) {
        this.delegate = delegate;
    }

    @Override
    public Optional<InputStream> get(String entry) throws IOException {
        return this.delegate.get(entry);
    }

    @Override
    public Optional<InputStream> getCache(String entry) throws IOException {
        return this.delegate.getCache(entry);
    }

    @Override
    public Map<String, InputStream> getAll(Pattern pattern) throws IOException {
        return this.delegate.getAll(pattern);
    }

    @Override
    public OutputStream write(String entry) throws IOException {
        return this.delegate.write(entry);
    }

    @Override
    public OutputStream writeCache(String entry) throws IOException {
        return this.delegate.writeCache(entry);
    }

    @Override
    public void remove(String entry) throws IOException {
        this.delegate.remove(entry);
    }

    @Override
    public void removeCache(String entry) throws IOException {
        this.delegate.removeCache(entry);
    }

    @Override
    public void save() throws IOException {
        this.delegate.save();
    }

    @Override
    public void saveTo(File target) throws IOException {
        this.delegate.saveTo(target);
    }

    @Override
    public ReplayMetaData getMetaData() throws IOException {
        return this.delegate.getMetaData();
    }

    @Override
    public void writeMetaData(PacketTypeRegistry registry, ReplayMetaData metaData) throws IOException {
        this.delegate.writeMetaData(registry, metaData);
    }

    @Override
    public ReplayInputStream getPacketData(PacketTypeRegistry registry) throws IOException {
        return this.delegate.getPacketData(registry);
    }

    @Override
    public ReplayOutputStream writePacketData() throws IOException {
        return this.delegate.writePacketData();
    }

    @Override
    public Map<Integer, String> getResourcePackIndex() throws IOException {
        return this.delegate.getResourcePackIndex();
    }

    @Override
    public void writeResourcePackIndex(Map<Integer, String> index) throws IOException {
        this.delegate.writeResourcePackIndex(index);
    }

    @Override
    public Optional<InputStream> getResourcePack(String hash) throws IOException {
        return this.delegate.getResourcePack(hash);
    }

    @Override
    public OutputStream writeResourcePack(String hash) throws IOException {
        return this.delegate.writeResourcePack(hash);
    }

    @Override
    public Map<String, Timeline> getTimelines(PathingRegistry pathingRegistry) throws IOException {
        return this.delegate.getTimelines(pathingRegistry);
    }

    @Override
    public void writeTimelines(PathingRegistry pathingRegistry, Map<String, Timeline> timelines) throws IOException {
        this.delegate.writeTimelines(pathingRegistry, timelines);
    }

    @Override
    public Optional<InputStream> getThumbBytes() throws IOException {
        return this.delegate.getThumbBytes();
    }

    @Override
    public void writeThumbBytes(byte[] image) throws IOException {
        this.delegate.writeThumbBytes(image);
    }

    @Override
    public Optional<Set<UUID>> getInvisiblePlayers() throws IOException {
        return this.delegate.getInvisiblePlayers();
    }

    @Override
    public void writeInvisiblePlayers(Set<UUID> uuids) throws IOException {
        this.delegate.writeInvisiblePlayers(uuids);
    }

    @Override
    public Optional<Set<Marker>> getMarkers() throws IOException {
        return this.delegate.getMarkers();
    }

    @Override
    public void writeMarkers(Set<Marker> markers) throws IOException {
        this.delegate.writeMarkers(markers);
    }

    @Override
    public Collection<ReplayAssetEntry> getAssets() throws IOException {
        return this.delegate.getAssets();
    }

    @Override
    public Optional<InputStream> getAsset(UUID uuid) throws IOException {
        return this.delegate.getAsset(uuid);
    }

    @Override
    public OutputStream writeAsset(ReplayAssetEntry asset) throws IOException {
        return this.delegate.writeAsset(asset);
    }

    @Override
    public void removeAsset(UUID uuid) throws IOException {
        this.delegate.removeAsset(uuid);
    }

    @Override
    public Collection<ModInfo> getModInfo() throws IOException {
        return this.delegate.getModInfo();
    }

    @Override
    public void writeModInfo(Collection<ModInfo> modInfo) throws IOException {
        this.delegate.writeModInfo(modInfo);
    }

    @Override
    public void close() throws IOException {
        this.delegate.close();
    }
}
