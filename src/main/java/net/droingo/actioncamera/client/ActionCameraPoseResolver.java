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

public final class ActionCameraPoseResolver {
    private static final double BLOCK_CENTER = 0.5D;

    /*
     * These should match the renderer's pole/head anchor point.
     * This is the physical point the extension pole uses.
     */
    private static final double FLOOR_HEAD_HINGE_X = 8.0D / 16.0D;
    private static final double FLOOR_HEAD_HINGE_Y = 2.0D / 16.0D;
    private static final double FLOOR_HEAD_HINGE_Z = 7.0D / 16.0D;

    private static final double WALL_HEAD_HINGE_X = 8.0D / 16.0D;
    private static final double WALL_HEAD_HINGE_Y = 7.25D / 16.0D;
    private static final double WALL_HEAD_HINGE_Z = 14.6D / 16.0D;

    /*
     * Important:
     * This is a fixed positional nudge, not a rotated forward/lens offset.
     *
     * The previous forward-based offset caused the camera to orbit when aiming.
     * This keeps the camera position stable while yaw/pitch only changes view direction.
     */
    private static final double CAMERA_FIXED_Y_PUSH = 2.0D / 16.0D;

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

        /*
         * Fixed camera position.
         *
         * This uses the same pole/head anchor point as the renderer, adds the
         * extension offset, then adds only a small static Y lift.
         *
         * There is intentionally NO forward vector offset here.
         * Rotation must not move the camera origin.
         */
        Vec3 anchorLocal = getPoleCameraAnchorBlockLocal(
                camera,
                attachFace,
                facing,
                mountSlot
        );

        Vec3 worldPosition = Vec3.atLowerCornerOf(pos)
                .add(anchorLocal)
                .add(camera.getExtensionOffset())
                .add(cameraFixedOffsetBlockLocal(attachFace, facing));

        ActionCameraPose basePose = new ActionCameraPose(
                worldPosition,
                finalYaw,
                finalPitch,
                finalRoll,
                camera.getFovOverride()
        );

        ActionCameraPose transformedPose = ActionCameraSublevelHooks.transformCameraPoseIfNeeded(
                level,
                pos,
                state,
                basePose,
                partialTick
        );

        /*
         * Sable-safe horizon lock.
         *
         * Setting local roll to 0 before this point is not enough, because a
         * Sable sublevel can rotate the final rendered camera and introduce a
         * new world-space roll. The final roll must be clamped after the
         * sublevel transform.
         */
        if (camera.isHorizonLevelingEnabled()) {
            return new ActionCameraPose(
                    transformedPose.position(),
                    transformedPose.yaw(),
                    transformedPose.pitch(),
                    0.0F,
                    transformedPose.fovOverride()
            );
        }

        return transformedPose;
    }

    private static Vec3 getPoleCameraAnchorBlockLocal(
            ActionCameraBlockEntity blockEntity,
            AttachFace attachFace,
            Direction facing,
            int mountSlot
    ) {
        /*
         * This matches the renderer's pole positioning path.
         * Use worldAssemblyOffset because that is where the visible model/pole is.
         */
        Vec3 slotOffset = ActionCameraMountSlot.worldAssemblyOffset(attachFace, facing, mountSlot);

        Vec3 hingeBeforeMountRotation = new Vec3(
                hingeXForAttachFace(attachFace),
                hingeYForAttachFace(attachFace),
                hingeZForAttachFace(attachFace)
        );

        Vec3 hingeAfterMountRotation = rotatePointAroundBlockCenter(
                hingeBeforeMountRotation,
                attachFace,
                facing
        );

        return new Vec3(
                slotOffset.x + blockEntity.getOffsetX() + hingeAfterMountRotation.x,
                slotOffset.y + blockEntity.getOffsetY() + hingeAfterMountRotation.y,
                slotOffset.z + blockEntity.getOffsetZ() + hingeAfterMountRotation.z
        );
    }

    private static Vec3 rotatePointAroundBlockCenter(
            Vec3 point,
            AttachFace attachFace,
            Direction facing
    ) {
        Vec3 shifted = point.subtract(BLOCK_CENTER, BLOCK_CENTER, BLOCK_CENTER);

        Vec3 rotated = switch (attachFace) {
            case FLOOR -> rotateY(shifted, yRotationForFacing(facing));
            case WALL -> rotateY(shifted, wallYRotationForFacing(facing));
            case CEILING -> {
                Vec3 yRotated = rotateY(shifted, yRotationForFacing(facing));
                yield rotateX(yRotated, 180.0F);
            }
        };

        return rotated.add(BLOCK_CENTER, BLOCK_CENTER, BLOCK_CENTER);
    }

    private static Vec3 cameraFixedOffsetBlockLocal(AttachFace attachFace, Direction facing) {
        return rotateVectorForMount(
                new Vec3(0.0D, CAMERA_FIXED_Y_PUSH, 0.0D),
                attachFace,
                facing
        );
    }

    private static Vec3 rotateVectorForMount(
            Vec3 vec,
            AttachFace attachFace,
            Direction facing
    ) {
        return switch (attachFace) {
            case FLOOR -> rotateY(vec, yRotationForFacing(facing));
            case WALL -> rotateY(vec, wallYRotationForFacing(facing));
            case CEILING -> {
                Vec3 yRotated = rotateY(vec, yRotationForFacing(facing));
                yield rotateX(yRotated, 180.0F);
            }
        };
    }

    private static Vec3 rotateY(Vec3 vec, float degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double x = vec.x * cos + vec.z * sin;
        double z = -vec.x * sin + vec.z * cos;

        return new Vec3(x, vec.y, z);
    }

    private static Vec3 rotateX(Vec3 vec, float degrees) {
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);

        double y = vec.y * cos - vec.z * sin;
        double z = vec.y * sin + vec.z * cos;

        return new Vec3(vec.x, y, z);
    }

    private static double hingeXForAttachFace(AttachFace attachFace) {
        return switch (attachFace) {
            case FLOOR, CEILING -> FLOOR_HEAD_HINGE_X;
            case WALL -> WALL_HEAD_HINGE_X;
        };
    }

    private static double hingeYForAttachFace(AttachFace attachFace) {
        return switch (attachFace) {
            case FLOOR, CEILING -> FLOOR_HEAD_HINGE_Y;
            case WALL -> WALL_HEAD_HINGE_Y;
        };
    }

    private static double hingeZForAttachFace(AttachFace attachFace) {
        return switch (attachFace) {
            case FLOOR, CEILING -> FLOOR_HEAD_HINGE_Z;
            case WALL -> WALL_HEAD_HINGE_Z;
        };
    }

    public static float yawForFacing(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            default -> 180.0F;
        };
    }

    private static float yRotationForFacing(Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    private static float wallYRotationForFacing(Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case SOUTH -> 180.0F;
            case EAST -> 270.0F;
            case WEST -> 90.0F;
            default -> 0.0F;
        };
    }
}
