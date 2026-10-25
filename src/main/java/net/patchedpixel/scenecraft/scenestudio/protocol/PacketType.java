package net.patchedpixel.scenecraft.scenestudio.protocol;

/**
 * The packet types the scenestudio knows about. This is intentionally not exhaustive, it only contains what the
 * filters and the random access replay need. Every other packet is handled as {@link #UnknownPlay} (or its
 * login / configuration counterpart).
 * <p>
 * Packet ids are not stored here. They are looked up by the vanilla packet name in the protocol of the running
 * game, see {@link VanillaPacketIds}. A type without a vanilla name has been removed from the protocol; such a type
 * is never produced when reading and only remains so code that still mentions it keeps compiling.
 */
public enum PacketType {
    UnknownLogin(State.LOGIN, null, true),
    UnknownConfiguration(State.CONFIGURATION, null, true),
    UnknownPlay(State.PLAY, null, true),
    LoginSuccess(State.LOGIN, "game_profile"),
    ConfigCustomPayload(State.CONFIGURATION, null),
    ConfigDisconnect(State.CONFIGURATION, null),
    ConfigFinish(State.CONFIGURATION, null),
    ConfigKeepAlive(State.CONFIGURATION, null),
    ConfigPing(State.CONFIGURATION, null),
    ConfigRegistries(State.CONFIGURATION, null),
    ConfigFeatures(State.CONFIGURATION, null),
    ConfigTags(State.CONFIGURATION, null),
    ConfigSelectKnownPacks(State.CONFIGURATION, null),
    KeepAlive(State.PLAY, "keep_alive"),
    JoinGame(State.PLAY, "login"),
    Chat(State.PLAY, "player_chat"),
    UpdateTime(State.PLAY, "set_time"),
    EntityEquipment(State.PLAY, "set_equipment"),
    SpawnPosition(State.PLAY, "set_default_spawn_position"),
    UpdateHealth(State.PLAY, "set_health"),
    Respawn(State.PLAY, "respawn"),
    PlayerPositionRotation(State.PLAY, "player_position"),
    ChangeHeldItem(State.PLAY, "set_carried_item"),
    PlayerUseBed(State.PLAY, null),
    EntityAnimation(State.PLAY, "animate"),
    SpawnPlayer(State.PLAY, "add_player"),
    EntityCollectItem(State.PLAY, "take_item_entity"),
    SpawnObject(State.PLAY, "add_entity"),
    SpawnMob(State.PLAY, null),
    SpawnPainting(State.PLAY, null),
    SpawnExpOrb(State.PLAY, "add_experience_orb"),
    EntityVelocity(State.PLAY, "set_entity_motion"),
    DestroyEntities(State.PLAY, "remove_entities"),
    EntityMovement(State.PLAY, null),
    EntityPosition(State.PLAY, "move_entity_pos"),
    EntityRotation(State.PLAY, "move_entity_rot"),
    EntityPositionRotation(State.PLAY, "move_entity_pos_rot"),
    EntityTeleport(State.PLAY, "teleport_entity"),
    EntityPositionSync(State.PLAY, null),
    EntityHeadLook(State.PLAY, "rotate_head"),
    EntityStatus(State.PLAY, "entity_event"),
    EntityAttach(State.PLAY, "set_entity_link"),
    EntityMetadata(State.PLAY, "set_entity_data"),
    EntityEffect(State.PLAY, "update_mob_effect"),
    EntityRemoveEffect(State.PLAY, "remove_mob_effect"),
    SetExperience(State.PLAY, "set_experience"),
    EntityProperties(State.PLAY, "update_attributes"),
    ChunkData(State.PLAY, "level_chunk_with_light"),
    MultiBlockChange(State.PLAY, "section_blocks_update"),
    BlockChange(State.PLAY, "block_update"),
    BlockValue(State.PLAY, "block_event"),
    BlockBreakAnim(State.PLAY, "block_destruction"),
    BulkChunkData(State.PLAY, null),
    Explosion(State.PLAY, "explode"),
    PlayEffect(State.PLAY, "level_event"),
    PlaySound(State.PLAY, "sound"),
    SpawnParticle(State.PLAY, "level_particles"),
    NotifyClient(State.PLAY, "game_event"),
    SpawnGlobalEntity(State.PLAY, null),
    OpenWindow(State.PLAY, "open_screen"),
    CloseWindow(State.PLAY, "container_close"),
    SetSlot(State.PLAY, "container_set_slot"),
    WindowItems(State.PLAY, "container_set_content"),
    WindowProperty(State.PLAY, "container_set_data"),
    ConfirmTransaction(State.PLAY, null),
    UpdateSign(State.PLAY, null),
    MapData(State.PLAY, "map_item_data"),
    UpdateTileEntity(State.PLAY, "block_entity_data"),
    OpenTileEntityEditor(State.PLAY, "open_sign_editor"),
    Statistics(State.PLAY, "award_stats"),
    PlayerListEntry(State.PLAY, "player_info_update"),
    PlayerAbilities(State.PLAY, "player_abilities"),
    TabComplete(State.PLAY, "command_suggestions"),
    ScoreboardObjective(State.PLAY, "set_objective"),
    UpdateScore(State.PLAY, "set_score"),
    DisplayScoreboard(State.PLAY, "set_display_objective"),
    Team(State.PLAY, "set_player_team"),
    PluginMessage(State.PLAY, "custom_payload"),
    Disconnect(State.PLAY, "disconnect"),
    Difficulty(State.PLAY, "change_difficulty"),
    Combat(State.PLAY, null),
    SwitchCamera(State.PLAY, "set_camera"),
    WorldBorder(State.PLAY, "initialize_border"),
    EntityNBTUpdate(State.PLAY, null),
    UnloadChunk(State.PLAY, "forget_level_chunk"),
    SetPassengers(State.PLAY, "set_passengers"),
    OpenHorseWindow(State.PLAY, "horse_screen_open"),
    UpdateLight(State.PLAY, "light_update"),
    TradeList(State.PLAY, "merchant_offers"),
    UpdateViewPosition(State.PLAY, "set_chunk_cache_center"),
    UpdateViewDistance(State.PLAY, "set_chunk_cache_radius"),
    EntitySoundEffect(State.PLAY, "sound_entity"),
    Tags(State.PLAY, "update_tags"),
    PlayerActionAck(State.PLAY, "block_changed_ack"),
    CombatEnd(State.PLAY, "player_combat_end"),
    CombatEnter(State.PLAY, "player_combat_enter"),
    CombatEntityDead(State.PLAY, "player_combat_kill"),
    DestroyEntity(State.PLAY, null),
    UpdateSimulationDistance(State.PLAY, "set_simulation_distance"),
    Features(State.PLAY, "update_enabled_features"),
    PlayerListEntryRemove(State.PLAY, "player_info_remove"),
    Bundle(State.PLAY, "bundle_delimiter"),
    Reconfigure(State.PLAY, null),
    ResetScore(State.PLAY, null);

    private final State state;
    private final String vanillaName;
    private final boolean unknown;

    PacketType(State state, String vanillaName) {
        this(state, vanillaName, false);
    }

    PacketType(State state, String vanillaName, boolean unknown) {
        this.state = state;
        this.vanillaName = vanillaName;
        this.unknown = unknown;
    }

    public State getState() {
        return state;
    }

    /**
     * @return The path of the vanilla packet id (e.g. {@code add_entity}) or null if the packet no longer exists
     */
    public String getVanillaName() {
        return vanillaName;
    }

    /**
     * @return Whether this is one of the placeholder types used for packets the scenestudio does not know
     */
    public boolean isUnknown() {
        return unknown;
    }

    /**
     * @return Whether this packet exists in the current protocol
     */
    public boolean existsInCurrentProtocol() {
        return unknown || vanillaName != null;
    }
}
