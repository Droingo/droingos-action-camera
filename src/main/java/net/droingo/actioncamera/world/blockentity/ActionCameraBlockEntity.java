package net.droingo.actioncamera.world.blockentity;

import net.droingo.actioncamera.network.ModNetworking;
import net.droingo.actioncamera.registry.ModBlockEntities;
import net.droingo.actioncamera.world.ActionCameraKnownCameras;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ActionCameraBlockEntity extends BlockEntity {
    public static final double DEFAULT_MAX_EXTENSION_DISTANCE = 1.5D;
    public static final double MIN_MAX_EXTENSION_DISTANCE = 0.25D;
    public static final double MAX_EXTENSION_DISTANCE = 5.0D;

    private static final int MAX_CAMERA_NAME_LENGTH = 32;

    private double offsetX;
    private double offsetY;
    private double offsetZ;

    private float yawOffset;
    private float pitchOffset;
    private float rollOffset;

    private float fovOverride;
    private float smoothing;

    private boolean extensionEnabled;
    private double extensionX;
    private double extensionY;
    private double extensionZ;

    private boolean externalRigVisible = true;
    private double maxExtensionDistance = DEFAULT_MAX_EXTENSION_DISTANCE;
    private boolean cameraNameAlwaysVisible;

    private String cameraName = "Action Camera";

    public ActionCameraBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ACTION_CAMERA.get(), pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        /*
         * Auto-discovery for C cycling.
         *
         * Client side:
         * Keeps normal loaded-camera discovery working.
         *
         * Server side:
         * Sends a passive clientbound packet that ReplayMod can record.
         * This packet does not force the player into the camera.
         */
        ActionCameraKnownCameras.register(worldPosition);

        if (level != null && !level.isClientSide()) {
            ModNetworking.sendCameraAvailable(this);
        }
    }

    @Override
    public void setRemoved() {
        ActionCameraKnownCameras.unregister(worldPosition);
        super.setRemoved();
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getOffsetZ() {
        return offsetZ;
    }

    public float getYawOffset() {
        return yawOffset;
    }

    public float getPitchOffset() {
        return pitchOffset;
    }

    public float getRollOffset() {
        return rollOffset;
    }

    public float getFovOverride() {
        return fovOverride;
    }

    public float getSmoothing() {
        return smoothing;
    }

    public boolean isExtensionEnabled() {
        return extensionEnabled;
    }

    public double getExtensionX() {
        return extensionEnabled ? extensionX : 0.0D;
    }

    public double getExtensionY() {
        return extensionEnabled ? extensionY : 0.0D;
    }

    public double getExtensionZ() {
        return extensionEnabled ? extensionZ : 0.0D;
    }

    public Vec3 getExtensionOffset() {
        return extensionEnabled ? new Vec3(extensionX, extensionY, extensionZ) : Vec3.ZERO;
    }

    public boolean isExternalRigVisible() {
        return externalRigVisible;
    }

    public double getMaxExtensionDistance() {
        return maxExtensionDistance;
    }

    public boolean isCameraNameAlwaysVisible() {
        return cameraNameAlwaysVisible;
    }

    public String getCameraName() {
        return cameraName;
    }

    public void setCameraData(
            double offsetX,
            double offsetY,
            double offsetZ,
            float yawOffset,
            float pitchOffset,
            float rollOffset,
            float fovOverride,
            float smoothing,
            String cameraName
    ) {
        setCameraData(
                offsetX,
                offsetY,
                offsetZ,
                yawOffset,
                pitchOffset,
                rollOffset,
                fovOverride,
                smoothing,
                cameraName,
                extensionEnabled,
                extensionX,
                extensionY,
                extensionZ,
                externalRigVisible,
                maxExtensionDistance,
                cameraNameAlwaysVisible
        );
    }

    public void setCameraData(
            double offsetX,
            double offsetY,
            double offsetZ,
            float yawOffset,
            float pitchOffset,
            float rollOffset,
            float fovOverride,
            float smoothing,
            String cameraName,
            boolean extensionEnabled,
            double extensionX,
            double extensionY,
            double extensionZ
    ) {
        setCameraData(
                offsetX,
                offsetY,
                offsetZ,
                yawOffset,
                pitchOffset,
                rollOffset,
                fovOverride,
                smoothing,
                cameraName,
                extensionEnabled,
                extensionX,
                extensionY,
                extensionZ,
                externalRigVisible,
                maxExtensionDistance,
                cameraNameAlwaysVisible
        );
    }

    public void setCameraData(
            double offsetX,
            double offsetY,
            double offsetZ,
            float yawOffset,
            float pitchOffset,
            float rollOffset,
            float fovOverride,
            float smoothing,
            String cameraName,
            boolean extensionEnabled,
            double extensionX,
            double extensionY,
            double extensionZ,
            boolean externalRigVisible,
            double maxExtensionDistance,
            boolean cameraNameAlwaysVisible
    ) {
        this.offsetX = Mth.clamp(offsetX, -0.5D, 0.5D);
        this.offsetY = Mth.clamp(offsetY, -0.5D, 0.5D);
        this.offsetZ = Mth.clamp(offsetZ, -0.5D, 0.5D);

        this.yawOffset = Mth.wrapDegrees(yawOffset);
        this.pitchOffset = Mth.clamp(pitchOffset, -89.0F, 89.0F);
        this.rollOffset = Mth.wrapDegrees(rollOffset);

        this.fovOverride = Mth.clamp(fovOverride, 0.0F, 170.0F);
        this.smoothing = Mth.clamp(smoothing, 0.0F, 0.95F);

        this.cameraName = sanitizeCameraName(cameraName);
        this.extensionEnabled = extensionEnabled;
        this.externalRigVisible = externalRigVisible;
        this.maxExtensionDistance = clampMaxExtensionDistance(maxExtensionDistance);
        this.cameraNameAlwaysVisible = cameraNameAlwaysVisible;

        Vec3 clampedExtension = clampExtensionOffset(
                new Vec3(extensionX, extensionY, extensionZ),
                this.maxExtensionDistance
        );

        this.extensionX = clampedExtension.x;
        this.extensionY = clampedExtension.y;
        this.extensionZ = clampedExtension.z;

        setChanged();

        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            ModNetworking.sendCameraAvailable(this);
        }
    }

    public void setExtensionData(boolean enabled, double x, double y, double z) {
        Vec3 clampedExtension = clampExtensionOffset(new Vec3(x, y, z), maxExtensionDistance);

        this.extensionEnabled = enabled;
        this.extensionX = clampedExtension.x;
        this.extensionY = clampedExtension.y;
        this.extensionZ = clampedExtension.z;

        setChanged();

        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            ModNetworking.sendCameraAvailable(this);
        }
    }

    public static double clampMaxExtensionDistance(double distance) {
        return Mth.clamp(distance, MIN_MAX_EXTENSION_DISTANCE, MAX_EXTENSION_DISTANCE);
    }

    public static Vec3 clampExtensionOffset(Vec3 offset) {
        return clampExtensionOffset(offset, DEFAULT_MAX_EXTENSION_DISTANCE);
    }

    public static Vec3 clampExtensionOffset(Vec3 offset, double maxDistance) {
        double clampedMaxDistance = clampMaxExtensionDistance(maxDistance);
        double length = offset.length();

        if (length <= clampedMaxDistance || length <= 0.000001D) {
            return offset;
        }

        return offset.scale(clampedMaxDistance / length);
    }

    private static String sanitizeCameraName(String name) {
        if (name == null || name.isBlank()) {
            return "Action Camera";
        }

        String trimmed = name.trim();

        if (trimmed.length() > MAX_CAMERA_NAME_LENGTH) {
            return trimmed.substring(0, MAX_CAMERA_NAME_LENGTH);
        }

        return trimmed;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putDouble("OffsetX", offsetX);
        tag.putDouble("OffsetY", offsetY);
        tag.putDouble("OffsetZ", offsetZ);

        tag.putFloat("YawOffset", yawOffset);
        tag.putFloat("PitchOffset", pitchOffset);
        tag.putFloat("RollOffset", rollOffset);

        tag.putFloat("FovOverride", fovOverride);
        tag.putFloat("Smoothing", smoothing);

        tag.putString("CameraName", cameraName);

        tag.putBoolean("ExtensionEnabled", extensionEnabled);
        tag.putDouble("ExtensionX", extensionX);
        tag.putDouble("ExtensionY", extensionY);
        tag.putDouble("ExtensionZ", extensionZ);

        tag.putBoolean("ExternalRigVisible", externalRigVisible);
        tag.putDouble("MaxExtensionDistance", maxExtensionDistance);
        tag.putBoolean("CameraNameAlwaysVisible", cameraNameAlwaysVisible);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        offsetX = Mth.clamp(tag.getDouble("OffsetX"), -0.5D, 0.5D);
        offsetY = Mth.clamp(tag.getDouble("OffsetY"), -0.5D, 0.5D);
        offsetZ = Mth.clamp(tag.getDouble("OffsetZ"), -0.5D, 0.5D);

        yawOffset = Mth.wrapDegrees(tag.getFloat("YawOffset"));
        pitchOffset = Mth.clamp(tag.getFloat("PitchOffset"), -89.0F, 89.0F);
        rollOffset = Mth.wrapDegrees(tag.getFloat("RollOffset"));

        fovOverride = Mth.clamp(tag.getFloat("FovOverride"), 0.0F, 170.0F);
        smoothing = Mth.clamp(tag.getFloat("Smoothing"), 0.0F, 0.95F);

        if (tag.contains("CameraName")) {
            cameraName = sanitizeCameraName(tag.getString("CameraName"));
        } else {
            cameraName = "Action Camera";
        }

        extensionEnabled = tag.getBoolean("ExtensionEnabled");

        externalRigVisible = !tag.contains("ExternalRigVisible") || tag.getBoolean("ExternalRigVisible");
        maxExtensionDistance = tag.contains("MaxExtensionDistance")
                ? clampMaxExtensionDistance(tag.getDouble("MaxExtensionDistance"))
                : DEFAULT_MAX_EXTENSION_DISTANCE;
        cameraNameAlwaysVisible = tag.contains("CameraNameAlwaysVisible")
                && tag.getBoolean("CameraNameAlwaysVisible");

        Vec3 clampedExtension = clampExtensionOffset(
                new Vec3(
                        tag.getDouble("ExtensionX"),
                        tag.getDouble("ExtensionY"),
                        tag.getDouble("ExtensionZ")
                ),
                maxExtensionDistance
        );

        extensionX = clampedExtension.x;
        extensionY = clampedExtension.y;
        extensionZ = clampedExtension.z;

        /*
         * Re-register after NBT load too. This covers chunk/replay reload paths
         * where onLoad timing differs between vanilla, server, and ReplayMod.
         */
        ActionCameraKnownCameras.register(worldPosition);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
