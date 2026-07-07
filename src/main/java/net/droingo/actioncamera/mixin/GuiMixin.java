package net.droingo.actioncamera.mixin;

import net.droingo.actioncamera.client.ActionCameraClientState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void droingoActionCamera$renderHardCameraNameOverlay(
            GuiGraphics guiGraphics,
            DeltaTracker deltaTracker,
            CallbackInfo callbackInfo
    ) {
        if (!ActionCameraClientState.shouldRenderHardCameraNameOverlay()) {
            return;
        }

        ActionCameraClientState.renderCameraNameOverlay(guiGraphics);
    }
}
