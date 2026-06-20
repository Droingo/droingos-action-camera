package net.droingo.actioncamera.client;

import net.droingo.actioncamera.DroingoActionCamera;
import net.droingo.actioncamera.world.blockentity.ActionCameraBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(
        modid = DroingoActionCamera.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientGameEvents {
    /*
     * How far C-cycling searches for loaded camera block entities.
     *
     * 10 chunks = 160 blocks in every horizontal direction.
     * This is a good prototype range and avoids scanning every block manually.
     */
    private static final int CAMERA_CYCLE_CHUNK_RADIUS = 10;

    private ClientGameEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        handleViewKeybind();
        ActionCameraClientState.clientTick();
    }

    private static void handleViewKeybind() {
        Minecraft minecraft = Minecraft.getInstance();

        while (ActionCameraKeyMappings.TOGGLE_ACTION_CAMERA_VIEW.consumeClick()) {
            if (minecraft.player == null || minecraft.level == null) {
                return;
            }

            /*
             * Do not let C interfere with right-click edit mode.
             * Shift is the save/exit control for editing.
             */
            if (ActionCameraClientState.isEditingCamera()) {
                return;
            }

            List<BlockPos> cameras = collectLoadedCameraPositions(minecraft);

            if (cameras.isEmpty()) {
                minecraft.player.displayClientMessage(Component.literal("No loaded Action Cameras nearby"), true);
                return;
            }

            /*
             * If not already viewing, prefer the camera the player is looking at.
             * If not looking at one, use the nearest loaded camera.
             */
            if (!ActionCameraClientState.isViewingCamera()) {
                BlockPos lookedAtCamera = getLookedAtCameraPos(minecraft);
                if (lookedAtCamera != null && cameras.contains(lookedAtCamera)) {
                    ActionCameraClientState.startViewing(lookedAtCamera);
                    return;
                }

                ActionCameraClientState.startViewing(cameras.getFirst());
                return;
            }

            /*
             * Already viewing:
             * C cycles to the next loaded camera instead of exiting.
             * Shift exits.
             */
            BlockPos current = ActionCameraClientState.getActiveCameraPos();
            int currentIndex = current == null ? -1 : cameras.indexOf(current);
            int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % cameras.size();

            ActionCameraClientState.startViewing(cameras.get(nextIndex));
        }
    }

    private static BlockPos getLookedAtCameraPos(Minecraft minecraft) {
        if (minecraft.level == null) {
            return null;
        }

        HitResult hitResult = minecraft.hitResult;
        if (!(hitResult instanceof BlockHitResult blockHitResult)) {
            return null;
        }

        BlockPos pos = blockHitResult.getBlockPos();
        if (minecraft.level.getBlockEntity(pos) instanceof ActionCameraBlockEntity) {
            return pos.immutable();
        }

        return null;
    }

    private static List<BlockPos> collectLoadedCameraPositions(Minecraft minecraft) {
        List<BlockPos> cameras = new ArrayList<>();

        if (minecraft.level == null || minecraft.player == null) {
            return cameras;
        }

        ChunkPos playerChunk = new ChunkPos(minecraft.player.blockPosition());

        for (int chunkX = playerChunk.x - CAMERA_CYCLE_CHUNK_RADIUS; chunkX <= playerChunk.x + CAMERA_CYCLE_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = playerChunk.z - CAMERA_CYCLE_CHUNK_RADIUS; chunkZ <= playerChunk.z + CAMERA_CYCLE_CHUNK_RADIUS; chunkZ++) {
                if (!minecraft.level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                LevelChunk chunk = minecraft.level.getChunk(chunkX, chunkZ);

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof ActionCameraBlockEntity) {
                        cameras.add(blockEntity.getBlockPos().immutable());
                    }
                }
            }
        }

        cameras.sort(Comparator.comparingDouble(pos ->
                pos.distSqr(minecraft.player.blockPosition())
        ));

        return cameras;
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
        if (pose == null || !ActionCameraClientState.isActive()) {
            return;
        }

        event.setYaw(pose.yaw());
        event.setPitch(pose.pitch());
        event.setRoll(pose.roll());
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!ActionCameraClientState.isActive()) {
            return;
        }

        float cameraFov = ActionCameraClientState.getActiveFovOverride();

        if (cameraFov > 0.0F) {
            event.setFOV(cameraFov);
            return;
        }

        /*
         * Lock to the user's normal FOV setting while Action Camera is active.
         *
         * This prevents vanilla sprint/speed/flying FOV changes from affecting
         * the cinematic camera view.
         */
        Minecraft minecraft = Minecraft.getInstance();
        event.setFOV(minecraft.options.fov().get());
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (ActionCameraClientState.isActive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        if (ActionCameraClientState.isViewingCamera()) {
            event.setCanceled(true);
        }
    }
}