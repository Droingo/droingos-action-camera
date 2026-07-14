package net.droingo.actioncamera.network;

import net.droingo.actioncamera.DroingoActionCamera;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateActionCameraPayload(
        BlockPos pos,
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
        boolean cameraNameAlwaysVisible,
        boolean horizonLevelingEnabled
) implements CustomPacketPayload {
    public static final Type<UpdateActionCameraPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(DroingoActionCamera.MOD_ID, "update_action_camera")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateActionCameraPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public UpdateActionCameraPayload decode(RegistryFriendlyByteBuf buffer) {
            return new UpdateActionCameraPayload(
                    buffer.readBlockPos(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readFloat(),
                    buffer.readUtf(64),
                    buffer.readBoolean(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readDouble(),
                    buffer.readBoolean(),
                    buffer.readDouble(),
                    buffer.readBoolean(),
                    buffer.readBoolean()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, UpdateActionCameraPayload payload) {
            buffer.writeBlockPos(payload.pos());

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

            buffer.writeBoolean(payload.externalRigVisible());
            buffer.writeDouble(payload.maxExtensionDistance());
            buffer.writeBoolean(payload.cameraNameAlwaysVisible());
            buffer.writeBoolean(payload.horizonLevelingEnabled());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
