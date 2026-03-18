package me.alfie.immersiveenchanting.api.internal.cost;

import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.enchanting.EnchantingNodeTooltip;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.tooltip.DescriptionLine;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;

public record EnchantingFuelDescriptionLine(EnchantingNodeTooltip enchantingNodeTooltip) implements DescriptionLine {
    @Override
    public void draw(GuiGraphics graphics, int lineX, int lineY) {
        //Draw label
        graphics.drawString(Minecraft.getInstance().font,
                getText(),
                lineX,
                lineY + 4, //Offset to centre text with cost stack
                0xFFFFFF);

        ItemStack stackToRender = enchantingNodeTooltip.getCurrentRenderedFuel().asItemStack();

        //Draw cost stack or "Free" if no item cost defined.
        if (stackToRender.is(Items.AIR) || stackToRender.isEmpty()) {
            graphics.drawString(
                    Minecraft.getInstance().font,
                    Component.translatable("gui.immersiveenchanting.no_materials_required").withStyle(ChatFormatting.ITALIC),
                    lineX + Minecraft.getInstance().font.width(getText()),
                    lineY + 4,
                    ChatFormatting.GRAY.getColor()
            );
        } else {
            Vector2i fuelStackPos = new Vector2i(lineX + Minecraft.getInstance().font.width(getText()), lineY);
            enchantingNodeTooltip.setFuelStackPos(fuelStackPos);
            graphics.renderItem(
                    stackToRender,
                    fuelStackPos.x,
                    fuelStackPos.y);
            graphics.renderItemDecorations(Minecraft.getInstance().font,
                    stackToRender,
                    fuelStackPos.x,
                    fuelStackPos.y);
        }
    }

    @Override
    public @NotNull Component getText() {
        Component label;
        label = Component.translatable("gui.immersiveenchanting.enchanting_fuel").withStyle(ChatFormatting.GRAY);
        return label;
    }
}
