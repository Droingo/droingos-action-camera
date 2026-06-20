package net.droingo.actioncamera.registry;

import net.droingo.actioncamera.DroingoActionCamera;
import net.droingo.actioncamera.world.blockentity.ActionCameraBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DroingoActionCamera.MOD_ID);

    public static final Supplier<BlockEntityType<ActionCameraBlockEntity>> ACTION_CAMERA =
            BLOCK_ENTITY_TYPES.register(
                    "action_camera",
                    () -> BlockEntityType.Builder.of(
                            ActionCameraBlockEntity::new,
                            ModBlocks.ACTION_CAMERA.get()
                    ).build(null)
            );

    private ModBlockEntities() {
    }
}