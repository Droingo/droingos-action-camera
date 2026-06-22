package net.droingo.actioncamera;

import net.droingo.actioncamera.network.ModNetworking;
import net.droingo.actioncamera.registry.ModBlockEntities;
import net.droingo.actioncamera.registry.ModBlocks;
import net.droingo.actioncamera.registry.ModCreativeTabs;
import net.droingo.actioncamera.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(DroingoActionCamera.MOD_ID)
public final class DroingoActionCamera {
    public static final String MOD_ID = "droingo_action_camera";

    public DroingoActionCamera(IEventBus modEventBus) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(ModNetworking::registerPayloads);
    }
}