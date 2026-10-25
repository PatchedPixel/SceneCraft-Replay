package net.patchedpixel.scenecraft.replay.camera;

import net.patchedpixel.scenecraft.replay.SceneCraftReplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.Entity;

import java.util.Arrays;

import static net.patchedpixel.scenecraft.core.versions.MCVer.*;

public class SpectatorCameraController implements CameraController {
    private final CameraEntity camera;

    public SpectatorCameraController(CameraEntity camera) {
        this.camera = camera;
    }

    @Override
    public void update(float partialTicksPassed) {
        Minecraft mc = getMinecraft();
        if (mc.options.keyShift.consumeClick()) {
            SceneCraftReplay.instance.getReplayHandler().spectateCamera();
        }

        // Soak up all remaining key presses
        for (KeyMapping binding : Arrays.asList(mc.options.keyAttack, mc.options.keyUse,
                mc.options.keyJump, mc.options.keyShift, mc.options.keyUp,
                mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight)) {
            //noinspection StatementWithEmptyBody
            while (binding.consumeClick());
        }

        // Prevent mouse movement
        // No longer needed

        // Always make sure the camera is in the exact same spot as the spectated entity
        // This is necessary as some rendering code for the hand doesn't respect the view entity
        // and always uses mc.thePlayer
        Entity view = mc.getCameraEntity();
        if (view != null && view != camera) {
            camera.setCameraPosRot(mc.getCameraEntity());
        }
    }

    @Override
    public void increaseSpeed() {

    }

    @Override
    public void decreaseSpeed() {

    }
}
