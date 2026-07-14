package net.droingo.actioncamera.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class ActionCameraViewerMemory {
    private static final Map<MemoryKey, MemoryState> STATES = new HashMap<>();

    private ActionCameraViewerMemory() {}

    static Snapshot getSnapshot(Minecraft minecraft) {
        MemoryState state = getState(minecraft, false);
        if (state == null) return new Snapshot(null, null, 0, Set.of());
        return new Snapshot(copy(state.lastViewedCamera), copy(state.lastSelectedCamera),
                Math.max(0, state.scrollRows), Set.copyOf(state.expandedGroups));
    }

    static void rememberViewedCamera(Minecraft minecraft, BlockPos pos) {
        MemoryState state = getState(minecraft, true);
        if (state != null && pos != null) state.lastViewedCamera = pos.immutable();
    }

    static void rememberSelectedCamera(Minecraft minecraft, BlockPos pos) {
        MemoryState state = getState(minecraft, true);
        if (state != null && pos != null) state.lastSelectedCamera = pos.immutable();
    }

    static void rememberScrollRows(Minecraft minecraft, int rows) {
        MemoryState state = getState(minecraft, true);
        if (state != null) state.scrollRows = Math.max(0, rows);
    }

    static void rememberExpandedGroups(Minecraft minecraft, Set<String> groups) {
        MemoryState state = getState(minecraft, true);
        if (state != null) {
            state.expandedGroups.clear();
            if (groups != null) state.expandedGroups.addAll(groups);
        }
    }

    private static MemoryState getState(Minecraft minecraft, boolean create) {
        if (minecraft == null || minecraft.player == null || minecraft.level == null) return null;
        MemoryKey key = new MemoryKey(minecraft.player.getUUID(), minecraft.level.dimension().location());
        return create ? STATES.computeIfAbsent(key, ignored -> new MemoryState()) : STATES.get(key);
    }

    private static BlockPos copy(BlockPos pos) { return pos == null ? null : pos.immutable(); }

    record Snapshot(BlockPos lastViewedCamera, BlockPos lastSelectedCamera, int scrollRows,
                    Set<String> expandedGroups) {}

    private record MemoryKey(UUID playerId, ResourceLocation dimension) {}

    private static final class MemoryState {
        private BlockPos lastViewedCamera;
        private BlockPos lastSelectedCamera;
        private int scrollRows;
        private final Set<String> expandedGroups = new HashSet<>();
    }
}
