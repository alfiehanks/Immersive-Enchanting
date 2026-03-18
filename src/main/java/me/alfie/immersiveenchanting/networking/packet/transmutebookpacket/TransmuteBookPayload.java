package me.alfie.immersiveenchanting.networking.packet.transmutebookpacket;

import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.datacomponent.ReplicatedDataComponent;
import me.alfie.immersiveenchanting.datapack.EnchantmentCostRegistry;
import me.alfie.immersiveenchanting.datapack.cost.CostDefinition;
import me.alfie.immersiveenchanting.datapack.cost.CostEntry;
import me.alfie.immersiveenchanting.util.CostHelper;
import me.alfie.immersiveenchanting.gui.EnchantingTableMenu;
import me.alfie.immersiveenchanting.item.AncientBook;
import me.alfie.immersiveenchanting.networking.packet.PayloadHandler;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import me.alfie.immersiveenchanting.util.FxHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class TransmuteBookPayload implements PayloadHandler<TransmuteBookPacket> {
    @Override
    public void execOnClient(TransmuteBookPacket packet, IPayloadContext context) {}

    @Override
    public void execOnServer(TransmuteBookPacket packet, IPayloadContext context) {
        Player player = context.player();
        Level level = player.level();
        AbstractContainerMenu menu = player.containerMenu;

        if(menu instanceof EnchantingTableMenu enchantingTableMenu) {
            ItemStack ancientBookStack = enchantingTableMenu.getToolSlotItem();
            Set<Holder<Enchantment>> unlockedEnchantments = enchantingTableMenu.getUnlockedEnchantments();

            //Get all enchantments
            List<Holder.Reference<Enchantment>> enchantments = new ArrayList<>(EnchantmentUtil.getAllEnchantments(player.level()));

            //Remove all enchantments that are already in bookshelf
            enchantments.removeIf(unlockedEnchantments::contains);

            //If all enchantments are unlocked already, pick any random enchantment.
            if(enchantments.isEmpty()) enchantments = new ArrayList<>(EnchantmentUtil.getAllEnchantments(player.level()));

            Random random = new Random();
            int randomIndex = random.nextInt(enchantments.size());
            Holder<Enchantment> randomEnchantment = enchantments.get(randomIndex);

            //Check cost
            boolean isBookReplicated = ReplicatedDataComponent.isReplicated(ancientBookStack);
            int playerXp = player.experienceLevel;
            ItemStack costSlotItemStack = enchantingTableMenu.getCostSlotItem();
            List<ItemStack> insertedItems = new ArrayList<>();
            insertedItems.add(costSlotItemStack);

            CostDefinition costNode = EnchantmentCostRegistry.getServerRegistry().getTransmuteCost().getCostForLevel(1);
            CostEntry validCost = CostHelper.findValidCost(costNode, insertedItems, playerXp);

            boolean hasEnoughCost = player.hasInfiniteMaterials()
                    || CostHelper.isCostValid(validCost);

            if (hasEnoughCost && !isBookReplicated) {
                if(player.hasInfiniteMaterials()) validCost = CostEntry.EMPTY;
                assert validCost != null;
                CostHelper.deductCost(validCost, costSlotItemStack, player);

                AncientBook.setStoredEnchantment(ancientBookStack, randomEnchantment);
                BlockPos tablePos = enchantingTableMenu.getBlockPos();
                FxHelper.playTransmuteFx(level, tablePos);

                ItemStack stack = enchantingTableMenu.getToolSlotItem().copyAndClear();
                ItemEntity entity = new ItemEntity(
                        level,
                        tablePos.getX() + 0.5,
                        tablePos.getY() + 1,
                        tablePos.getZ() + 0.5,
                        stack);
                entity.setPickUpDelay(40);
                entity.setDeltaMovement(Vec3.ZERO);
                level.addFreshEntity(entity);

                Enchantment newEnchantment = randomEnchantment.value();
                Component enchantmentName = newEnchantment.description().copy().withStyle(ChatFormatting.GOLD);
                Component text = Component.translatable("gui.immersiveenchanting.transmuted_to",
                                enchantmentName)
                        .withStyle(ChatFormatting.GRAY);


                player.displayClientMessage(text, true);
                player.closeContainer();
            } else {
                FxHelper.playEnchantFailFx(level, enchantingTableMenu.getBlockPos());
            }
        }
    }
}
