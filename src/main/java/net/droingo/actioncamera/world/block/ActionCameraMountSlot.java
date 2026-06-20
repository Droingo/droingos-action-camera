package net.droingo.actioncamera.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ActionCameraMountSlot {
    public static final int SLOT_TOP_LEFT = 0;
    public static final int SLOT_TOP_CENTER = 1;
    public static final int SLOT_TOP_RIGHT = 2;
    public static final int SLOT_CENTER_LEFT = 3;
    public static final int SLOT_CENTER = 4;
    public static final int SLOT_CENTER_RIGHT = 5;
    public static final int SLOT_BOTTOM_LEFT = 6;
    public static final int SLOT_BOTTOM_CENTER = 7;
    public static final int SLOT_BOTTOM_RIGHT = 8;

    /*
     * How far apart the 3x3 mount slots are from the centre.
     */
    private static final double SLOT_STEP = 5.0D / 16.0D;

    private static final double SHAPE_MIN = 5.0D / 16.0D;
    private static final double SHAPE_MAX = 11.0D / 16.0D;

    private ActionCameraMountSlot() {
    }

    public static int slotFromContext(BlockPlaceContext context) {
        return slotFromHit(context.getClickedFace(), context.getClickLocation());
    }

    public static int slotFromHit(Direction clickedFace, Vec3 hitLocation) {
        double localX = fraction(hitLocation.x);
        double localY = fraction(hitLocation.y);
        double localZ = fraction(hitLocation.z);

        double u;
        double v;

        /*
         * u = left/right across the clicked face.
         * v = top/bottom across the clicked face.
         *
         * For floor/ceiling, "top/bottom" becomes back/front across the surface.
         */
        switch (clickedFace) {
            case UP -> {
                u = localX;
                v = localZ;
            }
            case DOWN -> {
                u = localX;
                v = 1.0D - localZ;
            }
            case NORTH -> {
                u = localX;
                v = 1.0D - localY;
            }
            case SOUTH -> {
                u = 1.0D - localX;
                v = 1.0D - localY;
            }
            case WEST -> {
                u = 1.0D - localZ;
                v = 1.0D - localY;
            }
            case EAST -> {
                u = localZ;
                v = 1.0D - localY;
            }
            default -> {
                u = 0.5D;
                v = 0.5D;
            }
        }

        int column = snapThird(u);
        int row = snapThird(v);

        return row * 3 + column;
    }

    /**
     * The single source of truth for where a mount slot sits inside the placed
     * block position.
     *
     * This returns an offset in block/world axes, not model-local axes.
     *
     * Renderer, hitbox, and actual camera pose should all use this same value.
     */
    public static Vec3 worldAssemblyOffset(AttachFace attachFace, Direction facing, int slot) {
        Direction clickedFace = switch (attachFace) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> facing;
        };

        return previewWorldOffset(clickedFace, slot);
    }

    /**
     * Kept for older code paths/debugging, but new renderer/pose code should use
     * worldAssemblyOffset(...).
     */
    public static Vec3 localAssemblyOffset(AttachFace attachFace, int slot) {
        int safeSlot = Mth.clamp(slot, 0, 8);

        int column = safeSlot % 3;
        int row = safeSlot / 3;

        double x = (column - 1) * SLOT_STEP;

        return switch (attachFace) {
            case FLOOR -> new Vec3(x, 0.0D, (row - 1) * SLOT_STEP);
            case CEILING -> new Vec3(x, 0.0D, (1 - row) * SLOT_STEP);
            case WALL -> new Vec3(x, (1 - row) * SLOT_STEP, 0.0D);
        };
    }

    public static Vec3 previewWorldOffset(Direction clickedFace, int slot) {
        int safeSlot = Mth.clamp(slot, 0, 8);

        int column = safeSlot % 3;
        int row = safeSlot / 3;

        double horizontal = (column - 1) * SLOT_STEP;
        double vertical = (1 - row) * SLOT_STEP;
        double depth = (row - 1) * SLOT_STEP;

        return switch (clickedFace) {
            case UP -> new Vec3(horizontal, 0.0D, depth);
            case DOWN -> new Vec3(horizontal, 0.0D, -depth);

            case NORTH -> new Vec3(horizontal, vertical, 0.0D);
            case SOUTH -> new Vec3(-horizontal, vertical, 0.0D);

            case WEST -> new Vec3(0.0D, vertical, -horizontal);
            case EAST -> new Vec3(0.0D, vertical, horizontal);

            default -> Vec3.ZERO;
        };
    }

    public static AABB selectionBox(AttachFace attachFace, Direction facing, int slot) {
        Vec3 offset = worldAssemblyOffset(attachFace, facing, slot);

        AABB base = switch (attachFace) {
            case FLOOR -> new AABB(
                    SHAPE_MIN, 0.0D, SHAPE_MIN,
                    SHAPE_MAX, 10.0D / 16.0D, SHAPE_MAX
            );

            case CEILING -> new AABB(
                    SHAPE_MIN, 6.0D / 16.0D, SHAPE_MIN,
                    SHAPE_MAX, 1.0D, SHAPE_MAX
            );

            case WALL -> new AABB(
                    SHAPE_MIN, SHAPE_MIN, SHAPE_MIN,
                    SHAPE_MAX, SHAPE_MAX, SHAPE_MAX
            );
        };

        return base.move(offset);
    }

    public static AABB previewBox(Direction clickedFace, BlockPos placePos, int slot) {
        return previewBox(clickedFace, placePos.getX(), placePos.getY(), placePos.getZ(), slot);
    }

    public static AABB previewBox(Direction clickedFace, int blockX, int blockY, int blockZ, int slot) {
        Vec3 offset = previewWorldOffset(clickedFace, slot);

        double centerX = blockX + 0.5D + offset.x;
        double centerY = blockY + 0.5D + offset.y;
        double centerZ = blockZ + 0.5D + offset.z;

        double half = 3.5D / 16.0D;

        return switch (clickedFace) {
            case UP -> new AABB(
                    centerX - half, blockY + 0.02D, centerZ - half,
                    centerX + half, blockY + 0.62D, centerZ + half
            );

            case DOWN -> new AABB(
                    centerX - half, blockY + 0.38D, centerZ - half,
                    centerX + half, blockY + 0.98D, centerZ + half
            );

            default -> new AABB(
                    centerX - half, centerY - half, centerZ - half,
                    centerX + half, centerY + half, centerZ + half
            );
        };
    }

    public static String slotName(int slot) {
        return switch (Mth.clamp(slot, 0, 8)) {
            case SLOT_TOP_LEFT -> "top-left";
            case SLOT_TOP_CENTER -> "top-center";
            case SLOT_TOP_RIGHT -> "top-right";
            case SLOT_CENTER_LEFT -> "center-left";
            case SLOT_CENTER -> "center";
            case SLOT_CENTER_RIGHT -> "center-right";
            case SLOT_BOTTOM_LEFT -> "bottom-left";
            case SLOT_BOTTOM_CENTER -> "bottom-center";
            case SLOT_BOTTOM_RIGHT -> "bottom-right";
            default -> "center";
        };
    }

    private static double fraction(double value) {
        return value - Math.floor(value);
    }

    private static int snapThird(double value) {
        double clamped = Mth.clamp(value, 0.0D, 0.999999D);
        return Mth.clamp((int) Math.floor(clamped * 3.0D), 0, 2);
    }
}