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

public final class ActionCameraBlockEntityRenderer implements BlockEntityRenderer<ActionCameraBlockEntity> {
    private static final double BLOCK_CENTER = 0.5D;

    private static final double FLOOR_HEAD_HINGE_X = 8.0D / 16.0D;
    private static final double FLOOR_HEAD_HINGE_Y = 2.0D / 16.0D;
    private static final double FLOOR_HEAD_HINGE_Z = 7.0D / 16.0D;

    private static final double WALL_HEAD_HINGE_X = 8.0D / 16.0D;
    private static final double WALL_HEAD_HINGE_Y = 7.25D / 16.0D;
    private static final double WALL_HEAD_HINGE_Z = 14.6D / 16.0D;

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
        if (ActionCameraClientState.isActive()
                && activeCameraPos != null
                && activeCameraPos.equals(blockEntity.getBlockPos())) {
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

        /*
         * Stage 1:
         * Move the whole assembly to the exact 3x3 placement slot.
         */
        applyWorldAssemblyOffset(poseStack, attachFace, facing, mountSlot, blockEntity);

        /*
         * Stage 2:
         * Rotate the placed model into its floor/wall/ceiling orientation.
         */
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

        poseStack.pushPose();

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
        poseStack.popPose();
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

        double hingeX = hingeXForAttachFace(attachFace);
        double hingeY = hingeYForAttachFace(attachFace);
        double hingeZ = hingeZForAttachFace(attachFace);

        poseStack.translate(hingeX, hingeY, hingeZ);

        poseStack.mulPose(Axis.YP.rotationDegrees(visualYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(visualPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(visualRoll));

        /*
         * Visual-only lens/back correction.
         *
         * Wall model still needs this correction. Floor/ceiling were visually
         * backwards with this correction after the mount-slot renderer rewrite,
         * so they now use 0 degrees here.
         */
        poseStack.mulPose(Axis.YP.rotationDegrees(lensBackCorrectionDegrees(attachFace)));

        poseStack.translate(-hingeX, -hingeY, -hingeZ);
    }

    private static float lensBackCorrectionDegrees(AttachFace attachFace) {
        return switch (attachFace) {
            case FLOOR -> 0.0F;
            case CEILING, WALL -> 180.0F;
        };
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