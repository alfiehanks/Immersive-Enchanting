package me.alfie.immersiveenchanting.util;

import me.alfie.alfinolib.networking.Networking;
import me.alfie.immersiveenchanting.block.ModBlocks;
import me.alfie.immersiveenchanting.config.ServerConfig;
import me.alfie.immersiveenchanting.item.ModItems;
import me.alfie.immersiveenchanting.networking.AvailableEnchantmentsPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BookshelfChecker {

    public static void checkBookshelves(BlockPos tablePos, Level level, ServerPlayer serverPlayer) {
        List<Holder<Enchantment>> availableEnchantments = getEnchantmentsInBookshelves(tablePos, level);

        Networking.sendToClient(serverPlayer, new AvailableEnchantmentsPacket(
                EnchantmentUtil.toResourceKeys(availableEnchantments)));
    }

    public static List<Holder<Enchantment>> getEnchantmentsInBookshelves(BlockPos blockPos, Level level) {
        if(isCreativeBookshelfNearby(blockPos, level)) return EnchantmentUtil.getAllEnchantmentsInRegistry(level.registryAccess());
        if(!ServerConfig.areAncientBooksRequired()) return EnchantmentUtil.getAllEnchantmentsInRegistry(level.registryAccess());

        List<ChiseledBookShelfBlockEntity> bookshelves = getNearbyBookshelves(blockPos, level);
        List<Holder<Enchantment>> result = new ArrayList<>();

        for(ChiseledBookShelfBlockEntity bookshelf : bookshelves) {
            List<ItemStack> books = getBooks(bookshelf);

            for(ItemStack stack : books) {
                if(stack.getItem() == ModItems.ANCIENT_BOOK.get()) {
                    List<Holder<Enchantment>> enchantmentHolder = EnchantmentUtil.getAllStoredEnchantments(stack);
                    if(enchantmentHolder != null) result.addAll(enchantmentHolder);
                }
            }
        }

        return result;
    }

    private static List<ChiseledBookShelfBlockEntity> getNearbyBookshelves(BlockPos pos, Level level) {
        List<ChiseledBookShelfBlockEntity> result = new ArrayList<>();

        forEachRingPos(pos,
                2, ServerConfig.getBookshelfSearchRadius().x(),
                0, ServerConfig.getBookshelfSearchRadius().y(),
                2, ServerConfig.getBookshelfSearchRadius().z(),
                checkPos -> {
            BlockEntity be = level.getBlockEntity(checkPos);

            if (be instanceof ChiseledBookShelfBlockEntity shelf) {
                result.add(shelf);
            }
        });

        return result;
    }

    private static List<ItemStack> getBooks(ChiseledBookShelfBlockEntity bookshelf) {
        List<ItemStack> result = new ArrayList<>();

        for (int i = 0; i < 6; i++) {
            result.add(bookshelf.getItem(i));
        }

        return result;
    }

    /**
     * Iterates over every block position in a hollow rectangular prism shell around {@code center}.
     * The shell spans {@code ±maxRadius} on each axis but skips any position where both
     * {@code |dx| < minRadiusX} and {@code |dz| < minRadiusZ}, producing a ring rather than
     * a solid box. The Y axis is not hollow: all offsets from {@code minRadiusY} to
     * {@code maxRadiusY} (inclusive) are visited.
     */
    private static void forEachRingPos(
            BlockPos center,
            int minRadiusX, int maxRadiusX,
            int minRadiusY, int maxRadiusY,
            int minRadiusZ, int maxRadiusZ,
            Consumer<BlockPos> consumer
    ) {
        for (int dy = minRadiusY; dy <= maxRadiusY; dy++) {
            for (int dx = -maxRadiusX; dx <= maxRadiusX; dx++) {
                for (int dz = -maxRadiusZ; dz <= maxRadiusZ; dz++) {

                    int absX = Math.abs(dx);
                    int absZ = Math.abs(dz);

                    //skip inside inner ring
                    if (absX < minRadiusX && absZ < minRadiusZ) continue;

                    //skip max bounds
                    if (absX > maxRadiusX || absZ > maxRadiusZ) continue;

                    consumer.accept(center.offset(dx, dy, dz));
                }
            }
        }
    }

    /**
     * Returns {@code true} as soon as {@code predicate} matches any block in the hollow ring,
     * using the same ring geometry as {@link #forEachRingPos} but stopping early on the first
     * match. Y range is {@code minRadiusY} to {@code maxRadiusY - 1} (exclusive upper bound).
     */
    private static boolean anyInRing(
            BlockPos center,
            int minRadiusX, int maxRadiusX,
            int minRadiusY, int maxRadiusY,
            int minRadiusZ, int maxRadiusZ,
            Predicate<BlockPos> predicate
    ) {
        for (int dy = minRadiusY; dy <= maxRadiusY-1; dy++) {
            for (int dx = -maxRadiusX; dx <= maxRadiusX; dx++) {
                for (int dz = -maxRadiusZ; dz <= maxRadiusZ; dz++) {

                    int absX = Math.abs(dx);
                    int absZ = Math.abs(dz);

                    //skip inside inner ring
                    if (absX < minRadiusX && absZ < minRadiusZ) continue;

                    //skip max bounds
                    if (absX > maxRadiusX || absZ > maxRadiusZ) continue;

                    if (predicate.test(center.offset(dx, dy, dz))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isCreativeBookshelfNearby(BlockPos pos, Level level) {
        return anyInRing(pos,
                2, ServerConfig.getBookshelfSearchRadius().x(),
                0, ServerConfig.getBookshelfSearchRadius().y(),
                2, ServerConfig.getBookshelfSearchRadius().z(),
                checkPos ->
                level.getBlockState(checkPos).getBlock()
                        .equals(ModBlocks.CREATIVE_BOOKSHELF_BLOCK.get())
        );
    }
}
