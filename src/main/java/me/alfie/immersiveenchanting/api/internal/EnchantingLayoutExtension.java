package me.alfie.immersiveenchanting.api.internal;

import me.alfie.immersiveenchanting.api.DescriptionLayoutExtension;
import me.alfie.immersiveenchanting.api.internal.cost.EnchantingFuelDescriptionLine;
import me.alfie.immersiveenchanting.api.internal.cost.LevelsDescriptionLine;
import me.alfie.immersiveenchanting.api.internal.cost.MaterialsDescriptionLine;
import me.alfie.immersiveenchanting.config.ClientConfig;
import me.alfie.immersiveenchanting.config.ServerConfig;
import me.alfie.immersiveenchanting.datapack.EnchantmentCostRegistry;
import me.alfie.immersiveenchanting.datapack.cost.CostEntry;
import me.alfie.immersiveenchanting.datapack.cost.EnchantmentCost;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.NodeTooltip;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.enchanting.EnchantingNode;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.enchanting.EnchantingNodeTooltip;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.tooltip.DescriptionLayout;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.tooltip.DescriptionLine;
import me.alfie.immersiveenchanting.util.FxHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EnchantingLayoutExtension implements DescriptionLayoutExtension {

    @Override
    public void extendLayout(DescriptionLayout description, NodeTooltip parentTooltip) {
        //Only apply for EnchantingNodeTooltips
        if(parentTooltip instanceof EnchantingNodeTooltip enchantingNodeTooltip) {
            if (enchantingNodeTooltip.node instanceof EnchantingNode enchantingNode) {

                //Cost Layout
                if(!enchantingNode.isObtained() && enchantingNode.isBranchUnlocked) {
                    enchantingNodeTooltip.setCurrentRenderedCost(NodeTooltip.getCycledElement(enchantingNodeTooltip.getValidCosts(), ClientConfig.getItemCarouselSpeed()));

                    List<CostEntry> validFuels = EnchantmentCost.getRenderableAnyOfCosts(
                            EnchantmentCostRegistry.getClientRegistry()
                                    .getEnchantingFuels()
                                    .getCostForLevel(enchantingNode.getEnchantmentLevel()));
                    if(validFuels.isEmpty()) validFuels.add(CostEntry.EMPTY);
                    enchantingNodeTooltip.setCurrentRenderedFuel(NodeTooltip.getCycledElement(validFuels, ClientConfig.getItemCarouselSpeed()));

                    description.insertLine(0, new MaterialsDescriptionLine(enchantingNodeTooltip));

                    int nextLine = 2;
                    if(enchantingNodeTooltip.getCurrentRenderedCost().xpLevels() > 0) {
                        description.insertLine(nextLine, new LevelsDescriptionLine(enchantingNodeTooltip));
                        nextLine += 2;
                    }

                    description.insertLine(nextLine, new EnchantingFuelDescriptionLine(enchantingNodeTooltip));
                }

                //Locked branch
                if(!enchantingNode.isBranchUnlocked) {
                    description.insertLine(0, new DescriptionLine() {
                        @Override
                        public void draw(GuiGraphics graphics, int lineX, int lineY) {
                            //Draw label
                            graphics.drawString(Minecraft.getInstance().font,
                                    getText(),
                                    lineX,
                                    lineY + 4, //Offset to centre text with cost stack
                                    0xFFFFFF);
                        }

                        @Override
                        public @NotNull Component getText() {
                            Component label = Component.translatable("gui.immersiveenchanting.locked_enchantment_hint").withStyle(ChatFormatting.OBFUSCATED, ChatFormatting.GRAY);
                            return label;
                        }
                    });
                }

                //Equipped
                if(enchantingNode.isObtained()) {
                    description.insertLine(0, new DescriptionLine() {
                        @Override
                        public void draw(GuiGraphics graphics, int lineX, int lineY) {
                            //Draw label
                            graphics.drawString(Minecraft.getInstance().font,
                                    getText(),
                                    lineX,
                                    lineY + 4, //Offset to centre text with cost stack
                                    0xFFFFFF);
                        }

                        @Override
                        public @NotNull Component getText() {
                            Component label = Component.translatable("gui.immersiveenchanting.equipped").withStyle(ChatFormatting.LIGHT_PURPLE);
                            return label;
                        }
                    });
                    int enchantmentLevel = enchantingNode.getEnchantmentLevel();
                    int highestUnlockedLevel = enchantingNodeTooltip.screen.getMenu().getToolSlotItem().getEnchantmentLevel(enchantingNode.getEnchantmentHolder());

                    //Removal
                    if (enchantmentLevel == highestUnlockedLevel && ServerConfig.isEnchantmentRemovalAllowed()) {
                        description.insertLine(1, new DescriptionLine() {
                            @Override
                            public void draw(GuiGraphics graphics, int lineX, int lineY) {
                                graphics.drawString(Minecraft.getInstance().font,
                                        getText(),
                                        lineX,
                                        lineY + 4, //Offset to centre text with cost stack
                                        0xFFFFFF);
                            }

                            @Override
                            public @NotNull Component getText() {
                                return Component.translatable("gui.immersiveenchanting.hold_to_remove").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
                            }
                        });

                        if(enchantingNodeTooltip.screen.enchantingTab.getNodeHeldTime() > 0 ) {
                            description.insertLine(2, new DescriptionLine() {
                                @Override
                                public void draw(GuiGraphics graphics, int lineX, int lineY) {
                                    long heldTime = enchantingNodeTooltip.screen.enchantingTab.getNodeHeldTime();
                                    long threshold = enchantingNodeTooltip.screen.enchantingTab.HOLD_THRESHOLD;
                                    float progress = Math.min(1f, (float) heldTime / threshold);

                                    int bars = (int) (heldTime / (threshold/15));

                                    FxHelper.playRemoveProgressSound(enchantingNodeTooltip.screen.player,
                                            enchantingNodeTooltip.lastBars, bars, progress);
                                    enchantingNodeTooltip.lastBars = bars;

                                    String barText = "";
                                    for (int i = 0; i < bars; i++) {
                                        barText += "|";
                                    }

                                    Component barComponent = Component.literal(barText).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                                    graphics.drawString(Minecraft.getInstance().font,
                                            barComponent,
                                            lineX,
                                            lineY + 4, //Offset to centre text with cost stack
                                            0xFFFFFF);
                                }

                                @Override
                                public @NotNull Component getText() {
                                    return Component.empty();
                                }
                            });
                        }
                    }
                }
            }
        }
    }


}
