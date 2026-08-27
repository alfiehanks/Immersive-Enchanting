package me.alfie.immersiveenchanting.api.description;

import me.alfie.alfinolib.gui.GuiGraphicsX;
import me.alfie.alfinolib.gui.util.GuiGraphicsApi;
import me.alfie.alfinolib.gui.util.MousePos;
import me.alfie.immersiveenchanting.api.description.internal.lines.FuelsLine;
import me.alfie.immersiveenchanting.api.description.internal.lines.LevelsLine;
import me.alfie.immersiveenchanting.api.description.internal.lines.MaterialsLine;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.CostRegistry;
import me.alfie.immersiveenchanting.gui.tab.enchanting.tooltip.NodeTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DescriptionHelper {

    public static final int DEFAULT_LINE_WIDTH = 32;
    public static final int DEFAULT_LINE_HEIGHT = Minecraft.getInstance().font.lineHeight;

    /**Offset for item/sprite rendering that are 16x16 and require extra space (i.e, Materials/Fuel/XP); use with ITEM_LINE_HEIGHT*/
    public static final int ITEM_LINE_OFFSET = 5;

    /**Line height for lines using items/sprites that are 16x16 and require extra space (i.e, Materials/Fuel/XP)*/
    public static final int ITEM_LINE_HEIGHT = DEFAULT_LINE_HEIGHT + ITEM_LINE_OFFSET*2;


    public static void text(GuiGraphicsX gx, Component component, int x, int y) {
        GuiGraphicsApi.text(gx, Minecraft.getInstance().font, component, x, y, true);
    }

    /**
     * Inserts cost lines (materials, fuel, XP levels) into {@code description} starting at
     * {@code lineStart}, skipping any cost component that is empty (AIR stack / 0 levels).
     * Each item-stack cost occupies two slots to leave room for the rendered item icon.
     */
    public static void insertCostLines(NodeTooltip tooltip, DescriptionLayout description, int lineStart){
        int lineNumber = lineStart;

        //Error message if enchanting cost data is missing from the registry
        if(!CostRegistry.client().isRegistered(tooltip.node().branchId())) {
            lineWrapComponent(Component.translatable("immersiveenchanting.tooltip.desc.cost_load_error",
                            tooltip.node().branchId().toString().replace(":", "/"))
                    .withStyle(ChatFormatting.RED), DEFAULT_LINE_WIDTH, description, lineNumber);
            return;
        }

        //Error message if enchanting_fuels is missing from the registry
        if(!CostRegistry.client().isRegistered(CostRegistry.ENCHANTING_FUELS)) {
            lineWrapComponent(Component.translatable("immersiveenchanting.tooltip.desc.fuels_load_error")
                    .withStyle(ChatFormatting.RED), DEFAULT_LINE_WIDTH, description, lineNumber);
            return;
        }

        //Error message if enchanting cost data is missing from the registry
        if(tooltip.screen().enchantmentCostRenderer().getCurrentRenderedCost() == null) {
            lineWrapComponent(Component.translatable("immersiveenchanting.tooltip.desc.no_cost_data",
                            tooltip.node().branchId().toString().replace(":", "/"))
                    .withStyle(ChatFormatting.RED), DEFAULT_LINE_WIDTH, description, lineNumber);
            return;
        }

        //Error message if enchanting fuel data is missing for enchantment level
        if(tooltip.screen().enchantmentCostRenderer().getCurrentRenderedFuel() == null) {
            lineWrapComponent(Component.translatable("immersiveenchanting.tooltip.desc.no_fuel_data")
                    .withStyle(ChatFormatting.RED), DEFAULT_LINE_WIDTH, description, lineNumber);
            return;
        }



        if(!tooltip.screen().enchantmentCostRenderer().getCurrentRenderedCost().stack().is(Items.AIR)) {
            description.insertLine(lineNumber, new MaterialsLine(tooltip));
            lineNumber += 1;
        }

        if(!tooltip.screen().enchantmentCostRenderer().getCurrentRenderedFuel().stack().is(Items.AIR)) {
            description.insertLine(lineNumber, new FuelsLine(tooltip));
            lineNumber += 1;
        }

        if(tooltip.screen().enchantmentCostRenderer().getCurrentRenderedCost().xpLevels() > 0) description.insertLine(lineNumber, new LevelsLine(tooltip));
    }

    /**
     * Splits {@code component} into word-wrapped chunks of at most {@code lineSize} characters
     * and inserts each chunk as a separate {@link DescriptionLine} starting at {@code lineStart}.
     * The original component's style is preserved on every wrapped line.
     * @return The number of lines inserted
     */
    public static int lineWrapComponent(Component component, int lineSize, DescriptionLayout description, int lineStart) {
        List<String> textLines = chunkString(component.getString(), lineSize);
        int totalLines = 0;
        for (int i = 0; i < textLines.size(); i++) {
            final int finalI = i;

            description.insertLine(lineStart + i, new DescriptionLine() {
                @Override
                public void render(GuiGraphicsX gx, int lineX, int lineY, MousePos mousePos) {
                    text(gx, getText(), lineX, lineY);
                }

                @Override
                public @NotNull Component getText() {
                    return Component.literal(textLines.get(finalI)).withStyle(component.getStyle());
                }
            });

            totalLines++;
        }

        return totalLines;
    }

    private static List<String> chunkString(String text, int chunkSize) {
        java.util.List<String> parts = new ArrayList<>();

        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            // If adding this word would exceed the limit, flush the current chunk
            if (!current.isEmpty() &&
                    current.length() + 1 + word.length() > chunkSize) {

                parts.add(current.toString());
                current.setLength(0);
            }

            // Append word (with space if needed)
            if (!current.isEmpty()) {
                current.append(' ');
            }
            current.append(word);
        }

        // Add remainder
        if (!current.isEmpty()) {
            parts.add(current.toString());
        }

        return parts;
    }

}
