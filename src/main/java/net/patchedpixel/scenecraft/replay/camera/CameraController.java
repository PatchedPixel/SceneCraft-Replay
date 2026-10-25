package net.patchedpixel.scenecraft.replay.camera;

public interface CameraController {
    void update(float partialTicksPassed);

    void increaseSpeed();
    void decreaseSpeed();
}
