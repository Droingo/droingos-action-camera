package net.droingo.actioncamera.mixin;

import net.droingo.actioncamera.client.ActionCameraClientState;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method = "setup", at = @At("TAIL"))
    private void droingoActionCamera$overrideCamera(
            BlockGetter level,
            Entity renderViewEntity,
            boolean detached,
            boolean thirdPersonReverse,
            float partialTick,
            CallbackInfo callbackInfo
    ) {
        ActionCameraClientState.applyToCamera((Camera) (Object) this, partialTick);
    }
}