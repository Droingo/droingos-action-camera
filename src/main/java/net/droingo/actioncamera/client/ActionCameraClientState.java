package net.droingo.actioncamera.client;

import net.droingo.actioncamera.mixin.CameraAccessor;
import net.droingo.actioncamera.network.UpdateActionCameraPayload;
import net.droingo.actioncamera.world.blockentity.ActionCameraBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public final class ActionCameraClientState {
    private static BlockPos activeCameraPos;

    private static double editOffsetX;
    private static double editOffsetY;
    private static double editOffsetZ;

    private static float editYawOffset;
    private static float editPitchOffset;
    private static float editRollOffset;
    private static float editFovOverride;
    private static float editSmoothing;

    private static String editCameraName = "Action Camera";

    private static boolean dirty;
    private static int saveCooldownTicks;
    private static boolean wasShiftDown;

    private static ActionCameraPose smoothedPose;
    private static ActionCameraPose lastAppliedPose;

    private ActionCameraClientState() {
    }

    public static boolean isEditingCamera() {
        return activeCameraPos != null;
    }

    @Nullable
    public static ActionCameraPose getLastAppliedPose() {
        return lastAppliedPose;
    }

    public static void startEditing(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        BlockEntity blockEntity = minecraft.level.getBlockEntity(pos);
        if (!(blockEntity instanceof ActionCameraBlockEntity camera)) {
            return;
        }

        activeCameraPos = pos.immutable();

        editOffsetX = camera.getOffsetX();
        editOffsetY = camera.getOffsetY();
        editOffsetZ = camera.getOffsetZ();

        editYawOffset = camera.getYawOffset();
        editPitchOffset = camera.getPitchOffset();
        editRollOffset = camera.getRollOffset();
        editFovOverride = camera.getFovOverride();
        editSmoothing = camera.getSmoothing();
        editCameraName = camera.getCameraName();

        dirty = false;
        saveCooldownTicks = 0;
        wasShiftDown = true;

        smoothedPose = null;
        lastAppliedPose = null;

        minecraft.player.displayClientMessage(
                Component.literal("Action Camera edit mode. Move mouse to aim. Press Shift to save and exit."),
                true
        );
    }

    public static void stopEditing(boolean save) {
        Minecraft minecraft = Minecraft.getInstance();

        if (activeCameraPos == null) {
            return;
        }

        if (save) {
            sendUpdateToServer();
        }

        activeCameraPos = null;
        smoothedPose = null;
        lastAppliedPose = null;
        dirty = false;

        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("Action Camera saved"), true);
        }
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();

        if (!isEditingCamera()) {
            wasShiftDown = minecraft.options.keyShift.isDown();
            return;
        }

        if (minecraft.level == null || activeCameraPos == null) {
            stopEditing(false);
            return;
        }

        if (!(minecraft.level.getBlockEntity(activeCameraPos) instanceof ActionCameraBlockEntity)) {
            stopEditing(false);
            return;
        }

        boolean shiftDown = minecraft.options.keyShift.isDown();
        if (shiftDown && !wasShiftDown) {
            stopEditing(true);
            wasShiftDown = true;
            return;
        }
        wasShiftDown = shiftDown;

        if (dirty) {
            saveCooldownTicks--;

            if (saveCooldownTicks <= 0) {
                sendUpdateToServer();
                saveCooldownTicks = 5;
            }
        }
    }

    public static boolean handleMouseTurn(double vanillaYawDelta, double vanillaPitchDelta) {
        if (!isEditingCamera()) {
            return false;
        }

        /*
         * Vanilla mouse rotation eventually applies a 0.15F turn multiplier.
         * Using the same scale keeps this feeling close to normal Minecraft aiming.
         *
         * Yaw:
         * - current direction feels correct, so keep it as-is.
         *
         * Pitch:
         * - subtract instead of add so moving the mouse up makes the camera look up.
         * - invertYMouse has already been handled in MouseHandlerMixin.
         */
        editYawOffset = Mth.wrapDegrees(editYawOffset + (float) vanillaYawDelta * 0.15F);
        editPitchOffset = Mth.clamp(editPitchOffset - (float) vanillaPitchDelta * 0.15F, -89.0F, 89.0F);

        applyLiveEditToClientBlockEntity();

        dirty = true;
        return true;
    }

    private static void applyLiveEditToClientBlockEntity() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || activeCameraPos == null) {
            return;
        }

        BlockEntity blockEntity = minecraft.level.getBlockEntity(activeCameraPos);
        if (blockEntity instanceof ActionCameraBlockEntity camera) {
            camera.setCameraData(
                    editOffsetX,
                    editOffsetY,
                    editOffsetZ,
                    editYawOffset,
                    editPitchOffset,
                    editRollOffset,
                    editFovOverride,
                    editSmoothing,
                    editCameraName
            );
        }
    }

    private static void sendUpdateToServer() {
        if (activeCameraPos == null) {
            return;
        }

        PacketDistributor.sendToServer(new UpdateActionCameraPayload(
                activeCameraPos,
                editOffsetX,
                editOffsetY,
                editOffsetZ,
                editYawOffset,
                editPitchOffset,
                editRollOffset,
                editFovOverride,
                editSmoothing,
                editCameraName
        ));

        dirty = false;
    }

    public static float getActiveFovOverride() {
        if (!isEditingCamera()) {
            return 0.0F;
        }

        return editFovOverride;
    }

    public static void applyToCamera(Camera camera, float partialTick) {
        if (!isEditingCamera()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || activeCameraPos == null) {
            stopEditing(false);
            return;
        }

        BlockEntity blockEntity = minecraft.level.getBlockEntity(activeCameraPos);
        if (!(blockEntity instanceof ActionCameraBlockEntity actionCamera)) {
            stopEditing(false);
            return;
        }

        BlockState state = minecraft.level.getBlockState(activeCameraPos);

        ActionCameraPose rawPose = ActionCameraPoseResolver.resolve(
                minecraft.level,
                activeCameraPos,
                state,
                actionCamera,
                partialTick
        );

        ActionCameraPose finalPose = smooth(rawPose, actionCamera.getSmoothing());
        applyCameraFields(camera, finalPose);

        lastAppliedPose = finalPose;
    }

    private static ActionCameraPose smooth(ActionCameraPose rawPose, float smoothing) {
        float clampedSmoothing = Mth.clamp(smoothing, 0.0F, 0.95F);

        if (clampedSmoothing <= 0.0F || smoothedPose == null) {
            smoothedPose = rawPose;
            return rawPose;
        }

        float alpha = 1.0F - clampedSmoothing;

        Vec3 position = smoothedPose.position().lerp(rawPose.position(), alpha);

        float yaw = lerpDegrees(alpha, smoothedPose.yaw(), rawPose.yaw());
        float pitch = Mth.lerp(alpha, smoothedPose.pitch(), rawPose.pitch());
        float roll = lerpDegrees(alpha, smoothedPose.roll(), rawPose.roll());

        ActionCameraPose result = new ActionCameraPose(
                position,
                yaw,
                pitch,
                roll,
                rawPose.fovOverride()
        );

        smoothedPose = result;
        return result;
    }

    private static float lerpDegrees(float alpha, float start, float end) {
        return start + Mth.wrapDegrees(end - start) * alpha;
    }

    private static void applyCameraFields(Camera camera, ActionCameraPose pose) {
        CameraAccessor accessor = (CameraAccessor) camera;
        accessor.droingoActionCamera$setDetached(true);

        Vec3 position = pose.position();

        accessor.droingoActionCamera$setPosition(position);
        accessor.droingoActionCamera$getBlockPosition().set(
                Mth.floor(position.x),
                Mth.floor(position.y),
                Mth.floor(position.z)
        );

        accessor.droingoActionCamera$setYRot(pose.yaw());
        accessor.droingoActionCamera$setXRot(pose.pitch());

        Quaternionf rotation = accessor.droingoActionCamera$getRotation();
        rotation.rotationYXZ(
                (float) Math.toRadians(-pose.yaw()),
                (float) Math.toRadians(pose.pitch()),
                (float) Math.toRadians(pose.roll())
        );

        accessor.droingoActionCamera$getForwards().set(0.0F, 0.0F, 1.0F).rotate(rotation);
        accessor.droingoActionCamera$getUp().set(0.0F, 1.0F, 0.0F).rotate(rotation);
        accessor.droingoActionCamera$getLeft().set(1.0F, 0.0F, 0.0F).rotate(rotation);
    }
}