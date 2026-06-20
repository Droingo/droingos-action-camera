package net.droingo.actioncamera.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.droingo.actioncamera.client.ActionCameraClientState;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void droingoActionCamera$disableViewBob(PoseStack poseStack, float partialTick, CallbackInfo callbackInfo) {
        if (ActionCameraClientState.isActive()) {
            callbackInfo.cancel();
        }
    }
}