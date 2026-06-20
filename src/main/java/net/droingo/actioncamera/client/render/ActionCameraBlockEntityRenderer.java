package net.droingo.actioncamera.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.droingo.actioncamera.world.block.ActionCameraBlock;
import net.droingo.actioncamera.world.blockentity.ActionCameraBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.client.model.data.ModelData;

public final class ActionCameraBlockEntityRenderer implements BlockEntityRenderer<ActionCameraBlockEntity> {
    private static final double BLOCK_CENTER = 0.5D;

    /*
     * Floor / ceiling camera head hinge.
     *
     * This is the physical mount joint position, not the old Blockbench Cam origin.
     *
     * Pixel-space hinge:
     * X = 8
     * Y = 2
     * Z = 7
     *
     * Converted to block units by dividing by 16.
     */
    private static final double FLOOR_HEAD_HINGE_X = 8.0D / 16.0D;
    private static final double FLOOR_HEAD_HINGE_Y = 2.0D / 16.0D;
    private static final double FLOOR_HEAD_HINGE_Z = 7.0D / 16.0D;

    /*
     * Wall camera head hinge.
     *
     * From Blockbench Wall/Cam origin:
     * [8, 7.25, 14.6]
     *
     * Converted from pixels to block units.
     */
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
        BlockState state = blockEntity.getBlockState();

        AttachFace attachFace = state.hasProperty(ActionCameraBlock.ATTACH_FACE)
                ? state.getValue(ActionCameraBlock.ATTACH_FACE)
                : AttachFace.FLOOR;

        Direction facing = state.hasProperty(ActionCameraBlock.HORIZONTAL_FACING)
                ? state.getValue(ActionCameraBlock.HORIZONTAL_FACING)
                : Direction.NORTH;

        BakedModel headModel = getHeadModel(attachFace);

        poseStack.pushPose();

        /*
         * Stage 1:
         * Match the placed stand/block orientation.
         *
         * This rotates around the block centre, matching how the baked stand model
         * is rotated by the blockstate JSON.
         */
        applyBaseMountTransformAroundBlockCenter(poseStack, attachFace, facing);

        /*
         * Stage 2:
         * Rotate the dynamic head around the physical hinge.
         */
        applyLiveHeadRotation(poseStack, attachFace, blockEntity);

        renderBakedModel(
                headModel,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay,
                state,
                blockEntity.getBlockPos().asLong()
        );

        poseStack.popPose();
    }

    private static BakedModel getHeadModel(AttachFace attachFace) {
        return Minecraft.getInstance()
                .getModelManager()
                .getModel(attachFace == AttachFace.WALL
                        ? ActionCameraClientModels.WALL_HEAD
                        : ActionCameraClientModels.FLOOR_HEAD
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
        float visualYaw = visualYawForAttachFace(attachFace, blockEntity.getYawOffset());
        float visualPitch = visualPitchForAttachFace(attachFace, blockEntity.getPitchOffset());
        float visualRoll = blockEntity.getRollOffset();

        double hingeX = hingeXForAttachFace(attachFace);
        double hingeY = hingeYForAttachFace(attachFace);
        double hingeZ = hingeZForAttachFace(attachFace);

        poseStack.translate(hingeX, hingeY, hingeZ);

        /*
         * Visual-only camera head rotation.
         *
         * The real camera view uses the saved yaw/pitch directly elsewhere.
         * This renderer corrects the Blockbench model so the physical head points
         * the same way the actual view is pointing.
         */
        poseStack.mulPose(Axis.YP.rotationDegrees(visualYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(visualPitch));
        poseStack.mulPose(Axis.ZP.rotationDegrees(visualRoll));

        /*
         * Visual-only lens/back correction.
         *
         * The exported model's lens/back direction is reversed compared to the
         * actual camera forward direction. This flip only affects the visible model.
         */
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));

        poseStack.translate(-hingeX, -hingeY, -hingeZ);
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

    private static float visualYawForAttachFace(AttachFace attachFace, float savedYaw) {
        /*
         * Real camera view uses saved yaw directly.
         * The physical model visually turns opposite, so invert only the rendered yaw.
         */
        return -savedYaw;
    }

    private static float visualPitchForAttachFace(AttachFace attachFace, float savedPitch) {
        /*
         * Floor/wall visual pitch is reversed.
         * Ceiling was already correct because the base mount transform is upside-down.
         */
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