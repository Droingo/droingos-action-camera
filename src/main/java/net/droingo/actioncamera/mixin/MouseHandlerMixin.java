package net.droingo.actioncamera.mixin;

import net.droingo.actioncamera.client.ActionCameraClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void droingoActionCamera$turnCameraInsteadOfPlayer(CallbackInfo callbackInfo) {
        if (!ActionCameraClientState.isEditingCamera()) {
            return;
        }

        double sensitivity = this.minecraft.options.sensitivity().get() * 0.6000000238418579D + 0.20000000298023224D;
        double scaledSensitivity = sensitivity * sensitivity * sensitivity * 8.0D;

        double yawDelta = this.accumulatedDX * scaledSensitivity;
        double pitchDelta = this.accumulatedDY * scaledSensitivity;

        if (this.minecraft.options.invertYMouse().get()) {
            pitchDelta = -pitchDelta;
        }

        this.accumulatedDX = 0.0D;
        this.accumulatedDY = 0.0D;

        ActionCameraClientState.handleMouseTurn(yawDelta, pitchDelta);

        callbackInfo.cancel();
    }
}