package net.droingo.actioncamera.client;

import net.droingo.actioncamera.DroingoActionCamera;
import net.droingo.actioncamera.world.blockentity.ActionCameraBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(
        modid = DroingoActionCamera.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientGameEvents {
    private ClientGameEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ActionCameraClientState.clientTick();
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide()) {
            return;
        }

        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || event.getEntity() != minecraft.player) {
            return;
        }

        BlockPos pos = event.getPos();

        if (minecraft.level.getBlockEntity(pos) instanceof ActionCameraBlockEntity) {
            ActionCameraClientState.startEditing(pos);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        ActionCameraPose pose = ActionCameraClientState.getLastAppliedPose();
        if (pose == null || !ActionCameraClientState.isEditingCamera()) {
            return;
        }

        event.setYaw(pose.yaw());
        event.setPitch(pose.pitch());
        event.setRoll(pose.roll());
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        float fov = ActionCameraClientState.getActiveFovOverride();
        if (fov > 0.0F) {
            event.setFOV(fov);
        }
    }
}