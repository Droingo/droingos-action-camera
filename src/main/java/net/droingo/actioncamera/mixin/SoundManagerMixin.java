package net.droingo.actioncamera.mixin;

import net.droingo.actioncamera.client.ActionCameraClientState;
import net.minecraft.client.Camera;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {
    private boolean droingoActionCamera$usingVanillaSoundCamera;

    @Inject(method = "updateSource", at = @At("HEAD"), cancellable = true)
    private void droingoActionCamera$useVanillaCameraForAudio(Camera camera, CallbackInfo callbackInfo) {
        if (droingoActionCamera$usingVanillaSoundCamera) {
            return;
        }

        Camera vanillaSoundCamera = ActionCameraClientState.getVanillaSoundCameraOverride();

        if (vanillaSoundCamera == null) {
            return;
        }

        try {
            droingoActionCamera$usingVanillaSoundCamera = true;

            /*
             * Update the audio listener from the normal player/spectator camera,
             * not the Action Camera.
             *
             * ActionCameraClientState also keeps this override alive for a few
             * frames after exit, which prevents the listener orientation from
             * being left sideways after closing the cinematic camera.
             */
            ((SoundManager) (Object) this).updateSource(vanillaSoundCamera);
        } finally {
            droingoActionCamera$usingVanillaSoundCamera = false;
        }

        callbackInfo.cancel();
    }
}