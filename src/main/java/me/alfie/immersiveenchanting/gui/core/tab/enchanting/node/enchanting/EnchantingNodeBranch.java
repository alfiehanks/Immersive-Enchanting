package me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.enchanting;

import me.alfie.immersiveenchanting.datapack.EnchantmentCostRegistry;
import me.alfie.immersiveenchanting.datapack.EnchantmentMetadataRegistry;
import me.alfie.immersiveenchanting.gui.EnchantingTableScreen;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.NodeBranch;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.NodeType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantingNodeBranch extends NodeBranch {

    private final Holder<Enchantment> enchantmentHolder;
    private final int equippedLevel;
    private final boolean isBranchUnlocked;
    private final Player player;

    public EnchantingNodeBranch(EnchantingTableScreen screen, float branchAngle,
                                Holder<Enchantment> enchantmentHolder,
                                int equippedLevel,
                                boolean isBranchUnlocked,
                                Player player) {
        super(screen, branchAngle);

        this.enchantmentHolder = enchantmentHolder;
        this.equippedLevel = equippedLevel; //0 if not enchantment not equipped.
        this.isBranchUnlocked = isBranchUnlocked;
        this.player = player;

        ResourceLocation iconTexture = new ResourceLocation("immersiveenchanting", "textures/item/ancient_book.png");
        ResourceKey<Enchantment> enchantmentKey = enchantmentHolder.unwrapKey().get();
        ResourceLocation enchantmentRL = enchantmentKey.location();

        //Try to get an icon
        if (EnchantmentMetadataRegistry.getIcons().containsKey(enchantmentRL)) {
            iconTexture = EnchantmentMetadataRegistry.getIconTexture(enchantmentRL);
        }

        int maxEnchantmentLevel;
        if (EnchantmentCostRegistry.getClientRegistry().getCostRegistry().containsKey(enchantmentKey)) {
            maxEnchantmentLevel = EnchantmentCostRegistry.getClientRegistry().getEnchantmentCost(enchantmentKey).getHighestLevel();
        } else {
            //Fallback if no cost
            maxEnchantmentLevel = enchantmentHolder.value().getMaxLevel();
        }

        //Reveal ladder up to (equippedLevel + 1).
        int maxVisibleLevel = Math.min(equippedLevel + 1, maxEnchantmentLevel);

        for (int i = 1; i <= maxVisibleLevel; i++) {
            // Use ELITE if this is the final enchant level, otherwise BASIC
            NodeType nodeType = (i == maxEnchantmentLevel)
                    ? NodeType.ELITE
                    : NodeType.BASIC;

            EnchantingNode node = new EnchantingNode(
                    nodeType,
                    iconTexture,
                    i,
                    enchantmentHolder,
                    this.isBranchUnlocked,
                    this
            );

            if (i <= equippedLevel) {
                node.setObtained(true);
            }

            addNode(node);
        }
    }
}



