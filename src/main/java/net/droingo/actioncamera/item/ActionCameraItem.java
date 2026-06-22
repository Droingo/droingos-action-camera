package net.droingo.actioncamera.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public final class ActionCameraItem extends BlockItem {
    public ActionCameraItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        if (Screen.hasShiftDown()) {
            tooltipComponents.add(Component.translatable("tooltip.droingo_action_camera.action_camera.line_1")
                    .withStyle(ChatFormatting.GRAY));

            tooltipComponents.add(Component.translatable("tooltip.droingo_action_camera.action_camera.line_2")
                    .withStyle(ChatFormatting.GRAY));

            tooltipComponents.add(Component.translatable("tooltip.droingo_action_camera.action_camera.line_3")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.droingo_action_camera.hold_shift")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}