package net.droingo.actioncamera.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.droingo.actioncamera.client.ActionCameraClientState;
import net.droingo.actioncamera.world.block.ActionCameraBlock;
import net.droingo.actioncamera.world.block.ActionCameraMountSlot;
import net.droingo.actioncamera.world.blockentity.ActionCameraBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;

public final class ActionCameraBlockEntityRenderer implements BlockEntityRenderer<ActionCameraBlockEntity> {
    private static final double BLOCK_CENTER = 0.5D;

    private static final double FLOOR_HEAD_HINGE_X = 8.0D / 16.0D;
    private static final double FLOOR_HEAD_HINGE_Y = 2.0D / 16.0D;
    private static final double FLOOR_HEAD_HINGE_Z = 7.0D / 16.0D;

    /*
     * Ceiling pole anchor:
     * this is the visual connection point for the extension pole.
     */
    private static final double CEILING_POLE_ANCHOR_X = 8.0D / 16.0D;
    private static final double CEILING_POLE_ANCHOR_Y = 2.0D / 16.0D;
    private static final double CEILING_POLE_ANCHOR_Z = 9.0D / 16.0D;

    /*
     * Ceiling head pivot:
     * keep the actual head rotation pivot at the original model hinge.
     * This stops the head from rotating around a point too far back.
     */
    private static final double CEILING_HEAD_PIVOT_X = 8.0D / 16.0D;
    private static final double CEILING_HEAD_PIVOT_Y = 2.0D / 16.0D;
    private static final double CEILING_HEAD_PIVOT_Z = 7.0D / 16.0D;

    private static final double WALL_HEAD_HINGE_X = 8.0D / 16.0D;
    private static final double WALL_HEAD_HINGE_Y = 7.25D / 16.0D;
    private static final double WALL_HEAD_HINGE_Z = 14.6D / 16.0D;

    private static final double CEILING_POLE_HEAD_ATTACH_LOCAL_Y_OFFSET = -0.5D / 16.0D;
    private static final double CEILING_POLE_HEAD_ATTACH_LOCAL_Z_OFFSET = 0.0D;

    private static final double MIN_VISIBLE_EXTENSION_DISTANCE = 0.03D;

    public ActionCameraBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            ActionCameraBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockPos activeCameraPos = ActionCameraClientState.getActiveCameraPos();
        boolean isActiveCamera = ActionCameraClientState.isActive()
                && activeCameraPos != null
                && activeCameraPos.equals(blockEntity.getBlockPos());

        boolean hasVisibleExtension = hasVisibleExtension(blockEntity);

        if (isActiveCamera && !hasVisibleExtension) {
            return;
        }

        BlockState state = blockEntity.getBlockState();

        AttachFace attachFace = state.hasProperty(ActionCameraBlock.ATTACH_FACE)
                ? state.getValue(ActionCameraBlock.ATTACH_FACE)
                : AttachFace.FLOOR;

        Direction facing = state.hasProperty(ActionCameraBlock.HORIZONTAL_FACING)
                ? state.getValue(ActionCameraBlock.HORIZONTAL_FACING)
                : Direction.NORTH;

        int mountSlot = state.hasProperty(ActionCameraBlock.MOUNT_SLOT)
                ? state.getValue(ActionCameraBlock.MOUNT_SLOT)
                : ActionCameraMountSlot.SLOT_CENTER;

        BakedModel standModel = getStandModel(attachFace);
        BakedModel headModel = getHeadModel(attachFace);

        poseStack.pushPose();
        applyWorldAssemblyOffset(poseStack, attachFace, facing, mountSlot, blockEntity);
        applyBaseMountTransformAroundBlockCenter(poseStack, attachFace, facing);
        renderBakedModel(
                standModel,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                state,
                blockEntity.getBlockPos().asLong()
        );
        poseStack.popPose();

        if (!blockEntity.isExternalRigVisible()) {
            return;
        }

        if (hasVisibleExtension) {
            renderExtensionPole(
                    blockEntity,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    state,
                    attachFace,
                    facing,
                    mountSlot
            );
        }

