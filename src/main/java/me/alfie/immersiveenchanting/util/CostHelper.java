package me.alfie.immersiveenchanting.util;

import me.alfie.alfinolib.util.ResourceId;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.CostRegistry;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.codec.Cost;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.codec.CostHolder;
import me.alfie.immersiveenchanting.gui.EnchantingTableMenu;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public class CostHelper {
    public static boolean canEnchant(EnchantingTableMenu menu,
                                     Holder<Enchantment> enchantmentHolder, int level,
                                     Player player) {
        if(!CostRegistry.server().isRegistered(enchantmentHolder) ||
            !CostRegistry.server().isRegistered(CostRegistry.ENCHANTING_FUELS)) return false;
        if(!CostRegistry.server().get(enchantmentHolder).enabled()) return false;
        if(!isEnchantmentAvailableInBookshelves(enchantmentHolder, menu, player)) return false;

        ItemStack stackToEnchant = menu.getToolSlot().getItem();

        if(!stackToEnchant.is(Items.BOOK)) {
            if(!isEnchantmentNextLevel(stackToEnchant, enchantmentHolder, level)) return false;
            if(!stackToEnchant.supportsEnchantment(enchantmentHolder)) return false;
        }

        return tryConsumeValidCostAndFuel(
                EnchantmentUtil.toId(enchantmentHolder), level,
                menu, player);
    }

    public static boolean canTransmute(EnchantingTableMenu menu, Holder<Enchantment> oldEnchantment, Player player) {
        if(!CostRegistry.server().isRegistered(CostRegistry.TRANSMUTE)) return false;
        if(!CostRegistry.server().get(CostRegistry.TRANSMUTE).enabled()) return false;
        if(!isEnchantmentAvailableInBookshelves(oldEnchantment, menu, player)) return false;
        if(EnchantmentUtil.isReplicated(menu.getToolSlot().getItem())) return false;

        return tryConsumeValidCostAndFuel(
                CostRegistry.TRANSMUTE, 1,
                menu, player);
    }

    public static boolean canReplicate(EnchantingTableMenu menu, Player player) {
        if(!CostRegistry.server().isRegistered(CostRegistry.REPLICATE)) return false;
        if(!CostRegistry.server().get(CostRegistry.REPLICATE).enabled()) return false;

        return tryConsumeValidCostAndFuel(
                CostRegistry.REPLICATE, 1,
                menu, player);
    }

    private static boolean isEnchantmentAvailableInBookshelves(Holder<Enchantment> enchantmentHolder, EnchantingTableMenu menu, Player player) {
        List<Holder<Enchantment>> availableEnchantments = BookshelfChecker.getEnchantmentsInBookshelves(menu.getBlockPos(), player.level());
        return availableEnchantments.contains(enchantmentHolder);
    }

    private static boolean isEnchantmentNextLevel(ItemStack stack, Holder<Enchantment> enchantmentHolder, int level) {
        int equippedLevel = EnchantmentUtil.getEnchantmentLevel(stack, enchantmentHolder);
        return level == equippedLevel + 1;
    }

    private static boolean tryConsumeValidCostAndFuel(ResourceId costId,
                                                      int costLevel,
                                                      EnchantingTableMenu menu,
                                                      Player player) {
        if(player.hasInfiniteMaterials()) return true;
        CostHolder costHolder = CostRegistry.server().get(costId).levelCosts().getLevel(costLevel);
        CostHolder fuelHolder = CostRegistry.server().get(CostRegistry.ENCHANTING_FUELS).levelCosts().getLevel(costLevel);

        Cost validCost = null;
        Cost validFuel = null;

        for(Cost cost : costHolder.costs()) {
            validCost = cost.test(menu.getCostSlot().getItem(), player);
        }

        for(Cost cost : fuelHolder.costs()) {
            validFuel = cost.test(menu.getFuelSlot().getItem(), player);
        }

        if(validCost == null || validFuel == null || player.experienceLevel < validCost.xpLevels()) return false;

        validCost.itemCost().tryConsume(menu.getCostSlot().getItem());
        validFuel.itemCost().tryConsume(menu.getFuelSlot().getItem());
        player.giveExperienceLevels(-validCost.xpLevels());
        return true;
    }
}
