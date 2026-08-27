package me.alfie.immersiveenchanting.api.description.internal.lines;

import me.alfie.alfinolib.gui.GuiGraphicsX;
import me.alfie.alfinolib.gui.util.GuiGraphicsApi;
import me.alfie.alfinolib.gui.util.MousePos;
import me.alfie.immersiveenchanting.api.description.DescriptionHelper;
import me.alfie.immersiveenchanting.api.description.DescriptionLine;
import me.alfie.immersiveenchanting.gui.EnchantingTableScreen;
import me.alfie.immersiveenchanting.gui.core.Sprite;
import me.alfie.immersiveenchanting.gui.tab.enchanting.tooltip.NodeTooltip;
import me.alfie.immersiveenchanting.gui.tab.enchanting.tooltip.RenderedCost;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public record LevelsLine(NodeTooltip tooltip) implements DescriptionLine {

    /**
     * Renders the "XP Levels:" label, followed by an XP orb icon sprite, then the
     * required level count in green to the right of the icon.
     */
    @Override
    public void render(GuiGraphicsX gx, int lineX, int lineY, MousePos mousePos) {
        DescriptionHelper.text(gx, getText(), lineX, lineY + DescriptionHelper.ITEM_LINE_OFFSET);

        EnchantingTableScreen screen = tooltip.screen();
        RenderedCost renderedCost = screen.enchantmentCostRenderer().getCurrentRenderedCost();

        GuiGraphicsApi.blit(gx,
                Sprite.XP_LEVEL.id(),
                lineX + Minecraft.getInstance().font.width(getText().getString()),
                lineY + DescriptionHelper.ITEM_LINE_OFFSET/2,
                Sprite.XP_LEVEL.width(), Sprite.XP_LEVEL.height());

        DescriptionHelper.text(gx,
                Component.literal(String.valueOf(renderedCost.xpLevels())).withColor(0xC8FF8F),
                lineX + Minecraft.getInstance().font.width(getText().getString()) + 10,
                lineY + DescriptionHelper.ITEM_LINE_OFFSET/2 + 4);

    }

    @Override
    public @NotNull Component getText() {
        return Component.translatable("immersiveenchanting.tooltip.desc.xp_levels").withStyle(ChatFormatting.GRAY);
    }

    @Override
    public int getLineHeight() {
        return DescriptionHelper.ITEM_LINE_HEIGHT;
    }
}
