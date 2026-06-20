package net.droingo.actioncamera.network;

import net.droingo.actioncamera.world.blockentity.ActionCameraBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
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
    }

    private static void handleUpdateActionCamera(UpdateActionCameraPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            if (!player.level().isLoaded(payload.pos())) {
                return;
            }

            double maxDistance = 64.0D;
            if (player.distanceToSqr(Vec3.atCenterOf(payload.pos())) > maxDistance * maxDistance) {
                return;
            }

            if (player.level().getBlockEntity(payload.pos()) instanceof ActionCameraBlockEntity camera) {
                camera.setCameraData(
                        payload.offsetX(),
                        payload.offsetY(),
                        payload.offsetZ(),
                        payload.yawOffset(),
                        payload.pitchOffset(),
                        payload.rollOffset(),
                        payload.fovOverride(),
                        payload.smoothing(),
                        payload.cameraName()
                );
            }
        });
    }
}