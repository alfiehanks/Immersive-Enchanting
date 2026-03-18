package me.alfie.immersiveenchanting.networking.packet.enchantitem;

import me.alfie.immersiveenchanting.compat.ModCompat;
import me.alfie.immersiveenchanting.datapack.EnchantmentCostRegistry;
import me.alfie.immersiveenchanting.datapack.cost.CostDefinition;
import me.alfie.immersiveenchanting.datapack.cost.CostEntry;
import me.alfie.immersiveenchanting.util.CostHelper;
import me.alfie.immersiveenchanting.gui.EnchantingTableMenu;
import me.alfie.immersiveenchanting.networking.packet.PayloadHandler;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import me.alfie.immersiveenchanting.util.FxHelper;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EnchantItemPayload implements PayloadHandler<EnchantItemPacket> {
    @Override //Empty
    public void execOnClient(EnchantItemPacket packet, IPayloadContext context) {}

    @Override
    public void execOnServer(EnchantItemPacket packet, IPayloadContext context) {
        Player player = context.player();
        Level level = player.level();
        EnchantingTableMenu enchantingTableMenu = (EnchantingTableMenu) player.containerMenu;
        ItemStack itemToEnchant = enchantingTableMenu.getToolSlotItem();
        BlockPos tablePos = enchantingTableMenu.getBlockPos();


        Optional<Holder.Reference<Enchantment>> enchantmentReference = EnchantmentUtil.getEnchantmentHolder(
                level.registryAccess(), packet.enchantment());
        Holder<Enchantment> enchantmentHolder = enchantmentReference.orElseThrow(() ->
                new IllegalStateException("Enchantment not found: " + packet.enchantment()));

        //Currently, this check only exists on NeoForge 1.21.1 as Enchant Limiter is not available on Forge 1.20.1.
        if (!ModCompat.canEnchant(itemToEnchant, enchantmentHolder)) {
            FxHelper.playEnchantFailFx(level, tablePos);
            return;
        }

        int playerXp = player.experienceLevel;
        ItemStack costSlotItemStack = enchantingTableMenu.getCostSlotItem();
        List<ItemStack> insertedItems = new ArrayList<>();
        insertedItems.add(costSlotItemStack);
        List<ItemStack> insertedFuels = new ArrayList<>();
        insertedFuels.add(enchantingTableMenu.getEnchantingFuelSlotItem());

        //Check cost
        CostDefinition costNode = EnchantmentCostRegistry.getServerRegistry().getEnchantmentCost(packet.enchantment()).getCostForLevel(packet.enchantmentLevel());
        CostEntry validCost = CostHelper.findValidCost(costNode, insertedItems, playerXp);

        //Check fuel
        CostDefinition enchantingFuelNode = EnchantmentCostRegistry.getServerRegistry().getEnchantingFuels().getCostForLevel(packet.enchantmentLevel());
        CostEntry validEnchantingFuel = CostHelper.findValidCost(enchantingFuelNode, insertedFuels, playerXp);

        boolean hasEnoughCost = player.hasInfiniteMaterials()
                || (CostHelper.isCostValid(validCost)
                    && CostHelper.isCostValid(validEnchantingFuel));

        if (hasEnoughCost) {
            if(player.hasInfiniteMaterials()) {
                validCost = CostEntry.EMPTY;
                validEnchantingFuel = CostEntry.EMPTY;
            }

            assert validCost != null;
            assert validEnchantingFuel != null;
            CostHelper.deductCost(validCost, validEnchantingFuel,
                    costSlotItemStack, enchantingTableMenu.getEnchantingFuelSlotItem(),
                    player);

            itemToEnchant.enchant(enchantmentHolder, packet.enchantmentLevel());

            player.awardStat(Stats.ENCHANT_ITEM);
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, itemToEnchant, validCost.xpLevels());
            }

            boolean isHighestTier = packet.enchantmentLevel() == EnchantmentCostRegistry.getServerRegistry()
                    .getEnchantmentCost(packet.enchantment())
                    .getHighestLevel();
            FxHelper.playEnchantSuccessFx(level, tablePos, isHighestTier);
        } else {
            FxHelper.playEnchantFailFx(level, tablePos);
        }
    }
}
