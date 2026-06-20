package net.droingo.actioncamera.client;

import dev.ryanhcode.sable.companion.SableCompanion;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(
        modid = DroingoActionCamera.MOD_ID,
        value = Dist.CLIENT
)
public final class ClientGameEvents {
    /*
     * Normal-world camera discovery range.
     *
     * Sable plot cameras may be far outside this chunk range, so we also keep
     * KNOWN_CAMERAS for cameras that were looked at/activated before.
     */
    private static final int CAMERA_CYCLE_CHUNK_RADIUS = 10;

    private static final Set<BlockPos> KNOWN_CAMERAS = new LinkedHashSet<>();

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

            BlockPos lookedAtCamera = getLookedAtCameraPos(minecraft);
            if (lookedAtCamera != null) {
                rememberCamera(lookedAtCamera);
            }

            List<BlockPos> cameras = collectLoadedCameraPositions(minecraft);

            /*
             * If not already viewing, prefer the camera the player is looking at.
             * This matters for Sable because the camera can be in a far-away plot
             * even though the visible block is right in front of you.
             */
            if (!ActionCameraClientState.isViewingCamera()) {
                if (lookedAtCamera != null) {
                    ActionCameraClientState.startViewing(lookedAtCamera);
                    return;
                }

                if (cameras.isEmpty()) {
                    minecraft.player.displayClientMessage(Component.literal("No loaded Action Cameras nearby"), true);
                    return;
                }

                ActionCameraClientState.startViewing(cameras.get(0));
                return;
            }

            /*
             * Already viewing:
             * C cycles to the next loaded/known camera.
             * Shift exits.
             */
            BlockPos current = ActionCameraClientState.getActiveCameraPos();
            if (current != null) {
                rememberCamera(current);
            }

            if (cameras.isEmpty()) {
                minecraft.player.displayClientMessage(Component.literal("No loaded Action Cameras nearby"), true);
                return;
            }

            int currentIndex = current == null ? -1 : cameras.indexOf(current);
            int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % cameras.size();

            ActionCameraClientState.startViewing(cameras.get(nextIndex));
        }
    }

    private static void rememberCamera(BlockPos pos) {
        KNOWN_CAMERAS.add(pos.immutable());
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
        Set<BlockPos> cameraSet = new LinkedHashSet<>();

        if (minecraft.level == null || minecraft.player == null) {
            return new ArrayList<>();
        }

        /*
         * Keep known cameras first.
         * Invalid/unloaded ones get filtered out below.
         */
        cameraSet.addAll(KNOWN_CAMERAS);

        /*
         * Discover normal-world cameras around the player.
         */
        ChunkPos playerChunk = new ChunkPos(minecraft.player.blockPosition());

        for (int chunkX = playerChunk.x - CAMERA_CYCLE_CHUNK_RADIUS; chunkX <= playerChunk.x + CAMERA_CYCLE_CHUNK_RADIUS; chunkX++) {
            for (int chunkZ = playerChunk.z - CAMERA_CYCLE_CHUNK_RADIUS; chunkZ <= playerChunk.z + CAMERA_CYCLE_CHUNK_RADIUS; chunkZ++) {
                if (!minecraft.level.hasChunk(chunkX, chunkZ)) {
                    continue;
                }

                LevelChunk chunk = minecraft.level.getChunk(chunkX, chunkZ);

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof ActionCameraBlockEntity) {
                        cameraSet.add(blockEntity.getBlockPos().immutable());
                    }
                }
            }
        }

        /*
         * Filter to cameras that are actually loaded and still valid.
         * This keeps stale known cameras from breaking cycling after chunks unload.
         */
        cameraSet.removeIf(pos -> {
            if (!minecraft.level.isLoaded(pos)) {
                return true;
            }

            return !(minecraft.level.getBlockEntity(pos) instanceof ActionCameraBlockEntity);
        });

        List<BlockPos> cameras = new ArrayList<>(cameraSet);
        Vec3 playerPos = minecraft.player.position();

        cameras.sort(Comparator.comparingDouble(pos ->
                SableCompanion.INSTANCE.distanceSquaredWithSubLevels(
                        minecraft.level,
                        Vec3.atCenterOf(pos),
                        playerPos
                )
        ));

        KNOWN_CAMERAS.clear();
        KNOWN_CAMERAS.addAll(cameras);

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
            rememberCamera(pos);
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
}