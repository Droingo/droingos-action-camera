package net.droingo.actioncamera.client;

import net.droingo.actioncamera.DroingoActionCamera;
import net.droingo.actioncamera.client.render.ActionCameraBlockEntityRenderer;
import net.droingo.actioncamera.client.render.ActionCameraClientModels;
import net.droingo.actioncamera.registry.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(
        modid = DroingoActionCamera.MOD_ID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ActionCameraKeyMappings.TOGGLE_ACTION_CAMERA_VIEW);
        event.register(ActionCameraKeyMappings.TOGGLE_EXTENSION_POLE_EDIT);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ActionCameraClientModels.FLOOR_STAND);
        event.register(ActionCameraClientModels.FLOOR_HEAD);

        event.register(ActionCameraClientModels.WALL_STAND);
        event.register(ActionCameraClientModels.WALL_HEAD);

        event.register(ActionCameraClientModels.EXTENSION_POLE);
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.ACTION_CAMERA.get(),
                ActionCameraBlockEntityRenderer::new
        );
    }
}