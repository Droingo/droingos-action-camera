package net.droingo.actioncamera.world.block;

import com.mojang.serialization.MapCodec;
import net.droingo.actioncamera.world.blockentity.ActionCameraBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class ActionCameraBlock extends BaseEntityBlock {
    public static final MapCodec<ActionCameraBlock> CODEC = simpleCodec(ActionCameraBlock::new);

    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<AttachFace> ATTACH_FACE = BlockStateProperties.ATTACH_FACE;

    /*
     * 0 1 2
     * 3 4 5
     * 6 7 8
     */
    public static final IntegerProperty MOUNT_SLOT = IntegerProperty.create("mount_slot", 0, 8);

    public ActionCameraBlock(BlockBehaviour.Properties properties) {
        super(properties);

        registerDefaultState(
                stateDefinition.any()
                        .setValue(HORIZONTAL_FACING, Direction.NORTH)
                        .setValue(ATTACH_FACE, AttachFace.FLOOR)
                        .setValue(MOUNT_SLOT, ActionCameraMountSlot.SLOT_CENTER)
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
    protected RenderShape getRenderShape(BlockState state) {
        /*
         * The whole visual camera assembly is rendered by the block entity renderer.
         */
        return RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();

        AttachFace attachFace;
        Direction horizontalFacing;

        if (clickedFace == Direction.UP) {
            attachFace = AttachFace.FLOOR;
            horizontalFacing = context.getHorizontalDirection();
        } else if (clickedFace == Direction.DOWN) {
            attachFace = AttachFace.CEILING;
            horizontalFacing = context.getHorizontalDirection();
        } else {
            attachFace = AttachFace.WALL;
            horizontalFacing = clickedFace;
        }

        int mountSlot = ActionCameraMountSlot.slotFromContext(context);

        return defaultBlockState()
                .setValue(ATTACH_FACE, attachFace)
                .setValue(HORIZONTAL_FACING, horizontalFacing)
                .setValue(MOUNT_SLOT, mountSlot);
    }

    /*
     * Single-punch pickup.
     *
     * Left-clicking/punching the Action Camera removes it instantly and puts the
     * item back into the player's inventory. If the inventory is full, it drops.
     *
     * Creative mode removes it without duplicating an item.
     */
    @Override
    public void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return;
        }

        if (player.isSpectator()) {
            return;
        }

        pickUpCamera(level, pos, state, player);
    }

    private void pickUpCamera(Level level, BlockPos pos, BlockState state, Player player) {
        boolean removed = level.removeBlock(pos, false);
        if (!removed) {
            return;
        }

        // Vanilla block-break particles/sound event.
        level.levelEvent(2001, pos, Block.getId(state));

        if (player.isCreative()) {
            return;
        }

        ItemStack stack = new ItemStack(state.getBlock());

        boolean addedToInventory = player.getInventory().add(stack);
        if (!addedToInventory || !stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction supportDirection = getSupportDirection(state);
        BlockPos supportPos = pos.relative(supportDirection);
        Direction supportFace = supportDirection.getOpposite();
        BlockState supportState = level.getBlockState(supportPos);

        return canAttachToSupport(level, supportPos, supportState, supportFace);
    }

    private static boolean canAttachToSupport(
            LevelReader level,
            BlockPos supportPos,
            BlockState supportState,
            Direction supportFace
    ) {
        /*
         * Normal full-block behaviour.
         */
        if (supportState.isFaceSturdy(level, supportPos, supportFace)) {
            return true;
        }

        /*
         * Extra support list for partial blocks.
         *
         * These blocks often fail isFaceSturdy(...) because their support face is
         * not a full cube, but visually they are perfectly reasonable things to
         * mount a tiny Action Camera onto.
         */
        Block supportBlock = supportState.getBlock();

        if (supportBlock instanceof FenceBlock
                || supportBlock instanceof FenceGateBlock
                || supportBlock instanceof WallBlock
                || supportBlock instanceof SlabBlock
                || supportBlock instanceof StairBlock) {
            return hasAnyUsableShape(level, supportPos, supportState);
        }

        return false;
    }

    private static boolean hasAnyUsableShape(LevelReader level, BlockPos supportPos, BlockState supportState) {
        if (!(level instanceof BlockGetter blockGetter)) {
            return true;
        }

        VoxelShape collisionShape = supportState.getCollisionShape(blockGetter, supportPos);
        if (!collisionShape.isEmpty()) {
            return true;
        }

        VoxelShape visualShape = supportState.getShape(blockGetter, supportPos);
        return !visualShape.isEmpty();
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos currentPos,
            BlockPos neighborPos
    ) {
        if (direction == getSupportDirection(state) && !canSurvive(state, level, currentPos)) {
            return Blocks.AIR.defaultBlockState();
        }

        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    private static Direction getSupportDirection(BlockState state) {
        AttachFace attachFace = state.getValue(ATTACH_FACE);

        return switch (attachFace) {
            case FLOOR -> Direction.DOWN;
            case CEILING -> Direction.UP;
            case WALL -> state.getValue(HORIZONTAL_FACING).getOpposite();
        };
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        AABB box = selectionBoxFromState(state);

        return Shapes.box(
                box.minX,
                box.minY,
                box.minZ,
                box.maxX,
                box.maxY,
                box.maxZ
        );
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        AABB box = selectionBoxFromState(state);

        return Shapes.box(
                box.minX,
                box.minY,
                box.minZ,
                box.maxX,
                box.maxY,
                box.maxZ
        );
    }

    private static AABB selectionBoxFromState(BlockState state) {
        AttachFace attachFace = state.getValue(ATTACH_FACE);
        Direction facing = state.getValue(HORIZONTAL_FACING);
        int mountSlot = state.getValue(MOUNT_SLOT);

        return ActionCameraMountSlot.selectionBox(attachFace, facing, mountSlot);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(
                HORIZONTAL_FACING,
                rotation.rotate(state.getValue(HORIZONTAL_FACING))
        );
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(HORIZONTAL_FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING, ATTACH_FACE, MOUNT_SLOT);
    }
}