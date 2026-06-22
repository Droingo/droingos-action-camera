package net.droingo.actioncamera.registry;

import net.droingo.actioncamera.DroingoActionCamera;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DroingoActionCamera.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DROINGO_ACTION_CAMERA_TAB =
            CREATIVE_MODE_TABS.register("droingo_action_camera", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.droingo_action_camera.droingo_action_camera"))
                    .icon(() -> new ItemStack(ModItems.ACTION_CAMERA.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.ACTION_CAMERA.get());
                    })
                    .build()
            );

    private ModCreativeTabs() {
    }
}