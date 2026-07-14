package net.droingo.actioncamera.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side camera viewer memory.
 *
 * State is separated by player UUID and dimension so that:
 * - different players do not share camera choices
 * - an Overworld BlockPos is not accidentally reused in another dimension
 */
final class ActionCameraViewerMemory {
    private static final Map<MemoryKey, MemoryState> STATES = new HashMap<>();

    private ActionCameraViewerMemory() {
    }

    static Snapshot getSnapshot(Minecraft minecraft) {
        MemoryKey key = createKey(minecraft);

        if (key == null) {
            return new Snapshot(null, null, 0);
        }

        MemoryState state = STATES.get(key);

        if (state == null) {
            return new Snapshot(null, null, 0);
        }

        return new Snapshot(
                immutableOrNull(state.lastViewedCamera),
                immutableOrNull(state.lastSelectedCamera),
                Math.max(0, state.scrollRows)
        );
    }

    static void rememberViewedCamera(
            Minecraft minecraft,
            BlockPos position
    ) {
        MemoryState state = getOrCreateState(minecraft);

        if (state == null || position == null) {
            return;
        }

        state.lastViewedCamera = position.immutable();
    }

    static void rememberSelectedCamera(
            Minecraft minecraft,
            BlockPos position
    ) {
        MemoryState state = getOrCreateState(minecraft);

        if (state == null || position == null) {
            return;
        }

        state.lastSelectedCamera = position.immutable();
    }

    static void rememberScrollRows(
            Minecraft minecraft,
            int scrollRows
    ) {
        MemoryState state = getOrCreateState(minecraft);

        if (state == null) {
            return;
        }

        state.scrollRows = Math.max(0, scrollRows);
    }

    private static MemoryState getOrCreateState(Minecraft minecraft) {
        MemoryKey key = createKey(minecraft);

        if (key == null) {
            return null;
        }

        return STATES.computeIfAbsent(
                key,
                ignored -> new MemoryState()
        );
    }

    private static MemoryKey createKey(Minecraft minecraft) {
        if (
                minecraft == null
                        || minecraft.player == null
                        || minecraft.level == null
        ) {
            return null;
        }

        UUID playerId = minecraft.player.getUUID();

        ResourceLocation dimension =
                minecraft.level.dimension().location();

        return new MemoryKey(
                playerId,
                dimension
        );
    }

    private static BlockPos immutableOrNull(BlockPos position) {
        return position == null
                ? null
                : position.immutable();
    }

    record Snapshot(
            BlockPos lastViewedCamera,
            BlockPos lastSelectedCamera,
            int scrollRows
    ) {
    }

    private record MemoryKey(
            UUID playerId,
            ResourceLocation dimension
    ) {
    }

    private static final class MemoryState {
        private BlockPos lastViewedCamera;
        private BlockPos lastSelectedCamera;
        private int scrollRows;
    }
}