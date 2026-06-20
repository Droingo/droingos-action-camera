package net.droingo.actioncamera.compat;

import net.droingo.actioncamera.client.ActionCameraPose;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sable/Create Aeronautics compatibility hook.
 *
 * For now this returns the static-world pose.
 * Later we will transform this pose through the Sable client render pose.
 */
public final class ActionCameraSublevelHooks {
    private ActionCameraSublevelHooks() {
    }

    public static ActionCameraPose transformCameraPoseIfNeeded(
            Level level,
            BlockPos cameraBlockPos,
            BlockState cameraBlockState,
            ActionCameraPose basePose,
            float partialTick
    ) {
        return basePose;
    }
}