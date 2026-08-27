package me.alfie.immersiveenchanting.api.description.internal.lines;

import me.alfie.alfinolib.gui.GuiGraphicsX;
import me.alfie.alfinolib.gui.util.GuiGraphicsApi;
import me.alfie.alfinolib.gui.util.MousePos;
import me.alfie.immersiveenchanting.api.description.DescriptionHelper;
import me.alfie.immersiveenchanting.api.description.DescriptionLine;
import me.alfie.immersiveenchanting.gui.EnchantingTableScreen;
import me.alfie.immersiveenchanting.gui.tab.enchanting.tooltip.NodeTooltip;
import me.alfie.immersiveenchanting.gui.tab.enchanting.tooltip.RenderedCost;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public record MaterialsLine(NodeTooltip tooltip) implements DescriptionLine {

    /**
     * Renders the "Materials:" label and then draws the required material item stack
     * immediately to the right of the label text.
     */
    @Override
    public void render(GuiGraphicsX gx, int lineX, int lineY, MousePos mousePos) {
        DescriptionHelper.text(gx, getText(), lineX, lineY + DescriptionHelper.ITEM_LINE_OFFSET);

        EnchantingTableScreen screen = tooltip.screen();
        RenderedCost renderedCost = screen.enchantmentCostRenderer().getCurrentRenderedCost();

        GuiGraphicsApi.itemStackWithTooltip(gx, renderedCost.stack(),
                screen.getFont(),
                lineX + screen.getFont().width(getText().getString()),
                lineY + DescriptionHelper.ITEM_LINE_OFFSET/2, mousePos);
    }

    @Override
    public @NotNull Component getText() {
        return Component.translatable("immersiveenchanting.tooltip.desc.materials").withStyle(ChatFormatting.GRAY);
    }

    @Override
    public int getLineHeight() {
        return DescriptionHelper.ITEM_LINE_HEIGHT;
    }
}
