package net.patchedpixel.scenecraft.replay.camera;

import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.patchedpixel.scenecraft.platform.KeyBindingRegistry;
import net.patchedpixel.scenecraft.SceneCraft;
import net.patchedpixel.scenecraft.core.SettingsRegistry;
import net.patchedpixel.scenecraft.core.events.KeyBindingEventCallback;
import net.patchedpixel.scenecraft.core.events.PreRenderCallback;
import net.patchedpixel.scenecraft.core.events.PreRenderHandCallback;
import net.patchedpixel.scenecraft.core.events.SettingsChangedCallback;
import net.patchedpixel.scenecraft.replay.ReplayHandler;
import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.patchedpixel.scenecraft.replay.events.RenderHotbarCallback;
import net.patchedpixel.scenecraft.replay.events.RenderSpectatorCrosshairCallback;
import net.patchedpixel.scenecraft.gui.utils.EventRegistrations;
import net.patchedpixel.scenecraft.gui.versions.callbacks.PreTickCallback;
import net.patchedpixel.scenecraft.platform.Utils;
import net.patchedpixel.scenecraft.replay.Setting;
import net.patchedpixel.scenecraft.scenestudio.util.Location;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;

import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.world.entity.Avatar;

import net.minecraft.world.entity.player.Input;

import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.util.Mth;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.tags.TagKey;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.PlayerModelPart;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static net.patchedpixel.scenecraft.core.versions.MCVer.*;

/**
 * The camera entity used as the main player entity during replay viewing.
 * During a replay the player should be an instance of this class.
 * Camera movement is controlled by a separate {@link CameraController}.
 */
