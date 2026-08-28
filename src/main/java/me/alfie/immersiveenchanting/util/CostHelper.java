package me.alfie.immersiveenchanting.util;

import me.alfie.alfinolib.util.ResourceId;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.CostRegistry;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.codec.Cost;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.codec.CostHolder;
import me.alfie.immersiveenchanting.compat.BundledNotSiloedCompat;
import me.alfie.immersiveenchanting.gui.EnchantingTableMenu;
import me.alfie.alfinolib.util.codec.ItemCost;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.IdentityHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        for(Cost cost : costHolder.costs()) {
            if(player.experienceLevel < cost.xpLevels()) continue;

            for(Cost fuel : fuelHolder.costs()) {
                if(player.experienceLevel < fuel.xpLevels()) continue;

                PaymentPlan payment = new PaymentPlan(player);
                if(!payment.reserve(cost.itemCost(), menu.getCostSlot().getItem())) continue;
                if(!payment.reserve(fuel.itemCost(), menu.getFuelSlot().getItem())) continue;

                if(!payment.consume()) continue;
                menu.getCostSlot().setChanged();
                menu.getFuelSlot().setChanged();
                player.getInventory().setChanged();
                player.giveExperienceLevels(-cost.xpLevels());
                return true;
            }
        }

        return false;
    }

    /** Builds an atomic payment across a legacy table slot and the player's complete inventory. */
    private static final class PaymentPlan {
        private final List<ItemStack> inventoryStacks;
        private final Map<ItemStack, Integer> reservedCounts = new IdentityHashMap<>();
        private final BundledNotSiloedCompat.InventoryAccess bundledInventory;
        private final List<BundledNotSiloedCompat.InventoryEntry> bundledEntries;
        private final List<BundledReservation> bundledReservations = new ArrayList<>();

        private PaymentPlan(Player player) {
            bundledInventory = BundledNotSiloedCompat.inventory(player).orElse(null);
            bundledEntries = bundledInventory == null ? List.of() : bundledInventory.entries();
            inventoryStacks = bundledInventory == null ? player.getInventory().items : List.of();
        }

        private boolean reserve(ItemCost itemCost, ItemStack tableStack) {
            if(itemCost.getItems().contains(Items.AIR) || itemCost.count() <= 0) return true;

            int remaining = itemCost.count();
            remaining = reserveFromStack(itemCost, tableStack, remaining);

            if(bundledInventory != null) {
                remaining = reserveFromBundledInventory(itemCost, remaining);
            } else {
                for(ItemStack stack : inventoryStacks) {
                    if(remaining == 0) break;
                    remaining = reserveFromStack(itemCost, stack, remaining);
                }
            }

            return remaining == 0;
        }

        private int reserveFromStack(ItemCost itemCost, ItemStack stack, int remaining) {
            if(stack.isEmpty() || !matchesIgnoringCount(itemCost, stack)) return remaining;

            int alreadyReserved = reservedCounts.getOrDefault(stack, 0);
            int available = stack.getCount() - alreadyReserved;
            int amount = Math.min(remaining, Math.max(available, 0));
            if(amount > 0) reservedCounts.put(stack, alreadyReserved + amount);
            return remaining - amount;
        }

        private boolean matchesIgnoringCount(ItemCost itemCost, ItemStack stack) {
            ItemStack candidate = stack.copy();
            candidate.setCount(Math.max(itemCost.count(), 1));
            return itemCost.isValid(candidate);
        }

        private int reserveFromBundledInventory(ItemCost itemCost, int remaining) {
            for(BundledNotSiloedCompat.InventoryEntry entry : bundledEntries) {
                if(remaining == 0) break;

                ItemStack representative = entry.representative();
                if(!matchesIgnoringCount(itemCost, representative)) continue;

                BundledReservation reservation = getBundledReservation(representative);
                long available = entry.quantity() - reservation.amount;
                int amount = (int)Math.min(remaining, Math.max(available, 0));
                reservation.amount += amount;
                remaining -= amount;
            }
            return remaining;
        }

        private BundledReservation getBundledReservation(ItemStack prototype) {
            for(BundledReservation reservation : bundledReservations) {
                if(ItemStack.isSameItemSameComponents(reservation.prototype, prototype)) return reservation;
            }

            BundledReservation reservation = new BundledReservation(prototype.copyWithCount(1));
            bundledReservations.add(reservation);
            return reservation;
        }

        private boolean consume() {
            for(BundledReservation reservation : bundledReservations) {
                if(bundledInventory.count(reservation.prototype) < reservation.amount) return false;
            }

            for(BundledReservation reservation : bundledReservations) {
                if(bundledInventory.extract(reservation.prototype, reservation.amount) != reservation.amount) return false;
            }

            reservedCounts.forEach(ItemStack::shrink);
            return true;
        }

        private static final class BundledReservation {
            private final ItemStack prototype;
            private int amount;

            private BundledReservation(ItemStack prototype) {
                this.prototype = prototype;
            }
        }
    }
}
