package net.droingo.actioncamera.network;

import net.droingo.actioncamera.world.ActionCameraKnownCameras;
import net.droingo.actioncamera.world.blockentity.ActionCameraBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetworking {
    private static final String NETWORK_VERSION = "1";

    private ModNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);

        registrar.playToServer(
                UpdateActionCameraPayload.TYPE,
                UpdateActionCameraPayload.STREAM_CODEC,
                ModNetworking::handleUpdateActionCamera
        );

        registrar.playToClient(
                CameraAvailablePayload.TYPE,
                CameraAvailablePayload.STREAM_CODEC,
                ModNetworking::handleCameraAvailable
        );
    }

    public static void sendCameraAvailable(ActionCameraBlockEntity camera) {
        if (!(camera.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        CameraAvailablePayload payload = CameraAvailablePayload.from(camera);

        PacketDistributor.sendToPlayersTrackingChunk(
                serverLevel,
                new ChunkPos(camera.getBlockPos()),
                payload
        );
    }

    private static void handleCameraAvailable(CameraAvailablePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            /*
             * Replay-safe passive registration.
             *
             * This packet must NOT enter the camera view.
             * It only tells the client/replay that this camera exists.
             */
            ActionCameraKnownCameras.register(payload.pos());
        });
    }

    private static void handleUpdateActionCamera(UpdateActionCameraPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            if (!(player.level() instanceof ServerLevel serverLevel)) {
                return;
            }

            BlockPos pos = payload.pos();

            if (!serverLevel.isLoaded(pos)) {
                return;
            }

            double maxDistance = 64.0D;

            if (player.distanceToSqr(Vec3.atCenterOf(pos)) > maxDistance * maxDistance) {
                return;
            }

            if (!(serverLevel.getBlockEntity(pos) instanceof ActionCameraBlockEntity camera)) {
                return;
            }

            camera.setCameraData(
                    payload.offsetX(),
                    payload.offsetY(),
                    payload.offsetZ(),
                    payload.yawOffset(),
                    payload.pitchOffset(),
                    payload.rollOffset(),
                    payload.fovOverride(),
                    payload.smoothing(),
                    payload.cameraName(),
                    payload.extensionEnabled(),
                    payload.extensionX(),
                    payload.extensionY(),
                    payload.extensionZ(),
                    payload.externalRigVisible(),
                    payload.maxExtensionDistance(),
                    payload.cameraNameAlwaysVisible()
            );
        });
    }
}
