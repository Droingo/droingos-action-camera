package net.droingo.actioncamera.client;

import net.droingo.actioncamera.compat.ActionCameraSublevelHooks;
import net.droingo.actioncamera.world.block.ActionCameraBlock;
import net.droingo.actioncamera.world.block.ActionCameraMountSlot;
import net.droingo.actioncamera.world.blockentity.ActionCameraBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class ActionCameraPoseResolver {
    private static final double CAMERA_VIEW_FORWARD_OFFSET = 4.0D / 16.0D;

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

        AttachFace attachFace = state.hasProperty(ActionCameraBlock.ATTACH_FACE)
                ? state.getValue(ActionCameraBlock.ATTACH_FACE)
                : AttachFace.FLOOR;

        int mountSlot = state.hasProperty(ActionCameraBlock.MOUNT_SLOT)
                ? state.getValue(ActionCameraBlock.MOUNT_SLOT)
                : ActionCameraMountSlot.SLOT_CENTER;

        float baseYaw = yawForFacing(facing);
        float basePitch = 0.0F;

        float finalYaw = baseYaw + camera.getYawOffset();
        float finalPitch = basePitch + camera.getPitchOffset();
        float finalRoll = camera.getRollOffset();

        Vec3 slotOffset = ActionCameraMountSlot.worldAssemblyOffset(attachFace, facing, mountSlot);

        Quaternionf cameraRotation = new Quaternionf().rotationYXZ(
                (float) Math.toRadians(-finalYaw),
                (float) Math.toRadians(finalPitch),
                (float) Math.toRadians(finalRoll)
        );

        Vector3f forwardViewOffset = new Vector3f(
                0.0F,
                0.0F,
                (float) CAMERA_VIEW_FORWARD_OFFSET
        ).rotate(cameraRotation);

        Vec3 worldPosition = Vec3.atCenterOf(pos).add(
                slotOffset.x + camera.getOffsetX() + forwardViewOffset.x(),
                slotOffset.y + camera.getOffsetY() + forwardViewOffset.y(),
                slotOffset.z + camera.getOffsetZ() + forwardViewOffset.z()
        );

        ActionCameraPose basePose = new ActionCameraPose(
                worldPosition,
                finalYaw,
                finalPitch,
                finalRoll,
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