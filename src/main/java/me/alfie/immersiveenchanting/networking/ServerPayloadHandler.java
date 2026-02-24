package me.alfie.immersiveenchanting.networking;

import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.block.CreativeBookshelf;
import me.alfie.immersiveenchanting.config.ServerConfig;
import me.alfie.immersiveenchanting.datapack.EnchantmentCostRegistry;
import me.alfie.immersiveenchanting.gui.EnchantingTableMenu;
import me.alfie.immersiveenchanting.item.AncientBook;
import me.alfie.immersiveenchanting.item.ModItems;
import me.alfie.immersiveenchanting.networking.packets.EnchantItemPacket;
import me.alfie.immersiveenchanting.networking.packets.GetBookshelfContentsPacket;
import me.alfie.immersiveenchanting.networking.packets.UnlockedEnchantmentsPacket;
import me.alfie.immersiveenchanting.networking.packets.UpdateToolSlotPacket;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ServerPayloadHandler {

    /**
     * Apply an enchantment to an item.
     *
     * @param packet
     * @param context
     */
    public static void onEnchantItem(final EnchantItemPacket packet, final NetworkEvent.Context context) {
        Player player = context.getSender();
        if (player == null) return;
        Level level = player.level();

        AbstractContainerMenu enchantingTableMenu = player.containerMenu;
        ItemStack itemToEnchant = enchantingTableMenu.getSlot(EnchantingTableMenu.SLOTS.TOOL.ordinal()).getItem();

        RegistryAccess registryAccess = player.level().registryAccess();
        Optional<Holder.Reference<Enchantment>> enchantmentHolder = ImmersiveEnchanting.getEnchantmentHolder(
                registryAccess,
                packet.enchantment
        );

        Holder<Enchantment> enchantment = enchantmentHolder.orElseThrow(() ->
                new IllegalStateException("Enchantment not found: " + packet.enchantment)
        );

        //Check enchantment cost
        final int xpLevelCost = packet.enchantmentLevel * 3;
        boolean hasEnoughCost;

        if (ServerConfig.isXpCostModeEnabled()) {
            hasEnoughCost = player.isCreative() || player.experienceLevel >= xpLevelCost;
        } else {
            ItemStack costSlotItemStack = enchantingTableMenu.getSlot(EnchantingTableMenu.SLOTS.COST.ordinal()).getItem();
            ItemStack requiredItemCostStack = EnchantmentCostRegistry.getServerRegistry().getEnchantmentCost(packet.enchantment).getLevel(packet.enchantmentLevel).asItemStack();

            ItemStack requiredLapisCost = EnchantmentCostRegistry.getServerRegistry().getLapisCost();
            ItemStack lapisSlotStack = enchantingTableMenu.getSlot(EnchantingTableMenu.SLOTS.LAPIS.ordinal()).getItem();

            hasEnoughCost = player.isCreative() ||
                    (requiredLapisCost.isEmpty() ||
                            (lapisSlotStack.is(requiredLapisCost.getItem()) && lapisSlotStack.getCount() >= requiredLapisCost.getCount()))
                            && (requiredItemCostStack.isEmpty() ||
                            (costSlotItemStack.is(requiredItemCostStack.getItem()) &&
                                    costSlotItemStack.getCount() >= requiredItemCostStack.getCount()));
        }

        if (hasEnoughCost) {
            if (!player.isCreative()) {
                if (ServerConfig.isXpCostModeEnabled()) {
                    player.giveExperienceLevels(-xpLevelCost);
                } else {
                    ItemStack costSlotItemStack = enchantingTableMenu.getSlot(EnchantingTableMenu.SLOTS.COST.ordinal()).getItem();
                    ItemStack requiredItemCostStack = EnchantmentCostRegistry.getServerRegistry().getEnchantmentCost(packet.enchantment).getLevel(packet.enchantmentLevel).asItemStack();
                    ItemStack requiredLapisCost = EnchantmentCostRegistry.getServerRegistry().getLapisCost();
                    enchantingTableMenu.getSlot(EnchantingTableMenu.SLOTS.LAPIS.ordinal()).getItem()
                            .shrink(requiredLapisCost.getCount()); //Use enchantment cost registry
                    if (!requiredItemCostStack.isEmpty()) {
                        costSlotItemStack.shrink(requiredItemCostStack.getCount()); //Use enchantment cost if not air
                    }
                }
            }

            //Enchant item server side
            //1.20.1 item.enchant() doesnt overwrite enchantments, it appends them
            //Remove old enchantment and add the new one
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(itemToEnchant);
            enchantments.remove(enchantment.get()); //Remove old one
            enchantments.put(enchantment.get(), packet.enchantmentLevel); //Add new one
            EnchantmentHelper.setEnchantments(enchantments, itemToEnchant);

            player.awardStat(Stats.ENCHANT_ITEM);
            if (player instanceof ServerPlayer serverPlayer) {
                int levelsSpent = ServerConfig.isXpCostModeEnabled() ? xpLevelCost : 1;
                CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, itemToEnchant, levelsSpent);
            }

            if (packet.enchantmentLevel == EnchantmentCostRegistry.getServerRegistry().getEnchantmentCost(packet.enchantment).getHighestLevel()) {
                //Sound FX for highest tier.
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                //Sound FX for normal tier.
                level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        } else {
            //If unable to enchant
            level.playSound(null, player.blockPosition(), SoundEvents.IRON_TRAPDOOR_CLOSE,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public static void onUpdateSlotPacket(final UpdateToolSlotPacket packet, final NetworkEvent.Context context) {
        int mode = packet.mode;

        Slot toolSlot = context.getSender().containerMenu.getSlot(EnchantingTableMenu.SLOTS.TOOL.ordinal());
        if (mode == UpdateToolSlotPacket.MODE.TAKE.ordinal()) {
            ItemStack itemStack = toolSlot.getItem();
            context.getSender().containerMenu.setCarried(itemStack.copyAndClear());
            toolSlot.setChanged();
        } else if (mode == UpdateToolSlotPacket.MODE.PLACE.ordinal()) {
            ItemStack carriedStack = context.getSender().containerMenu.getCarried();
            toolSlot.safeInsert(carriedStack);
            toolSlot.setChanged();
        }
    }

    public static void onGetBookshelfContentsPacket(final GetBookshelfContentsPacket packet, final NetworkEvent.Context context) {
        BlockPos tablePos = new BlockPos(packet.blockPosX, packet.blockPosY, packet.blockPosZ);
        checkBookshelvesAndUpdateClient(tablePos, context.getSender().level(), context.getSender());
    }

    /**
     * Check nearby bookshelves for ancient books and send UnlockedEnchantmentsPacket to client.
     *
     * @param tablePos
     * @param level
     * @param serverPlayer
     */
    public static void checkBookshelvesAndUpdateClient(BlockPos tablePos, Level level, ServerPlayer serverPlayer) {
        List<BlockEntity> bookshelves = getChiseledBookshelvesNearby(tablePos, level);

        //Store resource locations as strings - i.e "minecraft:respiration"
        List<ResourceKey<Enchantment>> unlockedEnchantments = new ArrayList<>();
        for (BlockEntity bookshelf : bookshelves) {
            List<ItemStack> books = getChiseledBookshelfContents(bookshelf);

            //Get books in bookshelf
            for (ItemStack book : books) {
                if (book.getItem() == ModItems.ANCIENT_BOOK.get()) {
                    ResourceKey<Enchantment> key = AncientBook.getEnchantment(book, level);

                    if (key != null) {
                        unlockedEnchantments.add(key);
                    }
                } else if (ServerConfig.isVanillaBookModeEnabled() && book.getItem() instanceof EnchantedBookItem) {
                    ListTag storedEnchantments = EnchantedBookItem.getEnchantments(book);
                    for (int i = 0; i < storedEnchantments.size(); i++) {
                        CompoundTag tag = storedEnchantments.getCompound(i);
                        ResourceLocation enchantmentRL = ResourceLocation.tryParse(tag.getString("id"));
                        if (enchantmentRL != null) {
                            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, enchantmentRL);
                            unlockedEnchantments.add(key);
                        }
                    }
                }
            }
        }

        //Unlock all enchantments if creative bookshelf is near
        if (isCreativeBookshelfNearby(tablePos, level) || !ServerConfig.areAncientBooksRequired()) {
            //Clear unlockedEnchantments from the bookshelf search
            unlockedEnchantments.clear();

            //Add all enchantments that exist
            RegistryAccess registryAccess = level.registryAccess();
            Registry<Enchantment> enchantmentRegistry = ImmersiveEnchanting.getEnchantmentRegistry(registryAccess);
            List<Holder.Reference<Enchantment>> allEnchantments = enchantmentRegistry.asLookup().listElements().toList();

            // Convert each Holder to its ResourceLocation string
            unlockedEnchantments = allEnchantments.stream()
                    .map(Holder.Reference::key)
                    .toList();
        }

        ModPacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> serverPlayer),
                new UnlockedEnchantmentsPacket(unlockedEnchantments)
        );
    }

    /**
     * Returns a list of all the bookshelves in a 5x5 radius of a coordinate.
     * Same positions as the vanilla enchanting table searches for.
     *
     * @param pos
     * @param level
     * @return
     */
    private static List<BlockEntity> getChiseledBookshelvesNearby(BlockPos pos, Level level) {
        List<BlockEntity> blockEntities = new ArrayList<>();

        for (int dy = 0; dy <= ServerConfig.getBookshelfSearchHeight()-1; dy++) { // check table level and level above
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    // Skip inner 3x3 square; only outer ring
                    if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;

                    BlockPos checkPos = pos.offset(dx, dy, dz);
                    BlockEntity blockEntity = level.getBlockEntity(checkPos);

                    if (blockEntity instanceof ChiseledBookShelfBlockEntity) {
                        blockEntities.add(blockEntity);
                    }
                }
            }
        }

        return blockEntities;
    }

    /**
     * Returns all the itemIds contained in a chiseled bookshelf.
     *
     * @param shelf
     * @return
     */
    private static List<ItemStack> getChiseledBookshelfContents(BlockEntity shelf) {
        List<ItemStack> contents = new ArrayList<>();
        if (shelf instanceof ChiseledBookShelfBlockEntity) {
            for (int i = 0; i < 6; i++) {
                ItemStack stack = ((ChiseledBookShelfBlockEntity) shelf).getItem(i);
                contents.add(stack);
            }

        }
        return contents;
    }

    /**
     * Returns true if a creative bookshelf is within the 5x5 ring.
     *
     * @param pos
     * @param level
     * @return
     */
    private static boolean isCreativeBookshelfNearby(BlockPos pos, Level level) {
        for (int dy = 0; dy <= 1; dy++) { // check table level and level above
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    // Skip inner 3x3 square; only outer ring
                    if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;

                    BlockPos checkPos = pos.offset(dx, dy, dz);

                    if (level.getBlockState(checkPos).getBlock() instanceof CreativeBookshelf) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

