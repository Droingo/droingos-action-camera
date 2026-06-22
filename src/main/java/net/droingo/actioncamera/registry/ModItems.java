package net.droingo.actioncamera.registry;

import net.droingo.actioncamera.DroingoActionCamera;
import net.droingo.actioncamera.item.ActionCameraItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, DroingoActionCamera.MOD_ID);

    public static final Supplier<ActionCameraItem> ACTION_CAMERA = ITEMS.register(
            "action_camera",
            () -> new ActionCameraItem(
                    ModBlocks.ACTION_CAMERA.get(),
                    new Item.Properties()
            )
    );

    private ModItems() {
    }
}