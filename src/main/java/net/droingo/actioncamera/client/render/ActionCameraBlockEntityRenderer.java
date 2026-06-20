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
     * Rotation pivots are in block-local coordinates.
     *
     * 0.5, 0.5, 0.5 = center of the block.
     *
     * These are deliberately simple for the first pass.
     * Once we see the model in-game, we can move these so the camera head rotates
     * around the exact neck/joint point instead of the block centre.
     */
    private static final double FLOOR_PIVOT_X = 0.5D;
    private static final double FLOOR_PIVOT_Y = 0.45D;
    private static final double FLOOR_PIVOT_Z = 0.5D;

    private static final double WALL_PIVOT_X = 0.5D;
    private static final double WALL_PIVOT_Y = 0.5D;
    private static final double WALL_PIVOT_Z = 0.5D;

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

        BakedModel headModel = Minecraft.getInstance()
                .getModelManager()
                .getModel(attachFace == AttachFace.WALL
                        ? ActionCameraClientModels.WALL_HEAD
                        : ActionCameraClientModels.FLOOR_HEAD
                );

        double pivotX = attachFace == AttachFace.WALL ? WALL_PIVOT_X : FLOOR_PIVOT_X;
        double pivotY = attachFace == AttachFace.WALL ? WALL_PIVOT_Y : FLOOR_PIVOT_Y;
        double pivotZ = attachFace == AttachFace.WALL ? WALL_PIVOT_Z : FLOOR_PIVOT_Z;

        poseStack.pushPose();

        poseStack.translate(pivotX, pivotY, pivotZ);

        /*
         * Step 1:
         * Rotate the neutral north-facing model to match block placement.
         */
        poseStack.mulPose(Axis.YP.rotationDegrees(modelYRotationForFacing(facing)));

        /*
         * Step 2:
         * Rotate only the camera head from the block entity tuning data.
         *
         * These values are currently default 0.
         * The next commit will add GUI/networking so these can be edited in-game.
         */
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

    /*
     * Matches the blockstate convention:
     * neutral model faces north.
     */
    private static float modelYRotationForFacing(Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }
}