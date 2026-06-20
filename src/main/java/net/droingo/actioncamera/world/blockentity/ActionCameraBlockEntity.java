package net.droingo.actioncamera.world.blockentity;

import net.droingo.actioncamera.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class ActionCameraBlockEntity extends BlockEntity {
    private double offsetX = 0.0D;
    private double offsetY = 0.0D;
    private double offsetZ = 0.0D;

    private float yawOffset = 0.0F;
    private float pitchOffset = 0.0F;
    private float rollOffset = 0.0F;

    private float fovOverride = 0.0F;
    private float smoothing = 0.0F;

    private String cameraName = "Action Camera";

    public ActionCameraBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ACTION_CAMERA.get(), pos, blockState);
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
        this.offsetX = Mth.clamp(offsetX, -0.5D, 0.5D);
        this.offsetY = Mth.clamp(offsetY, -0.5D, 0.5D);
        this.offsetZ = Mth.clamp(offsetZ, -0.5D, 0.5D);

        this.yawOffset = Mth.wrapDegrees(yawOffset);
        this.pitchOffset = Mth.clamp(pitchOffset, -89.0F, 89.0F);
        this.rollOffset = Mth.wrapDegrees(rollOffset);

        this.fovOverride = Mth.clamp(fovOverride, 0.0F, 170.0F);
        this.smoothing = Mth.clamp(smoothing, 0.0F, 0.95F);

        String safeName = cameraName == null ? "Action Camera" : cameraName.trim();
        if (safeName.isEmpty()) {
            safeName = "Action Camera";
        }
        if (safeName.length() > 32) {
            safeName = safeName.substring(0, 32);
        }

        this.cameraName = safeName;
        setChanged();

        if (this.level != null && !this.level.isClientSide) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
        }
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("OffsetX", Tag.TAG_DOUBLE)) {
            offsetX = tag.getDouble("OffsetX");
        }
        if (tag.contains("OffsetY", Tag.TAG_DOUBLE)) {
            offsetY = tag.getDouble("OffsetY");
        }
        if (tag.contains("OffsetZ", Tag.TAG_DOUBLE)) {
            offsetZ = tag.getDouble("OffsetZ");
        }

        if (tag.contains("YawOffset", Tag.TAG_FLOAT)) {
            yawOffset = tag.getFloat("YawOffset");
        }
        if (tag.contains("PitchOffset", Tag.TAG_FLOAT)) {
            pitchOffset = tag.getFloat("PitchOffset");
        }
        if (tag.contains("RollOffset", Tag.TAG_FLOAT)) {
            rollOffset = tag.getFloat("RollOffset");
        }

        if (tag.contains("FovOverride", Tag.TAG_FLOAT)) {
            fovOverride = tag.getFloat("FovOverride");
        }
        if (tag.contains("Smoothing", Tag.TAG_FLOAT)) {
            smoothing = tag.getFloat("Smoothing");
        }
        if (tag.contains("CameraName", Tag.TAG_STRING)) {
            cameraName = tag.getString("CameraName");
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
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