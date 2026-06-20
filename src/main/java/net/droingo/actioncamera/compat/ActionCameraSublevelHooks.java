package net.droingo.actioncamera.compat;

import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import net.droingo.actioncamera.client.ActionCameraPose;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Sable/Create Aeronautics compatibility hook.
 *
 * Sable stores sublevel blocks in far-away plot coordinates.
 * The visible moving contraption is drawn using the sublevel's render pose.
 *
 * So when an Action Camera block is inside a Sable plot, the camera pose must be:
 *
 * plot-space camera position/rotation
 *      -> Sable client render pose
 *      -> real rendered world-space camera position/rotation
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
        ClientSubLevelAccess subLevel = SableCompanion.INSTANCE.getContainingClient(cameraBlockPos);

        if (subLevel == null) {
            return basePose;
        }

        /*
         * Use renderPose(partialTick), not a logical/tick pose.
         *
         * renderPose is the client/interpolated pose used for drawing the sublevel
         * this frame, which is what we need for smooth gameplay and ReplayMod renders.
         */
        Pose3dc renderPose = subLevel.renderPose(partialTick);

        Vec3 transformedPosition = renderPose.transformPosition(basePose.position());

        RotationResult transformedRotation = transformRotation(
                renderPose,
                basePose.yaw(),
                basePose.pitch(),
                basePose.roll()
        );

        return new ActionCameraPose(
                transformedPosition,
                transformedRotation.yaw(),
                transformedRotation.pitch(),
                transformedRotation.roll(),
                basePose.fovOverride()
        );
    }

    private static RotationResult transformRotation(Pose3dc pose, float yaw, float pitch, float roll) {
        /*
         * This must match ActionCameraClientState.applyCameraFields(...):
         *
         * Minecraft camera yaw/pitch/roll -> Quaternion:
         * rotationYXZ(-yaw, pitch, roll)
         */
        Quaternionf localCameraRotation = new Quaternionf().rotationYXZ(
                (float) Math.toRadians(-yaw),
                (float) Math.toRadians(pitch),
                (float) Math.toRadians(roll)
        );

        Quaterniondc subLevelOrientationD = pose.orientation();

        Quaternionf subLevelRotation = new Quaternionf(
                (float) subLevelOrientationD.x(),
                (float) subLevelOrientationD.y(),
                (float) subLevelOrientationD.z(),
                (float) subLevelOrientationD.w()
        );

        /*
         * Sublevel rotation first, then the camera's local rotation inside that sublevel.
         */
        Quaternionf worldRotation = subLevelRotation.mul(localCameraRotation, new Quaternionf());

        /*
         * Convert back to the same Minecraft yaw/pitch/roll convention we use elsewhere.
         */
        Vector3f eulerYXZ = worldRotation.getEulerAnglesYXZ(new Vector3f());

        float transformedPitch = (float) Math.toDegrees(eulerYXZ.x());
        float transformedYaw = -(float) Math.toDegrees(eulerYXZ.y());
        float transformedRoll = (float) Math.toDegrees(eulerYXZ.z());

        return new RotationResult(
                Mth.wrapDegrees(transformedYaw),
                Mth.clamp(transformedPitch, -89.0F, 89.0F),
                Mth.wrapDegrees(transformedRoll)
        );
    }

    private record RotationResult(float yaw, float pitch, float roll) {
    }
}