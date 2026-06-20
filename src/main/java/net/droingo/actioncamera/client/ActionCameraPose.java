package net.droingo.actioncamera.client;

import net.minecraft.world.phys.Vec3;

public record ActionCameraPose(
        Vec3 position,
        float yaw,
        float pitch,
        float roll,
        float fovOverride
) {
}