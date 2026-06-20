package net.droingo.actioncamera.world.block;

import com.mojang.serialization.MapCodec;
import net.droingo.actioncamera.world.blockentity.ActionCameraBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class ActionCameraBlock extends BaseEntityBlock {
    public static final MapCodec<ActionCameraBlock> CODEC = simpleCodec(ActionCameraBlock::new);

    public static final EnumProperty<AttachFace> ATTACH_FACE = BlockStateProperties.ATTACH_FACE;
    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;

    /*
     * Selection shapes only.
     *
     * These are NOT collision boxes.
     * They are just the outline used for looking at / right-clicking / breaking the camera.
     *
     * Keep them simple. The detailed model does not need a detailed hitbox.
     */

    private static final VoxelShape FLOOR_SELECTION_SHAPE = box(
            5.0D, 0.0D, 5.0D,
            11.0D, 8.0D, 11.0D
    );

    private static final VoxelShape CEILING_SELECTION_SHAPE = box(
            5.0D, 8.0D, 5.0D,
            11.0D, 16.0D, 11.0D
    );

    /*
     * Wall selection boxes.
     *
     * Direction means the camera is facing that direction.
     * So a NORTH-facing wall camera is mounted to the south side of the block
     * and projects toward north.
     */

    private static final VoxelShape WALL_SELECTION_NORTH = box(
            5.0D, 4.0D, 8.0D,
            11.0D, 12.0D, 16.0D
    );

    private static final VoxelShape WALL_SELECTION_SOUTH = box(
            5.0D, 4.0D, 0.0D,
            11.0D, 12.0D, 8.0D
    );

    private static final VoxelShape WALL_SELECTION_EAST = box(
            0.0D, 4.0D, 5.0D,
            8.0D, 12.0D, 11.0D
    );

    private static final VoxelShape WALL_SELECTION_WEST = box(
            8.0D, 4.0D, 5.0D,
            16.0D, 12.0D, 11.0D
    );

    public ActionCameraBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(
                this.stateDefinition.any()
                        .setValue(ATTACH_FACE, AttachFace.FLOOR)
                        .setValue(HORIZONTAL_FACING, Direction.NORTH)
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ActionCameraBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Direction playerFacing = context.getHorizontalDirection();

        AttachFace attachFace;
        Direction horizontalFacing;

        if (clickedFace == Direction.UP) {
            attachFace = AttachFace.FLOOR;
            horizontalFacing = playerFacing;
        } else if (clickedFace == Direction.DOWN) {
            attachFace = AttachFace.CEILING;
            horizontalFacing = playerFacing;
        } else {
            attachFace = AttachFace.WALL;
            horizontalFacing = clickedFace;
        }

        return this.defaultBlockState()
                .setValue(ATTACH_FACE, attachFace)
                .setValue(HORIZONTAL_FACING, horizontalFacing);
    }

    /**
     * Selection outline / click target.
     * This is the only visible wireframe hitbox.
     */
    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return getSelectionShapeForState(state);
    }

    /**
     * Physical collision.
     *
     * Empty means:
     * - players can walk through it
     * - entities do not collide with it
     * - vehicles/Sable contraptions should not catch on it
     */
    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.empty();
    }

    /**
     * Occlusion/light shape.
     *
     * Empty because the model is tiny and should not block lighting like a cube.
     */
    @Override
    protected VoxelShape getOcclusionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos
    ) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ATTACH_FACE, HORIZONTAL_FACING);
    }

    @Override
    protected void onRemove(
            BlockState oldState,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (oldState.getBlock() != newState.getBlock()) {
            level.removeBlockEntity(pos);
        }

        super.onRemove(oldState, level, pos, newState, movedByPiston);
    }

    private static VoxelShape getSelectionShapeForState(BlockState state) {
        AttachFace attachFace = state.hasProperty(ATTACH_FACE)
                ? state.getValue(ATTACH_FACE)
                : AttachFace.FLOOR;

        Direction facing = state.hasProperty(HORIZONTAL_FACING)
                ? state.getValue(HORIZONTAL_FACING)
                : Direction.NORTH;

        return switch (attachFace) {
            case FLOOR -> FLOOR_SELECTION_SHAPE;
            case CEILING -> CEILING_SELECTION_SHAPE;
            case WALL -> getWallSelectionShape(facing);
        };
    }

    private static VoxelShape getWallSelectionShape(Direction facing) {
        return switch (facing) {
            case NORTH -> WALL_SELECTION_NORTH;
            case SOUTH -> WALL_SELECTION_SOUTH;
            case EAST -> WALL_SELECTION_EAST;
            case WEST -> WALL_SELECTION_WEST;
            default -> WALL_SELECTION_NORTH;
        };
    }
}