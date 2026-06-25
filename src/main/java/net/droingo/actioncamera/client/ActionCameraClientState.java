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
import org.joml.Vector3f;

public final class ActionCameraClientState {
    private enum Mode {
        NONE,
        VIEW,
        EDIT
    }

    private static Mode mode = Mode.NONE;

    private static BlockPos activeCameraPos;
    private static String activeCameraLabel = "Action Camera";

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

    /*
     * Audio fix:
     *
     * The visual Camera object is overwritten by the Action Camera.
     * Minecraft's sound manager also reads that Camera object for listener
     * position/orientation.
     *
     * We capture the normal vanilla/player camera before overwriting it.
     * SoundManagerMixin then uses this snapshot while Action Camera is active.
     *
     * Important:
     * On exit, Minecraft can still process one or more sound-listener updates
     * using stale camera state. So we keep forcing the vanilla/player listener
     * for a few frames after leaving the Action Camera.
     */
    private static CameraSnapshot lastVanillaCameraSnapshot;
    private static int forceVanillaSoundCameraTicks;

    private static final int EXIT_AUDIO_RESET_TICKS = 8;
    private static final Camera VANILLA_SOUND_CAMERA = new Camera();

    private ActionCameraClientState() {
    }

    public static boolean isActive() {
        return mode != Mode.NONE && activeCameraPos != null;
    }

    public static boolean isViewingCamera() {
        return mode == Mode.VIEW && activeCameraPos != null;
    }

    public static boolean isEditingCamera() {
        return mode == Mode.EDIT && activeCameraPos != null;
    }

    @Nullable
    public static BlockPos getActiveCameraPos() {
        return activeCameraPos;
    }

    public static String getActiveCameraLabel() {
        return activeCameraLabel == null || activeCameraLabel.isBlank() ? "Action Camera" : activeCameraLabel;
    }

    @Nullable
    public static ActionCameraPose getLastAppliedPose() {
        return lastAppliedPose;
    }

    @Nullable
    public static Camera getVanillaSoundCameraOverride() {
        boolean shouldUseOverride = isActive() || forceVanillaSoundCameraTicks > 0;

        if (!shouldUseOverride) {
            return null;
        }

        if (lastVanillaCameraSnapshot == null) {
            return null;
        }

        lastVanillaCameraSnapshot.apply(VANILLA_SOUND_CAMERA);
        return VANILLA_SOUND_CAMERA;
    }

    public static void startViewing(BlockPos pos) {
        startViewing(pos, null);
    }

    public static void startViewing(BlockPos pos, @Nullable String label) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        if (!(minecraft.level.getBlockEntity(pos) instanceof ActionCameraBlockEntity camera)) {
            return;
        }

        activeCameraPos = pos.immutable();
        activeCameraLabel = label == null || label.isBlank() ? camera.getCameraName() : label;
        mode = Mode.VIEW;

        forceVanillaSoundCameraTicks = 0;
        dirty = false;
        saveCooldownTicks = 0;
        wasShiftDown = minecraft.options.keyShift.isDown();

        smoothedPose = null;
        lastAppliedPose = null;

        minecraft.player.displayClientMessage(Component.literal(getActiveCameraLabel()), true);
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
        activeCameraLabel = camera.getCameraName();
        mode = Mode.EDIT;

        editOffsetX = camera.getOffsetX();
        editOffsetY = camera.getOffsetY();
        editOffsetZ = camera.getOffsetZ();
        editYawOffset = camera.getYawOffset();
        editPitchOffset = camera.getPitchOffset();
        editRollOffset = camera.getRollOffset();
        editFovOverride = camera.getFovOverride();
        editSmoothing = camera.getSmoothing();
        editCameraName = camera.getCameraName();

        forceVanillaSoundCameraTicks = 0;
        dirty = false;
        saveCooldownTicks = 0;
        wasShiftDown = true;

        smoothedPose = null;
        lastAppliedPose = null;

