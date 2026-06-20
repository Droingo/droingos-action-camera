package net.droingo.actioncamera.world.blockentity;

import net.droingo.actioncamera.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
        this.offsetX = Mth.clamp(offsetX, -32.0D, 32.0D);
        this.offsetY = Mth.clamp(offsetY, -32.0D, 32.0D);
        this.offsetZ = Mth.clamp(offsetZ, -32.0D, 32.0D);

        this.yawOffset = Mth.wrapDegrees(yawOffset);
        this.pitchOffset = Mth.clamp(pitchOffset, -180.0F, 180.0F);
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
}