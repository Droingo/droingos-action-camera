package net.droingo.actioncamera.client.render;

import net.droingo.actioncamera.DroingoActionCamera;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

public final class ActionCameraClientModels {
    public static final ModelResourceLocation FLOOR_STAND = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(DroingoActionCamera.MOD_ID, "block/action_camera_floor_stand")
    );

    public static final ModelResourceLocation FLOOR_HEAD = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(DroingoActionCamera.MOD_ID, "block/action_camera_floor_head")
    );

    public static final ModelResourceLocation WALL_STAND = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(DroingoActionCamera.MOD_ID, "block/action_camera_wall_stand")
    );

    public static final ModelResourceLocation WALL_HEAD = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(DroingoActionCamera.MOD_ID, "block/action_camera_wall_head")
    );

    public static final ModelResourceLocation EXTENSION_POLE = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(DroingoActionCamera.MOD_ID, "block/action_camera_extension_pole")
    );

    private ActionCameraClientModels() {
    }
}
