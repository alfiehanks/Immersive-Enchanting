package me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.enchanting;

import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.config.ServerConfig;
import me.alfie.immersiveenchanting.datapack.cost.CostEntry;
import me.alfie.immersiveenchanting.gui.EnchantingTableScreen;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.NodeTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

public class EnchantingNodeTooltip extends NodeTooltip {


    public int lastBars = 0;
    private CostEntry currentRenderedFuel;
    private Vector2i fuelStackPos = new Vector2i(0, 0);
    public List<Component> fuelDescriptionComponents = new ArrayList<>() {{add(Component.empty());}};

    public EnchantingNodeTooltip(EnchantingNode node,
                                 EnchantingTableScreen screen,
                                 List<CostEntry> validCosts) {
        super(node, screen, validCosts);

        //Decide title text
        Component titleText = Enchantment.getFullname(node.getEnchantmentHolder(), node.getEnchantmentLevel())
                .copy() // creates a mutable copy
                .withStyle(ChatFormatting.WHITE);
        if(!node.isBranchUnlocked) {
            if(ServerConfig.isObfuscateLockedEnchantments()) {
                titleText = ImmersiveEnchanting.styleWithAltFont(titleText);
            }
        }


        tooltipTitle.setTitleText(titleText);
    }

    public Vector2i getFuelStackPos() {
        return fuelStackPos;
    }

    public void setFuelStackPos(Vector2i fuelStackPos) {
        this.fuelStackPos = fuelStackPos;
    }

    public void setCurrentRenderedFuel(CostEntry currentRenderedFuel) {
        this.currentRenderedFuel = currentRenderedFuel;

        CostEntry renderedCost = getCurrentRenderedFuel();

        if(renderedCost.getCostItemTag().isPresent()) {
            String itemTag = renderedCost.getCostItemTag().get().itemTag();
            fuelDescriptionComponents.set(0, Component.translatable("gui.immersiveenchanting.accepts_any_tag", itemTag)
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    public CostEntry getCurrentRenderedFuel() {
        return currentRenderedFuel;
    }
}
