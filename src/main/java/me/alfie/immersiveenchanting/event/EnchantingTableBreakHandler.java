package me.alfie.immersiveenchanting.event;

import me.alfie.immersiveenchanting.util.IEnchantingTableInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.Collections;

/**
 * Drops the persistent enchanting-table inventory when the block is broken.
 *
 * <p>Since Immersive Enchanting does not own the enchanting-table block
 * (it modifies the vanilla one via a Mixin), we hook into the
 * {@link BreakBlockEvent} game-bus event to detect breakage and drop
 * the stored items. After dropping, the slots are cleared to make sure no
 * duplication can occur.</p>
 */
public final class EnchantingTableBreakHandler {

    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) return;

        BlockPos pos = event.getPos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof EnchantingTableBlockEntity)) return;

        IEnchantingTableInventory inventory = (IEnchantingTableInventory) be;
        NonNullList<ItemStack> items = inventory.immersive$getItems();

        boolean anything = false;
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) { anything = true; break; }
        }
        if (!anything) return;

        Containers.dropContents(level, pos, items);

        // Clear the list so nothing can be duplicated if the BE is read again
        // before unloading.
        Collections.fill(items, ItemStack.EMPTY);
    }
}