@SuppressWarnings("EntityConstructor")
public class CameraEntity
        extends LocalPlayer
{
    private static final UUID CAMERA_UUID = UUID.nameUUIDFromBytes("SceneCraftCamera".getBytes(StandardCharsets.UTF_8));

    /**
     * Roll of this camera in degrees.
     */
    public float roll;

    private CameraController cameraController;

    private long lastControllerUpdate = System.currentTimeMillis();

    /**
     * The entity whose hand was the last one rendered.
     */
    private Entity lastHandRendered = null;

    /**
     * The hashCode and equals methods of Entity are not stable.
     * Therefore we cannot register any event handlers directly in the CameraEntity class and
     * instead have this inner class.
     */
    private EventHandler eventHandler = new EventHandler();

    public CameraEntity(
            Minecraft mcIn,
            ClientLevel worldIn,
            ClientPacketListener netHandlerPlayClient,
            StatsCounter statisticsManager
            , ClientRecipeBook recipeBook
            , net.minecraft.client.player.ItemActivation itemActivation
    ) {
        super(mcIn,
                worldIn,
                netHandlerPlayClient,
                statisticsManager
                , recipeBook
                , Input.EMPTY
                , false
                , mcIn.computeChatAbilities()
                , itemActivation
        );
        setUUID(CAMERA_UUID);
        eventHandler.register();
        if (SceneCraftReplay.instance.getReplayHandler().getSpectatedUUID() == null) {
            cameraController = SceneCraftReplay.instance.createCameraController(this);
        } else {
            cameraController = new SpectatorCameraController(this);
        }
    }

    public CameraController getCameraController() {
        return cameraController;
    }

    public void setCameraController(CameraController cameraController) {
        this.cameraController = cameraController;
    }

    /**
     * Moves the camera by the specified delta.
     * @param x Delta in X direction
     * @param y Delta in Y direction
     * @param z Delta in Z direction
     */
    public void moveCamera(double x, double y, double z) {
        setCameraPosition(this.getX() + x, this.getY() + y, this.getZ() + z);
    }

    /**
     * Set the camera position.
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public void setCameraPosition(double x, double y, double z) {
        this.xOld = this.xo = x;
        this.yOld = this.yo = y;
        this.zOld = this.zo = z;
        this.setPosRaw(x, y, z);
        updateBoundingBox();
    }

    /**
     * Sets the camera rotation.
     * @param yaw Yaw in degrees
     * @param pitch Pitch in degrees
     * @param roll Roll in degrees
     */
    public void setCameraRotation(float yaw, float pitch, float roll) {
        // Note: MC's `setPitch` now forces values into the [-90; 90] range, however the math it uses is incorrect, so
        //       we need to wrap our value into the [-180; 180] range first to get correct results.
        pitch = Mth.wrapDegrees(pitch);
        this.yRotO = yaw;
        this.xRotO = pitch;
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.roll = roll;
    }

    /**
     * Sets the camera position and rotation to that of the specified AdvancedPosition
     * @param pos The position and rotation to set
     */
    public void setCameraPosRot(Location pos) {
        setCameraRotation(pos.getYaw(), pos.getPitch(), roll);
        setCameraPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Sets the camera position and rotation to that of the specified entity.
     * @param to The entity whose position to copy
     */
    public void setCameraPosRot(Entity to) {
        if (to == this) return;
        float yOffset = 0;
        this.xo = to.xo;
        this.yo = to.yo + yOffset;
        this.zo = to.zo;
        this.yRotO = to.yRotO;
        this.xRotO = to.xRotO;
        this.setPosRaw(to.getX(), to.getY(), to.getZ());
        this.setYRot(to.getYRot());
        this.setXRot(to.getXRot());
        this.xOld = to.xOld;
        this.yOld = to.yOld + yOffset;
        this.zOld = to.zOld;
        this.wrapArmYaw();
        updateBoundingBox();
    }

    @Override
    public float getViewYRot(float tickDelta) {
        Entity view = this.minecraft.getCameraEntity();
        if (view != null && view != this) {
            return this.yRotO + (this.getYRot() - this.yRotO) * tickDelta;
        }
        return super.getViewYRot(tickDelta);
    }

    @Override
    public float getViewXRot(float tickDelta) {
        Entity view = this.minecraft.getCameraEntity();
        if (view != null && view != this) {
            return this.xRotO + (this.getXRot() - this.xRotO) * tickDelta;
        }
        return super.getViewXRot(tickDelta);
    }

    private void updateBoundingBox() {
        float width = getBbWidth();
        float height = getBbHeight();
        setBoundingBox(new AABB(
                this.getX() - width / 2, this.getY(), this.getZ() - width / 2,
                this.getX() + width / 2, this.getY() + height, this.getZ() + width / 2));
    }

    @Override
    public void tick() {
        Entity view =
            this.minecraft.getCameraEntity();
        if (view != null) {
            // Make sure we're always spectating the right entity
            // This is important if the spectated player respawns as their
            // entity is recreated, and we have to spectate a new entity
            UUID spectating = SceneCraftReplay.instance.getReplayHandler().getSpectatedUUID();
            // FIXME remap bug: Pattern doesn't work when these two are inlined
            Level cameraWorld = this.level();
            Level viewWorld = view.level();
            if (spectating != null && (view.getUUID() != spectating
                    || viewWorld != cameraWorld)
                    || cameraWorld.getEntity(view.getId()) != view) {
                if (spectating == null) {
                    // Entity (non-player) died, stop spectating
                    SceneCraftReplay.instance.getReplayHandler().spectateEntity(this);
                    return;
                }
                view = cameraWorld.getPlayerByUUID(spectating);
                if (view != null) {
                    this.minecraft.setCameraEntity(view);
                } else {
                    this.minecraft.setCameraEntity(this);
                    return;
                }
            }
            // Move camera to their position so when we exit the first person view
            // we don't jump back to where we entered it
            if (view != this) {
                setCameraPosRot(view);
            }
        }
    }

    @Override
    public void resetPos() {
        // Make sure our world is up-to-date in case of world changes
        if (this.minecraft.level != null) {
            // FIXME cannot use Patters because `setWorld` is `protected` in 1.20
            this.setLevel(this.minecraft.level);
        }
        super.resetPos();
    }

    @Override
    public void setRot(float yaw, float pitch) {
        if (this.minecraft.getCameraEntity() == this) {
            // Only update camera rotation when the camera is the view
            super.setRot(yaw, pitch);
        }
    }

    @Override
    public boolean isInWall() {
        return falseUnlessSpectating(Entity::isInWall); // Make sure no suffocation overlay is rendered
    }

    @Override
    public boolean isEyeInFluid(
            TagKey<Fluid> fluid
    ) {
        return falseUnlessSpectating(entity -> entity.isEyeInFluid(fluid));
    }

    @Override
    public float getWaterVision() {
        return falseUnlessSpectating(__ -> true) ? super.getWaterVision() : 1f;
    }

    @Override
    public boolean isOnFire() {
        return falseUnlessSpectating(Entity::isOnFire); // Make sure no fire overlay is rendered
    }

    private boolean falseUnlessSpectating(Function<Entity, Boolean> property) {
        Entity view = this.minecraft.getCameraEntity();
        if (view != null && view != this) {
            return property.apply(view);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false; // We are in full control of ourselves
    }

    @Override
    protected void spawnSprintParticle() {
        // We do not produce any particles, we are a camera
    }

    @Override
    public boolean isPickable() {
        return false; // We are a camera, we cannot collide
    }

    @Override
    public boolean isSpectator() {
        ReplayHandler replayHandler = SceneCraftReplay.instance.getReplayHandler();
        return replayHandler == null || replayHandler.isCameraView(); // Make sure we're treated as spectator
    }

    @Override
    public boolean shouldRender(double double_1, double double_2, double double_3) {
        return false; // never render the camera otherwise it'd be visible e.g. in 3rd-person or with shaders
    }

    @Override
    public float getFieldOfViewModifier(boolean firstPerson, float fovEffectScale) {
        Entity view = this.minecraft.getCameraEntity();
        if (view != this && view instanceof AbstractClientPlayer) {
            return ((AbstractClientPlayer) view).getFieldOfViewModifier(firstPerson, fovEffectScale);
        }
        return 1;
    }

    @Override
    public boolean isInvisible() {
        Entity view = this.minecraft.getCameraEntity();
        if (view != this) {
            return view.isInvisible();
        }
        return super.isInvisible();
    }

    @Override
    public PlayerSkin getSkin() {
        Entity view = this.minecraft.getCameraEntity();
        if (view != this && view instanceof ClientAvatarEntity) {
            return ((ClientAvatarEntity) view).getSkin();
        }
        return super.getSkin();
    }

    @Override
    public boolean isModelPartShown(PlayerModelPart modelPart) {
        Entity view = this.minecraft.getCameraEntity();
        if (view != this && view instanceof Avatar) {
            return ((Avatar) view).isModelPartShown(modelPart);
        }
        return super.isModelPartShown(modelPart);
    }

    @Override
    public HumanoidArm getMainArm() {
        Entity view = this.minecraft.getCameraEntity();
        if (view != this && view instanceof Player) {
            return ((Player) view).getMainArm();
        }
        return super.getMainArm();
    }

    @Override
    public float getSwingAnimation(float renderPartialTicks) {
        Entity view = this.minecraft.getCameraEntity();
        if (view != this && view instanceof Player) {
            return ((Player) view).getSwingAnimation(renderPartialTicks);
        }
        return 0;
    }

    @Override
    public net.minecraft.world.entity.LivingEntity.SwingDescription getCurrentSwing() {
        Entity view = this.minecraft.getCameraEntity();
        if (view != this && view instanceof Player) {
            return ((Player) view).getCurrentSwing();
        }
        return null;
    }

    @Override
    public float getCurrentItemAttackStrengthDelay() {
        Entity view = this.minecraft.getCameraEntity();
        if (view != this && view instanceof Player) {
            return ((Player) view).getCurrentItemAttackStrengthDelay();
        }
        return 1;
    }

    @Override
    public float getAttackStrengthScale(float adjustTicks) {
        Entity view = this.minecraft.getCameraEntity();
        if (view != this && view instanceof Player) {
            return ((Player) view).getAttackStrengthScale(adjustTicks);
        }
        // Default to 1 as to not render the cooldown indicator (renders for < 1)
        return 1;
    }

    @Override
    public InteractionHand getUsedItemHand() {
        Entity view = this.minecraft.getCameraEntity();
        if (view != this && view instanceof Player) {
            return ((Player) view).getUsedItemHand();
        }
        return super.getUsedItemHand();
    }

    @Override
    public boolean isUsingItem() {
        Entity view = this.minecraft.getCameraEntity();
        if (view != this && view instanceof Player) {
            return ((Player) view).isUsingItem();
        }
        return super.isUsingItem();
    }

    @Override
    public void onEquipItem(EquipmentSlot slot, ItemStack stack, ItemStack itemStack) {
        // Suppress equip sounds
    }

    @Override
    public HitResult pick(double maxDistance, float tickDelta, boolean fluids) {
        HitResult result = super.pick(maxDistance, tickDelta, fluids);

        // Make sure we can never look at blocks (-> no outline)
        if (result instanceof BlockHitResult) {
            BlockHitResult blockResult = (BlockHitResult) result;
            result = BlockHitResult.miss(result.getLocation(), blockResult.getDirection(), blockResult.getBlockPos());
        }

        return result;
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (eventHandler != null) {
            eventHandler.unregister();
            eventHandler = null;
        }
    }

    private void update() {
        if (MCVer.getMinecraft().level != this.level()) {
            if (eventHandler != null) {
                eventHandler.unregister();
                eventHandler = null;
            }
            return;
        }

        long now = System.currentTimeMillis();
        long timePassed = now - lastControllerUpdate;
        cameraController.update(timePassed / 50f);
        lastControllerUpdate = now;

        handleInputEvents();

        Map<String, KeyBindingRegistry.Binding> keyBindings = SceneCraft.instance.getKeyBindingRegistry().getBindings();
        if (keyBindings.get("scenecraft.input.rollclockwise").keyBinding.isDown()) {
            roll += Utils.isCtrlDown() ? 0.2 : 1;
        }
        if (keyBindings.get("scenecraft.input.rollcounterclockwise").keyBinding.isDown()) {
            roll -= Utils.isCtrlDown() ? 0.2 : 1;
        }

        this.noPhysics = this.isSpectator();

        syncInventory();
    }

    // If we are spectating a player, "steal" its inventory so the rendering code knows what item(s) to render
    // and if we aren't, then reset ours.
    private void syncInventory() {
        Entity view = this.minecraft.getCameraEntity();
        Player viewPlayer = view != this && view instanceof Player ? (Player) view : null;

        ItemStack empty = ItemStack.EMPTY;

        // TODO switch to replacing the entire inventory for 1.14+ as well, should be easier and faster
        this.setItemSlot(EquipmentSlot.HEAD, viewPlayer != null ? viewPlayer.getItemBySlot(EquipmentSlot.HEAD) : empty);
        this.setItemSlot(EquipmentSlot.MAINHAND, viewPlayer != null ? viewPlayer.getItemBySlot(EquipmentSlot.MAINHAND) : empty);
        this.setItemSlot(EquipmentSlot.OFFHAND, viewPlayer != null ? viewPlayer.getItemBySlot(EquipmentSlot.OFFHAND) : empty);

        this.lastItemInMainHand = viewPlayer != null ? viewPlayer.lastItemInMainHand : empty;
        this.useItem = viewPlayer != null ? viewPlayer.getUseItem() : empty;
        this.useItemRemaining = viewPlayer != null ? viewPlayer.useItemRemaining : 0;
    }

    private void handleInputEvents() {
        if (this.minecraft.options.keyAttack.consumeClick() || this.minecraft.options.keyUse.consumeClick()) {
            if (this.minecraft.gui.screen() == null && canSpectate(this.minecraft.crosshairPickEntity)) {
                SceneCraftReplay.instance.getReplayHandler().spectateEntity(
                        this.minecraft.crosshairPickEntity);
                // Make sure we don't exit right away
                //noinspection StatementWithEmptyBody
                while (this.minecraft.options.keyShift.consumeClick());
            }
        }
    }

    private void updateArmYawAndPitch() {
        this.yBobO = this.yBob;
        this.xBobO = this.xBob;
        this.xBob = this.xBob +  (this.getXRot() - this.xBob) * 0.5f;
        this.yBob = this.yBob + wrapDegrees(this.getYRot() - this.yBob) * 0.5f;
        this.wrapArmYaw();
    }

    private void wrapArmYaw() {
        this.yBob = wrapDegreesTo(this.yBob, this.getYRot());
        this.yBobO = wrapDegreesTo(this.yBobO, this.yBob);
    }

    private static float wrapDegreesTo(float value, float towardsValue) {
        while (towardsValue - value < -180) {
            value -= 360;
        }
        while (towardsValue - value >= 180) {
            value += 360;
        }
        return value;
    }

    private static float wrapDegrees(float value) {
        value %= 360;
        return wrapDegreesTo(value, 0);
    }

    public boolean canSpectate(Entity e) {
        return e != null
                && !e.isInvisible();
    }

    private
    class EventHandler extends EventRegistrations {
        private final Minecraft mc = getMinecraft();

        private EventHandler() {}

        { on(PreTickCallback.EVENT, this::onPreClientTick); }
        private void onPreClientTick() {
            updateArmYawAndPitch();
        }

        { on(PreRenderCallback.EVENT, this::onRenderUpdate); }
        private void onRenderUpdate() {
            update();
        }

        { on(KeyBindingEventCallback.EVENT, CameraEntity.this::handleInputEvents); }

        { on(RenderSpectatorCrosshairCallback.EVENT, this::shouldRenderSpectatorCrosshair); }
        private Boolean shouldRenderSpectatorCrosshair() {
            return canSpectate(mc.crosshairPickEntity);
        }

        { on(RenderHotbarCallback.EVENT, this::shouldRenderHotbar); }
        private Boolean shouldRenderHotbar() {
            return false;
        }

        { on(SettingsChangedCallback.EVENT, this::onSettingsChanged); }
        private void onSettingsChanged(SettingsRegistry registry, SettingsRegistry.SettingKey<?> key) {
            if (key == Setting.CAMERA) {
                if (SceneCraftReplay.instance.getReplayHandler().getSpectatedUUID() == null) {
                    cameraController = SceneCraftReplay.instance.createCameraController(CameraEntity.this);
                } else {
                    cameraController = new SpectatorCameraController(CameraEntity.this);
                }
            }
        }

        { on(PreRenderHandCallback.EVENT, this::onRenderHand); }
        private boolean onRenderHand() {
            // Unless we are spectating another player, don't render our hand
            Entity view = mc.getCameraEntity();
            if (view == CameraEntity.this || !(view instanceof Player)) {
                return true; // cancel hand rendering
            } else {
                Player player = (Player) view;
                // When the spectated player has changed, force equip their items to prevent the equip animation
                if (lastHandRendered != player) {
                    lastHandRendered = player;

                    FirstPersonHandsAndItems acc = mc.player.firstPersonHandsAndItems();
                    acc.oMainHandHeight = 1;
                    acc.oOffHandHeight = 1;
                    acc.mainHandHeight = 1;
                    acc.offHandHeight = 1;
                    acc.mainHandItem = player.getItemBySlot(EquipmentSlot.MAINHAND);
                    acc.offHandItem = player.getItemBySlot(EquipmentSlot.OFFHAND);

                    mc.player.yBob = mc.player.yBobO = player.getYRot();
                    mc.player.xBob = mc.player.xBobO = player.getXRot();
                }
                return false;
            }
        }

        // Moved to MixinCamera
    }
}
