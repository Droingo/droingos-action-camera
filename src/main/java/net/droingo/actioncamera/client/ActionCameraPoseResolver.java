package net.droingo.actioncamera.client;

import net.droingo.actioncamera.compat.ActionCameraSublevelHooks;
import net.droingo.actioncamera.world.block.ActionCameraBlock;
import net.droingo.actioncamera.world.blockentity.ActionCameraBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ActionCameraPoseResolver {
    private ActionCameraPoseResolver() {
    }

    public static ActionCameraPose resolve(
            Level level,
            BlockPos pos,
            BlockState state,
            ActionCameraBlockEntity camera,
            float partialTick
    ) {
        Direction facing = state.hasProperty(ActionCameraBlock.HORIZONTAL_FACING)
                ? state.getValue(ActionCameraBlock.HORIZONTAL_FACING)
                : Direction.NORTH;

        float baseYaw = yawForFacing(facing);
        float basePitch = 0.0F;

        Vector3f localOffset = new Vector3f(
                (float) camera.getOffsetX(),
                (float) camera.getOffsetY(),
                (float) camera.getOffsetZ()
        );

        Quaternionf blockRotation = new Quaternionf().rotationYXZ(
                (float) Math.toRadians(-baseYaw),
                (float) Math.toRadians(basePitch),
                0.0F
        );

        localOffset.rotate(blockRotation);

        Vec3 worldPosition = Vec3.atCenterOf(pos).add(
                localOffset.x(),
                localOffset.y(),
                localOffset.z()
        );

        ActionCameraPose basePose = new ActionCameraPose(
                worldPosition,
                baseYaw + camera.getYawOffset(),
                basePitch + camera.getPitchOffset(),
                camera.getRollOffset(),
                camera.getFovOverride()
        );

        return ActionCameraSublevelHooks.transformCameraPoseIfNeeded(
                level,
                pos,
                state,
                basePose,
                partialTick
        );
    }

    private static float yawForFacing(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            default -> 180.0F;
        };
    }
}