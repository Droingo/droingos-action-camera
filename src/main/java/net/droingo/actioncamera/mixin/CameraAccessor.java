package net.droingo.actioncamera.mixin;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("position")
    void droingoActionCamera$setPosition(Vec3 position);

    @Accessor("blockPosition")
    BlockPos.MutableBlockPos droingoActionCamera$getBlockPosition();

    @Accessor("xRot")
    void droingoActionCamera$setXRot(float xRot);

    @Accessor("yRot")
    void droingoActionCamera$setYRot(float yRot);

    @Accessor("rotation")
    Quaternionf droingoActionCamera$getRotation();

    @Accessor("forwards")
    Vector3f droingoActionCamera$getForwards();

    @Accessor("up")
    Vector3f droingoActionCamera$getUp();

    @Accessor("left")
    Vector3f droingoActionCamera$getLeft();

    /*
     * Important:
     * Vanilla first-person camera hides the local player.
     *
     * When Action Camera is active, we set detached=true so Minecraft treats
     * this as a detached camera and still renders the player body in the world.
     */
    @Accessor("detached")
    void droingoActionCamera$setDetached(boolean detached);
}