        if (!isActiveCamera) {
            poseStack.pushPose();
            applyWorldAssemblyOffset(poseStack, attachFace, facing, mountSlot, blockEntity);
            poseStack.translate(
                    blockEntity.getExtensionX(),
                    blockEntity.getExtensionY(),
                    blockEntity.getExtensionZ()
            );
            applyBaseMountTransformAroundBlockCenter(poseStack, attachFace, facing);
            applyLiveHeadRotation(poseStack, attachFace, blockEntity);
            renderBakedModel(
                    headModel,
                    poseStack,
                    bufferSource,
                    packedLight,
                    packedOverlay,
                    state,
                    blockEntity.getBlockPos().asLong() + 1L
            );
            poseStack.popPose();
        }
    }

    private static boolean hasVisibleExtension(ActionCameraBlockEntity blockEntity) {
        if (!blockEntity.isExtensionEnabled()) {
            return false;
        }

        return blockEntity.getExtensionOffset().length() >= MIN_VISIBLE_EXTENSION_DISTANCE;
    }

    private static BakedModel getStandModel(AttachFace attachFace) {
        return Minecraft.getInstance()
                .getModelManager()
                .getModel(attachFace == AttachFace.WALL
                        ? ActionCameraClientModels.WALL_STAND
                        : ActionCameraClientModels.FLOOR_STAND
                );
    }

    private static BakedModel getHeadModel(AttachFace attachFace) {
        return Minecraft.getInstance()
                .getModelManager()
                .getModel(attachFace == AttachFace.WALL
                        ? ActionCameraClientModels.WALL_HEAD
                        : ActionCameraClientModels.FLOOR_HEAD
                );
    }

    private static BakedModel getPoleModel() {
        return Minecraft.getInstance()
                .getModelManager()
                .getModel(ActionCameraClientModels.EXTENSION_POLE);
    }

    private static void applyWorldAssemblyOffset(
            PoseStack poseStack,
            AttachFace attachFace,
            Direction facing,
            int mountSlot,
            ActionCameraBlockEntity blockEntity
    ) {
        Vec3 slotOffset = ActionCameraMountSlot.worldAssemblyOffset(attachFace, facing, mountSlot);

        poseStack.translate(
                slotOffset.x + blockEntity.getOffsetX(),
                slotOffset.y + blockEntity.getOffsetY(),
                slotOffset.z + blockEntity.getOffsetZ()
        );
    }

    private static void renderExtensionPole(
            ActionCameraBlockEntity blockEntity,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            BlockState state,
            AttachFace attachFace,
            Direction facing,
            int mountSlot
    ) {
        Vec3 start = getPoleAnchorBlockLocal(
                blockEntity,
                attachFace,
                facing,
                mountSlot
        );

        Vec3 end = start
                .add(blockEntity.getExtensionOffset())
                .add(poleHeadAttachmentOffset(attachFace, facing));

        Vec3 direction = end.subtract(start);
        double length = direction.length();

        if (length < MIN_VISIBLE_EXTENSION_DISTANCE) {
            return;
        }

        Vec3 normalizedDirection = direction.normalize();

        Quaternionf rotation = new Quaternionf().rotationTo(
                0.0F,
                0.0F,
                1.0F,
                (float) normalizedDirection.x,
                (float) normalizedDirection.y,
                (float) normalizedDirection.z
        );

        poseStack.pushPose();

        poseStack.translate(start.x, start.y, start.z);
        poseStack.mulPose(rotation);
        poseStack.scale(1.0F, 1.0F, (float) length);
        poseStack.translate(-0.5D, -0.5D, 0.0D);

        renderBakedModel(
                getPoleModel(),
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                state,
                blockEntity.getBlockPos().asLong() + 2L
        );

        poseStack.popPose();
    }

    private static Vec3 getPoleAnchorBlockLocal(
            ActionCameraBlockEntity blockEntity,
            AttachFace attachFace,
            Direction facing,
            int mountSlot
    ) {
        Vec3 slotOffset = ActionCameraMountSlot.worldAssemblyOffset(attachFace, facing, mountSlot);

        Vec3 anchorBeforeMountRotation = new Vec3(
                poleAnchorXForAttachFace(attachFace),
                poleAnchorYForAttachFace(attachFace),
                poleAnchorZForAttachFace(attachFace)
        );

        Vec3 anchorAfterMountRotation = rotatePointAroundBlockCenter(
                anchorBeforeMountRotation,
                attachFace,
                facing
        );

        return new Vec3(
                slotOffset.x + blockEntity.getOffsetX() + anchorAfterMountRotation.x,
                slotOffset.y + blockEntity.getOffsetY() + anchorAfterMountRotation.y,
                slotOffset.z + blockEntity.getOffsetZ() + anchorAfterMountRotation.z
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

    private static Vec3 poleHeadAttachmentOffset(AttachFace attachFace, Direction facing) {
        if (attachFace != AttachFace.CEILING) {
            return Vec3.ZERO;
        }

        return rotateVectorForMount(
                new Vec3(
                        0.0D,
                        CEILING_POLE_HEAD_ATTACH_LOCAL_Y_OFFSET,
                        CEILING_POLE_HEAD_ATTACH_LOCAL_Z_OFFSET
                ),
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

    private static void applyBaseMountTransformAroundBlockCenter(
            PoseStack poseStack,
            AttachFace attachFace,
            Direction facing
    ) {
        poseStack.translate(BLOCK_CENTER, BLOCK_CENTER, BLOCK_CENTER);

        switch (attachFace) {
            case FLOOR -> poseStack.mulPose(Axis.YP.rotationDegrees(yRotationForFacing(facing)));
            case WALL -> poseStack.mulPose(Axis.YP.rotationDegrees(wallYRotationForFacing(facing)));
            case CEILING -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(yRotationForFacing(facing)));
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            }
        }

        poseStack.translate(-BLOCK_CENTER, -BLOCK_CENTER, -BLOCK_CENTER);
    }

    private static void applyLiveHeadRotation(
            PoseStack poseStack,
            AttachFace attachFace,
            ActionCameraBlockEntity blockEntity
    ) {
        float visualYaw = visualYawForAttachFace(blockEntity.getYawOffset());
        float visualPitch = visualPitchForAttachFace(attachFace, blockEntity.getPitchOffset());
        float visualRoll = blockEntity.getRollOffset();

        double hingeX = headPivotXForAttachFace(attachFace);
        double hingeY = headPivotYForAttachFace(attachFace);
        double hingeZ = headPivotZForAttachFace(attachFace);

        poseStack.translate(hingeX, hingeY, hingeZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(visualYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(visualPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(visualRoll));
        poseStack.mulPose(Axis.YP.rotationDegrees(lensBackCorrectionDegrees(attachFace)));
        poseStack.translate(-hingeX, -hingeY, -hingeZ);
    }

    private static float lensBackCorrectionDegrees(AttachFace attachFace) {
        return switch (attachFace) {
            case FLOOR -> 0.0F;
            case CEILING, WALL -> 180.0F;
        };
    }

    private static double poleAnchorXForAttachFace(AttachFace attachFace) {
        return switch (attachFace) {
            case FLOOR -> FLOOR_HEAD_HINGE_X;
            case CEILING -> CEILING_POLE_ANCHOR_X;
            case WALL -> WALL_HEAD_HINGE_X;
        };
    }

    private static double poleAnchorYForAttachFace(AttachFace attachFace) {
        return switch (attachFace) {
            case FLOOR -> FLOOR_HEAD_HINGE_Y;
            case CEILING -> CEILING_POLE_ANCHOR_Y;
            case WALL -> WALL_HEAD_HINGE_Y;
        };
    }

    private static double poleAnchorZForAttachFace(AttachFace attachFace) {
        return switch (attachFace) {
            case FLOOR -> FLOOR_HEAD_HINGE_Z;
            case CEILING -> CEILING_POLE_ANCHOR_Z;
            case WALL -> WALL_HEAD_HINGE_Z;
        };
    }

    private static double headPivotXForAttachFace(AttachFace attachFace) {
        return switch (attachFace) {
            case FLOOR -> FLOOR_HEAD_HINGE_X;
            case CEILING -> CEILING_HEAD_PIVOT_X;
            case WALL -> WALL_HEAD_HINGE_X;
        };
    }

    private static double headPivotYForAttachFace(AttachFace attachFace) {
        return switch (attachFace) {
            case FLOOR -> FLOOR_HEAD_HINGE_Y;
            case CEILING -> CEILING_HEAD_PIVOT_Y;
            case WALL -> WALL_HEAD_HINGE_Y;
        };
    }

    private static double headPivotZForAttachFace(AttachFace attachFace) {
        return switch (attachFace) {
            case FLOOR -> FLOOR_HEAD_HINGE_Z;
            case CEILING -> CEILING_HEAD_PIVOT_Z;
            case WALL -> WALL_HEAD_HINGE_Z;
        };
    }
    private static float visualYawForAttachFace(float savedYaw) {
        return -savedYaw;
    }

    private static float visualPitchForAttachFace(AttachFace attachFace, float savedPitch) {
        return switch (attachFace) {
            case FLOOR, WALL -> -savedPitch;
            case CEILING -> savedPitch;
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

    private static void renderBakedModel(
            BakedModel model,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            BlockState state,
            long seed
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        ModelBlockRenderer modelRenderer = minecraft.getBlockRenderer().getModelRenderer();
        RandomSource random = RandomSource.create();

        for (RenderType renderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            random.setSeed(seed);
            VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

            modelRenderer.renderModel(
                    poseStack.last(),
                    vertexConsumer,
                    state,
                    model,
                    1.0F,
                    1.0F,
                    1.0F,
                    packedLight,
                    packedOverlay,
                    ModelData.EMPTY,
                    renderType
            );
        }
    }
}