        minecraft.player.displayClientMessage(
                Component.literal("Action Camera edit mode.\nMove mouse to aim.\nPress Shift to save and exit."),
                true
        );
    }

    public static void stopViewing() {
        Minecraft minecraft = Minecraft.getInstance();

        forceVanillaAudioForExit();

        mode = Mode.NONE;
        activeCameraPos = null;
        activeCameraLabel = "Action Camera";

        smoothedPose = null;
        lastAppliedPose = null;
        dirty = false;

        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("Action Camera disabled"), true);
        }
    }

    public static void stopEditing(boolean save) {
        Minecraft minecraft = Minecraft.getInstance();

        if (activeCameraPos == null) {
            forceVanillaAudioForExit();

            mode = Mode.NONE;
            activeCameraLabel = "Action Camera";
            return;
        }

        if (save) {
            sendUpdateToServer();
        }

        forceVanillaAudioForExit();

        mode = Mode.NONE;
        activeCameraPos = null;
        activeCameraLabel = "Action Camera";

        smoothedPose = null;
        lastAppliedPose = null;
        dirty = false;

        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal("Action Camera saved"), true);
        }
    }

    public static void stopActiveCamera() {
        if (isEditingCamera()) {
            stopEditing(true);
        } else if (isViewingCamera()) {
            stopViewing();
        }
    }

    public static void clientTick() {
        Minecraft minecraft = Minecraft.getInstance();

        if (forceVanillaSoundCameraTicks > 0) {
            forceVanillaSoundCameraTicks--;
        }

        if (!isActive()) {
            wasShiftDown = minecraft.options.keyShift.isDown();
            return;
        }

        if (minecraft.level == null || activeCameraPos == null) {
            forceVanillaAudioForExit();

            mode = Mode.NONE;
            activeCameraPos = null;
            activeCameraLabel = "Action Camera";
            return;
        }

        if (!(minecraft.level.getBlockEntity(activeCameraPos) instanceof ActionCameraBlockEntity)) {
            forceVanillaAudioForExit();

            mode = Mode.NONE;
            activeCameraPos = null;
            activeCameraLabel = "Action Camera";
            return;
        }

        boolean shiftDown = minecraft.options.keyShift.isDown();

        if (shiftDown && !wasShiftDown) {
            stopActiveCamera();
            wasShiftDown = true;
            return;
        }

        wasShiftDown = shiftDown;

        if (isEditingCamera() && dirty) {
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
        if (!isActive()) {
            return 0.0F;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || activeCameraPos == null) {
            return 0.0F;
        }

        if (minecraft.level.getBlockEntity(activeCameraPos) instanceof ActionCameraBlockEntity camera) {
            return camera.getFovOverride();
        }

        return 0.0F;
    }

    public static void applyToCamera(Camera camera, float partialTick) {
        if (!isActive()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.level == null || activeCameraPos == null) {
            forceVanillaAudioForExit();

            mode = Mode.NONE;
            activeCameraPos = null;
            activeCameraLabel = "Action Camera";
            return;
        }

        BlockEntity blockEntity = minecraft.level.getBlockEntity(activeCameraPos);

        if (!(blockEntity instanceof ActionCameraBlockEntity actionCamera)) {
            forceVanillaAudioForExit();

            mode = Mode.NONE;
            activeCameraPos = null;
            activeCameraLabel = "Action Camera";
            return;
        }

        /*
         * CameraMixin calls this at the tail of Camera.setup(...), before we
         * overwrite the Camera with our cinematic pose. So at this point the
         * Camera still represents the normal player/spectator camera.
         */
        captureVanillaCameraSnapshot(camera);

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

        /*
         * Force detached camera mode so the local player still renders.
         */
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

    private static void captureVanillaCameraSnapshot(Camera camera) {
        CameraAccessor accessor = (CameraAccessor) camera;

        Vec3 position = camera.getPosition();
        BlockPos blockPosition = camera.getBlockPosition();

        lastVanillaCameraSnapshot = new CameraSnapshot(
                position,
                blockPosition.getX(),
                blockPosition.getY(),
                blockPosition.getZ(),
                camera.getXRot(),
                camera.getYRot(),
                new Quaternionf(accessor.droingoActionCamera$getRotation()),
                new Vector3f(accessor.droingoActionCamera$getForwards()),
                new Vector3f(accessor.droingoActionCamera$getUp()),
                new Vector3f(accessor.droingoActionCamera$getLeft()),
                accessor.droingoActionCamera$isDetached()
        );
    }

    private static void forceVanillaAudioForExit() {
        if (lastVanillaCameraSnapshot != null) {
            forceVanillaSoundCameraTicks = EXIT_AUDIO_RESET_TICKS;
        }
    }

    private record CameraSnapshot(
            Vec3 position,
            int blockX,
            int blockY,
            int blockZ,
            float xRot,
            float yRot,
            Quaternionf rotation,
            Vector3f forwards,
            Vector3f up,
            Vector3f left,
            boolean detached
    ) {
        private void apply(Camera camera) {
            CameraAccessor accessor = (CameraAccessor) camera;

            accessor.droingoActionCamera$setPosition(position);
            accessor.droingoActionCamera$getBlockPosition().set(blockX, blockY, blockZ);
            accessor.droingoActionCamera$setXRot(xRot);
            accessor.droingoActionCamera$setYRot(yRot);
            accessor.droingoActionCamera$getRotation().set(rotation);
            accessor.droingoActionCamera$getForwards().set(forwards);
            accessor.droingoActionCamera$getUp().set(up);
            accessor.droingoActionCamera$getLeft().set(left);
            accessor.droingoActionCamera$setDetached(detached);
        }
    }
}