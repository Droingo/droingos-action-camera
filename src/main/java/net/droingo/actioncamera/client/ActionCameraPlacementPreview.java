package net.droingo.actioncamera.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.droingo.actioncamera.DroingoActionCamera;
import net.droingo.actioncamera.registry.ModItems;
import net.droingo.actioncamera.world.block.ActionCameraMountSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

@EventBusSubscriber(
        modid = DroingoActionCamera.MOD_ID,
        value = Dist.CLIENT
)
public final class ActionCameraPlacementPreview {
    private ActionCameraPlacementPreview() {
    }

    @SubscribeEvent
    public static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        Player player = minecraft.player;

        if (!isHoldingActionCamera(player)) {
            return;
        }

        BlockHitResult hit = event.getTarget();
        Direction clickedFace = hit.getDirection();

        /*
         * This matches ordinary attached-block placement:
         * click a surface, place the camera block in the adjacent air block.
         */
        BlockPos placePos = hit.getBlockPos().relative(clickedFace);

        BlockState placeState = minecraft.level.getBlockState(placePos);
        if (!placeState.canBeReplaced()) {
            return;
        }

        int slot = ActionCameraMountSlot.slotFromHit(clickedFace, hit.getLocation());

        AABB previewBox = ActionCameraMountSlot.previewBox(
                clickedFace,
                placePos.getX(),
                placePos.getY(),
                placePos.getZ(),
                slot
        );

        Vec3 cameraPosition = event.getCamera().getPosition();
        AABB renderBox = previewBox.move(
                -cameraPosition.x,
                -cameraPosition.y,
                -cameraPosition.z
        );

        VertexConsumer lineConsumer = event.getMultiBufferSource().getBuffer(RenderType.lines());

        LevelRenderer.renderLineBox(
                event.getPoseStack(),
                lineConsumer,
                renderBox,
                0.2F,
                0.85F,
                1.0F,
                0.95F
        );

        /*
         * Replace the vanilla full-block outline with our camera-size outline.
         */
        event.setCanceled(true);
    }

    private static boolean isHoldingActionCamera(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        return mainHand.is(ModItems.ACTION_CAMERA.get()) || offHand.is(ModItems.ACTION_CAMERA.get());
    }
}