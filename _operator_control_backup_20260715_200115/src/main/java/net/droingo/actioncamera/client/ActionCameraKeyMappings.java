package net.droingo.actioncamera.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class ActionCameraKeyMappings {
    public static final String CATEGORY = "key.categories.droingo_action_camera";

    public static final KeyMapping TOGGLE_ACTION_CAMERA_VIEW = new KeyMapping(
            "key.droingo_action_camera.toggle_action_camera_view",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY
    );

    public static final KeyMapping RENAME_ACTION_CAMERA = new KeyMapping(
            "key.droingo_action_camera.rename_action_camera",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY
    );

    public static final KeyMapping CYCLE_EDIT_OVERLAY_MODE = new KeyMapping(
            "key.droingo_action_camera.cycle_edit_overlay_mode",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_EXTENSION_POLE_EDIT = new KeyMapping(
            "key.droingo_action_camera.toggle_extension_pole_edit",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    private ActionCameraKeyMappings() {
    }
}