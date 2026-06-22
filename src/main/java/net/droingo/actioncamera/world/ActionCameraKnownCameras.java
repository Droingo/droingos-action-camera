package net.droingo.actioncamera.world;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ActionCameraKnownCameras {
    private static final Set<BlockPos> POSITIONS = ConcurrentHashMap.newKeySet();

    private ActionCameraKnownCameras() {
    }

    public static void register(BlockPos pos) {
        POSITIONS.add(pos.immutable());
    }

    public static void unregister(BlockPos pos) {
        POSITIONS.remove(pos);
    }

    public static List<BlockPos> snapshot() {
        return new ArrayList<>(POSITIONS);
    }

    public static void clear() {
        POSITIONS.clear();
    }
}