package net.droingo.actioncamera.network;

import net.droingo.actioncamera.DroingoActionCamera;
import net.droingo.actioncamera.world.block.ActionCameraBlock;
import net.droingo.actioncamera.world.blockentity.ActionCameraBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

public record CameraAvailablePayload(
        BlockPos pos,
        Direction horizontalFacing,
        AttachFace attachFace,
        int mountSlot,

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
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CameraAvailablePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(DroingoActionCamera.MOD_ID, "camera_available")
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, CameraAvailablePayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public CameraAvailablePayload decode(RegistryFriendlyByteBuf buffer) {
                    BlockPos pos = buffer.readBlockPos();
                    Direction horizontalFacing = buffer.readEnum(Direction.class);
                    AttachFace attachFace = buffer.readEnum(AttachFace.class);
                    int mountSlot = buffer.readVarInt();

                    double offsetX = buffer.readDouble();
                    double offsetY = buffer.readDouble();
                    double offsetZ = buffer.readDouble();

                    float yawOffset = buffer.readFloat();
                    float pitchOffset = buffer.readFloat();
                    float rollOffset = buffer.readFloat();

                    float fovOverride = buffer.readFloat();
                    float smoothing = buffer.readFloat();

                    String cameraName = buffer.readUtf(64);

                    boolean extensionEnabled = buffer.readBoolean();
                    double extensionX = buffer.readDouble();
                    double extensionY = buffer.readDouble();
                    double extensionZ = buffer.readDouble();

                    return new CameraAvailablePayload(
                            pos,
                            horizontalFacing,
                            attachFace,
                            mountSlot,
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
                            extensionZ
                    );
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, CameraAvailablePayload payload) {
                    buffer.writeBlockPos(payload.pos());
                    buffer.writeEnum(payload.horizontalFacing());
                    buffer.writeEnum(payload.attachFace());
                    buffer.writeVarInt(payload.mountSlot());

                    buffer.writeDouble(payload.offsetX());
                    buffer.writeDouble(payload.offsetY());
                    buffer.writeDouble(payload.offsetZ());

                    buffer.writeFloat(payload.yawOffset());
                    buffer.writeFloat(payload.pitchOffset());
                    buffer.writeFloat(payload.rollOffset());

                    buffer.writeFloat(payload.fovOverride());
                    buffer.writeFloat(payload.smoothing());

                    buffer.writeUtf(payload.cameraName(), 64);

                    buffer.writeBoolean(payload.extensionEnabled());
                    buffer.writeDouble(payload.extensionX());
                    buffer.writeDouble(payload.extensionY());
                    buffer.writeDouble(payload.extensionZ());
                }
            };

    public static CameraAvailablePayload from(ActionCameraBlockEntity camera) {
        BlockState state = camera.getBlockState();

        Direction horizontalFacing = Direction.NORTH;
        AttachFace attachFace = AttachFace.FLOOR;
        int mountSlot = 4;

        if (state.hasProperty(ActionCameraBlock.HORIZONTAL_FACING)) {
            horizontalFacing = state.getValue(ActionCameraBlock.HORIZONTAL_FACING);
        }

        if (state.hasProperty(ActionCameraBlock.ATTACH_FACE)) {
            attachFace = state.getValue(ActionCameraBlock.ATTACH_FACE);
        }

        if (state.hasProperty(ActionCameraBlock.MOUNT_SLOT)) {
            mountSlot = state.getValue(ActionCameraBlock.MOUNT_SLOT);
        }

        return new CameraAvailablePayload(
                camera.getBlockPos(),
                horizontalFacing,
                attachFace,
                mountSlot,

                camera.getOffsetX(),
                camera.getOffsetY(),
                camera.getOffsetZ(),

                camera.getYawOffset(),
                camera.getPitchOffset(),
                camera.getRollOffset(),

                camera.getFovOverride(),
                camera.getSmoothing(),

                camera.getCameraName(),

                camera.isExtensionEnabled(),
                camera.getExtensionX(),
                camera.getExtensionY(),
                camera.getExtensionZ()
        );
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}