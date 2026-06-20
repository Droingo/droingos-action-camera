package net.droingo.actioncamera.registry;

import net.droingo.actioncamera.DroingoActionCamera;
import net.droingo.actioncamera.world.block.ActionCameraBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, DroingoActionCamera.MOD_ID);

    public static final Supplier<Block> ACTION_CAMERA = BLOCKS.register(
            "action_camera",
            () -> new ActionCameraBlock(
                    BlockBehaviour.Properties.of()
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            )
    );

    private ModBlocks() {
    }
}