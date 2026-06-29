package net.droingo.actioncamera.client;

import net.droingo.actioncamera.mixin.CameraAccessor;
import net.droingo.actioncamera.network.UpdateActionCameraPayload;
import net.droingo.actioncamera.world.block.ActionCameraBlock;
import net.droingo.actioncamera.world.blockentity.ActionCameraBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public final class ActionCameraClientState {
    private enum Mode {
        NONE,
        VIEW,
        EDIT
    }

    private static final double EXTENSION_MOVE_SPEED = 0.035D;

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

    private static boolean editExtensionEnabled;
    private static double editExtensionX;
    private static double editExtensionY;
    private static double editExtensionZ;

    private static boolean extensionEditMode;

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

    public static boolean isExtensionArmEnabledForHud() {
        return isEditingCamera() && editExtensionEnabled;
    }

    public static boolean isExtensionPlacementModeForHud() {
        return isEditingCamera() && extensionEditMode;
    }

    public static double getExtensionDistanceForHud() {
        if (!isEditingCamera()) {
            return 0.0D;
        }

        return Math.sqrt(
                editExtensionX * editExtensionX
                        + editExtensionY * editExtensionY
                        + editExtensionZ * editExtensionZ
        );
    }

    public static boolean isEditingCamera() {
        return mode == Mode.EDIT && activeCameraPos != null;
    }

    public static boolean isExtensionEditMode() {
        return isEditingCamera() && extensionEditMode;
    }

    @Nullable
    public static BlockPos getActiveCameraPos() {
        return activeCameraPos;
    }

    public static String getActiveCameraLabel() {
        return activeCameraLabel == null || activeCameraLabel.isBlank()
                ? "Action Camera"
                : activeCameraLabel;
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
        activeCameraLabel = label == null || label.isBlank()
                ? camera.getCameraName()
                : label;

        mode = Mode.VIEW;
        extensionEditMode = false;

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

        editExtensionEnabled = camera.isExtensionEnabled();
        editExtensionX = camera.getExtensionX();
        editExtensionY = camera.getExtensionY();
        editExtensionZ = camera.getExtensionZ();

        extensionEditMode = false;

        forceVanillaSoundCameraTicks = 0;
        dirty = false;
        saveCooldownTicks = 0;

        wasShiftDown = true;

        smoothedPose = null;
        lastAppliedPose = null;


    }

    public static void stopViewing() {
        Minecraft minecraft = Minecraft.getInstance();

        forceVanillaAudioForExit();

        mode = Mode.NONE;
        activeCameraPos = null;
        activeCameraLabel = "Action Camera";
        extensionEditMode = false;

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
            extensionEditMode = false;
            return;
        }

        if (save) {
            sendUpdateToServer();
        }

        forceVanillaAudioForExit();

        mode = Mode.NONE;
        activeCameraPos = null;
        activeCameraLabel = "Action Camera";
        extensionEditMode = false;

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

    public static void toggleExtensionEditMode() {
        Minecraft minecraft = Minecraft.getInstance();

        if (!isEditingCamera()) {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal("Right-click a camera first to edit it."), true);
            }
            return;
        }

        extensionEditMode = !extensionEditMode;

        if (extensionEditMode) {
            /*
             * Do not force a visible pole yet.
             * The pole only becomes visible after the offset is actually moved.
             */
            editExtensionEnabled = true;
            applyLiveEditToClientBlockEntity();
            dirty = true;
        }
    }

    public static void toggleExtensionArm() {
        Minecraft minecraft = Minecraft.getInstance();

        if (!isEditingCamera()) {
            return;
        }

        boolean hasExistingArm = editExtensionEnabled
                || Math.abs(editExtensionX) > 0.0001D
                || Math.abs(editExtensionY) > 0.0001D
                || Math.abs(editExtensionZ) > 0.0001D;

        if (hasExistingArm) {
            /*
             * Arm OFF:
             * Completely remove/reset the extension.
             * This puts the camera head back on the stand and hides the pole.
             */
            editExtensionEnabled = false;
            editExtensionX = 0.0D;
            editExtensionY = 0.0D;
            editExtensionZ = 0.0D;
            extensionEditMode = false;

            applyLiveEditToClientBlockEntity();

            dirty = true;
            saveCooldownTicks = 0;



            return;
        }

        /*
         * Arm ON:
         * Enable the extension system and immediately enter extension edit mode.
         * The pole will not visibly render until the head is moved away from zero.
         */
        editExtensionEnabled = true;
        editExtensionX = 0.0D;
        editExtensionY = 0.0D;
        editExtensionZ = 0.0D;
        extensionEditMode = true;

        applyLiveEditToClientBlockEntity();

        dirty = true;
        saveCooldownTicks = 0;


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
            extensionEditMode = false;
            return;
        }

        if (!(minecraft.level.getBlockEntity(activeCameraPos) instanceof ActionCameraBlockEntity)) {
            forceVanillaAudioForExit();
            mode = Mode.NONE;
            activeCameraPos = null;
            activeCameraLabel = "Action Camera";
            extensionEditMode = false;
            return;
        }

        boolean shiftDown = minecraft.options.keyShift.isDown();

        /*
         * Shift exits normal view/edit mode.
         * While extension-pole editing is active, Shift is ignored so players do
         * not accidentally save/exit while positioning the camera head.
         */
        if (shiftDown && !wasShiftDown && !extensionEditMode) {
            stopActiveCamera();
            wasShiftDown = true;
            return;
        }

        wasShiftDown = shiftDown;

        if (isExtensionEditMode()) {
            tickExtensionMovement(minecraft);
        }

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

    private static void tickExtensionMovement(Minecraft minecraft) {
        if (minecraft.level == null || activeCameraPos == null) {
            return;
        }

        BlockState state = minecraft.level.getBlockState(activeCameraPos);

        Direction facing = state.hasProperty(ActionCameraBlock.HORIZONTAL_FACING)
                ? state.getValue(ActionCameraBlock.HORIZONTAL_FACING)
                : Direction.NORTH;

        float baseYaw = ActionCameraPoseResolver.yawForFacing(facing);
        float movementYaw = baseYaw + editYawOffset;
        double yawRadians = Math.toRadians(movementYaw);

        Vec3 forward = new Vec3(
                -Math.sin(yawRadians),
                0.0D,
                Math.cos(yawRadians)
        ).normalize();

        Vec3 right = new Vec3(
                forward.z,
                0.0D,
                -forward.x
        ).normalize();

        Vec3 delta = Vec3.ZERO;

        /*
         * FIX:
         * W/S felt reversed in game, so W now moves opposite the old forward
         * vector and S moves along it.
         */
        if (minecraft.options.keyUp.isDown()) {
            delta = delta.add(forward.scale(-EXTENSION_MOVE_SPEED));
        }

        if (minecraft.options.keyDown.isDown()) {
            delta = delta.add(forward.scale(EXTENSION_MOVE_SPEED));
        }

        if (minecraft.options.keyRight.isDown()) {
            delta = delta.add(right.scale(EXTENSION_MOVE_SPEED));
        }

        if (minecraft.options.keyLeft.isDown()) {
            delta = delta.add(right.scale(-EXTENSION_MOVE_SPEED));
        }

        if (minecraft.options.keyJump.isDown()) {
            delta = delta.add(0.0D, EXTENSION_MOVE_SPEED, 0.0D);
        }

        if (isControlDown(minecraft)) {
            delta = delta.add(0.0D, -EXTENSION_MOVE_SPEED, 0.0D);
        }

        if (delta.lengthSqr() <= 0.0D) {
            return;
        }

        Vec3 clamped = ActionCameraBlockEntity.clampExtensionOffset(new Vec3(
                editExtensionX + delta.x,
                editExtensionY + delta.y,
                editExtensionZ + delta.z
        ));

        editExtensionEnabled = true;
        editExtensionX = clamped.x;
        editExtensionY = clamped.y;
        editExtensionZ = clamped.z;

        applyLiveEditToClientBlockEntity();

        dirty = true;
    }

    private static boolean isControlDown(Minecraft minecraft) {
        long window = minecraft.getWindow().getWindow();

        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
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
                    editCameraName,
                    editExtensionEnabled,
                    editExtensionX,
                    editExtensionY,
                    editExtensionZ
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
                editCameraName,
                editExtensionEnabled,
                editExtensionX,
                editExtensionY,
                editExtensionZ
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

    public static String getEditingCameraNameForRenameScreen() {
        if (!isEditingCamera()) {
            return "Action Camera";
        }

        return editCameraName == null || editCameraName.isBlank()
                ? "Action Camera"
                : editCameraName;
    }

    public static void renameEditingCamera(String newName) {
        if (!isEditingCamera()) {
            return;
        }

        editCameraName = sanitizeClientCameraName(newName);
        activeCameraLabel = editCameraName;

        applyLiveEditToClientBlockEntity();

        dirty = true;
        saveCooldownTicks = 0;

        /*
         * Send immediately so the name is synced even before the normal edit tick.
         * stopEditing(true) will send again later, which is harmless.
         */
        sendUpdateToServer();
    }

    private static String sanitizeClientCameraName(String name) {
        if (name == null || name.isBlank()) {
            return "Action Camera";
        }

        String trimmed = name.trim();

        if (trimmed.length() > 32) {
            return trimmed.substring(0, 32);
        }

        return trimmed;
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
            extensionEditMode = false;
            return;
        }

        BlockEntity blockEntity = minecraft.level.getBlockEntity(activeCameraPos);

        if (!(blockEntity instanceof ActionCameraBlockEntity actionCamera)) {
            forceVanillaAudioForExit();
            mode = Mode.NONE;
            activeCameraPos = null;
            activeCameraLabel = "Action Camera";
            extensionEditMode = false;
            return;
        }

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