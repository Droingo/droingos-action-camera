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
    /*
     * These pivots are the point we rotate the camera head around.
     *
     * For now, keep them simple and centered.
     * If we want ultra-precise joint rotation later, we can tune them further.
     */
    private static final double FLOOR_PIVOT_X = 0.5D;
    private static final double FLOOR_PIVOT_Y = 0.5D;
    private static final double FLOOR_PIVOT_Z = 0.5D;

    private static final double WALL_PIVOT_X = 0.5D;
    private static final double WALL_PIVOT_Y = 0.5D;
    private static final double WALL_PIVOT_Z = 0.5D;

    private static final double CEILING_PIVOT_X = 0.5D;
    private static final double CEILING_PIVOT_Y = 0.5D;
    private static final double CEILING_PIVOT_Z = 0.5D;

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

        double pivotX = getPivotX(attachFace);
        double pivotY = getPivotY(attachFace);
        double pivotZ = getPivotZ(attachFace);

        poseStack.pushPose();

        /*
         * Small placement-specific model corrections.
         *
         * Do NOT use the pivot for this.
         * The pivot affects all rotations and will break north/south if we use it
         * to fix east/west model alignment.
         */
        applyPlacementCorrection(poseStack, attachFace, facing);

        /*
         * Rotate around the chosen pivot.
         */
        poseStack.translate(pivotX, pivotY, pivotZ);

        /*
         * 1) Apply the mount/base transform so the head matches the placed stand.
         * 2) Apply user tuning yaw/pitch/roll on top of that.
         */
        applyBaseMountRotation(poseStack, attachFace, facing);

        poseStack.mulPose(Axis.YP.rotationDegrees(blockEntity.getYawOffset()));
        poseStack.mulPose(Axis.XP.rotationDegrees(blockEntity.getPitchOffset()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(blockEntity.getRollOffset()));

        poseStack.translate(-pivotX, -pivotY, -pivotZ);

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

    private static void applyPlacementCorrection(PoseStack poseStack, AttachFace attachFace, Direction facing) {
        if (attachFace != AttachFace.FLOOR) {
            return;
        }

        /*
         * 2 pixels = 2 / 16 = 0.125 blocks.
         *
         * East/west floor heads need to move 2 pixels backward relative to
         * their facing direction.
         *
         * NORTH/SOUTH are already correct, so leave them untouched.
         */
        double twoPixels = 2.0D / 16.0D;

        switch (facing) {
            case EAST -> poseStack.translate(twoPixels, 0.0D, 0.0D);
            case WEST -> poseStack.translate(-twoPixels, 0.0D, 0.0D);
            default -> {
                // North/south already line up. No correction.
            }
        }
    }

    private static BakedModel getHeadModel(AttachFace attachFace) {
        return Minecraft.getInstance()
                .getModelManager()
                .getModel(attachFace == AttachFace.WALL
                        ? ActionCameraClientModels.WALL_HEAD
                        : ActionCameraClientModels.FLOOR_HEAD
                );
    }

    private static double getPivotX(AttachFace attachFace) {
        return switch (attachFace) {
            case FLOOR -> FLOOR_PIVOT_X;
            case WALL -> WALL_PIVOT_X;
            case CEILING -> CEILING_PIVOT_X;
        };
    }

    private static double getPivotY(AttachFace attachFace) {
        return switch (attachFace) {
            case FLOOR -> FLOOR_PIVOT_Y;
            case WALL -> WALL_PIVOT_Y;
            case CEILING -> CEILING_PIVOT_Y;
        };
    }

    private static double getPivotZ(AttachFace attachFace) {
        return switch (attachFace) {
            case FLOOR -> FLOOR_PIVOT_Z;
            case WALL -> WALL_PIVOT_Z;
            case CEILING -> CEILING_PIVOT_Z;
        };
    }

    private static void applyBaseMountRotation(PoseStack poseStack, AttachFace attachFace, Direction facing) {
        switch (attachFace) {
            case FLOOR -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(yRotationForFacing(facing)));
            }
            case WALL -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(wallYRotationForFacing(facing)));
            }
            case CEILING -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(yRotationForFacing(facing)));
                poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
            }
        }
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
            /*
             * Wall head model correction.
             *
             * The wall head model is 180 degrees reversed compared to the wall stand,
             * so this map intentionally flips each horizontal direction.
             */
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