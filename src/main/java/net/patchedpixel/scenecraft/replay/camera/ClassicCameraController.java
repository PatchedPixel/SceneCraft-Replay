package net.patchedpixel.scenecraft.replay.camera;

import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.minecraft.client.KeyMapping;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public class ClassicCameraController implements CameraController {
    private static final double LOWER_SPEED = 0.2;
    private static final double UPPER_SPEED = 200;
    private static final double SPEED_CHANGE = (UPPER_SPEED - LOWER_SPEED) / 20000;
    private static final double RATE = 5;
    private static final double MAX_STEP_SECONDS = 0.05;

    private final CameraEntity camera;

    private double maxSpeed = 10;
    private double threshold = maxSpeed / 20;

    private final Vector3f heading = new Vector3f();
    private double motion;
    private long lastNanos = System.nanoTime();

    public ClassicCameraController(CameraEntity camera) {
        this.camera = camera;
    }

    @Override
    public void update(float partialTicksPassed) {
        boolean forward = false, backward = false, left = false, right = false, up = false, down = false;
        for (KeyMapping kb : MCVer.getMinecraft().options.keyMappings) {
            if (!kb.isDown()) continue;
            switch (kb.getName()) {
                case "key.forward" -> forward = true;
                case "key.back" -> backward = true;
                case "key.left" -> left = true;
                case "key.right" -> right = true;
                case "key.jump" -> up = true;
                case "key.sneak" -> down = true;
            }
        }

        long now = System.nanoTime();
        double dt = net.minecraft.util.Mth.clamp((now - lastNanos) / 1_000_000_000D, 0, MAX_STEP_SECONDS);
        lastNanos = now;

        Vector3f wanted = computeDirection(
                (forward ? 1 : 0) - (backward ? 1 : 0),
                (right ? 1 : 0) - (left ? 1 : 0),
                (up ? 1 : 0) - (down ? 1 : 0)
        );

        boolean accelerating = wanted.lengthSquared() > 0;

        if (accelerating) {
            heading.set(wanted);
            if (motion < threshold) motion = threshold;
            motion = Math.min(motion * Math.exp(RATE * dt), maxSpeed);
        } else {
            motion *= Math.exp(-RATE * dt);
            if (motion < threshold) {
                motion = 0;
                heading.zero();
            }
        }

        if (motion <= 0 || heading.lengthSquared() == 0) {
            return;
        }

        double step = motion * dt;
        camera.moveCamera(heading.x * step, heading.y * step, heading.z * step);
    }

    @Override
    public void increaseSpeed() {
        setCameraMaximumSpeed(maxSpeed + SPEED_CHANGE);
    }

    @Override
    public void decreaseSpeed() {
        setCameraMaximumSpeed(maxSpeed - SPEED_CHANGE);
    }

    private void setCameraMaximumSpeed(double newMaxSpeed) {
        if (newMaxSpeed < LOWER_SPEED || newMaxSpeed > UPPER_SPEED) return;
        maxSpeed = newMaxSpeed;
        threshold = maxSpeed / 20;
    }

    private Vector3f computeDirection(int forwardAxis, int rightAxis, int verticalAxis) {
        Vector3f result = new Vector3f();
        float pitch = camera.getXRot();
        float yaw = camera.getYRot();
        if (forwardAxis != 0) {
            result.add(getVectorForRotation(pitch, yaw).mul(forwardAxis));
        }
        if (rightAxis != 0) {
            result.add(getVectorForRotation(0, yaw + 90).mul(rightAxis));
        }
        if (verticalAxis != 0) {
            result.add(0, verticalAxis, 0);
        }
        if (result.lengthSquared() > 0) {
            result.normalize();
        }
        return result;
    }

    private Vector3f getVectorForRotation(float pitch, float yaw) {
        float f1 = 0.017453292F;
        float f2 = Mth.cos(-yaw * f1 - (float) Math.PI);
        float f3 = Mth.sin(-yaw * f1 - (float) Math.PI);
        float f4 = -Mth.cos(-pitch * f1);
        float f5 = Mth.sin(-pitch * f1);
        return new Vector3f(f3 * f4, f5, f2 * f4);
    }
}
