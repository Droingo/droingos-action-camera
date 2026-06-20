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

@EventBusSubscriber(
        modid = DroingoActionCamera.MOD_ID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ActionCameraClientModels.FLOOR_HEAD);
        event.register(ActionCameraClientModels.WALL_HEAD);
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.ACTION_CAMERA.get(),
                ActionCameraBlockEntityRenderer::new
        );
    }
}