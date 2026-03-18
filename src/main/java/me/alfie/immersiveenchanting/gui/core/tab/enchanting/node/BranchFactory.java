package me.alfie.immersiveenchanting.gui.core.tab.enchanting.node;

import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.compat.ModCheck;
import me.alfie.immersiveenchanting.compat.ModCompat;
import me.alfie.immersiveenchanting.config.ServerConfig;
import me.alfie.immersiveenchanting.datacomponent.ReplicatedDataComponent;
import me.alfie.immersiveenchanting.datapack.EnchantmentCostRegistry;
import me.alfie.immersiveenchanting.gui.EnchantingTableScreen;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.enchanting.EnchantingNodeBranch;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.replicate.ReplicateNodeBranch;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.transmute.TransmuteNodeBranch;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class BranchFactory {

    /**
     * Finds which enchantments are applicable for an item stack , generates branch angles, and creates the branches.
     *
     * @param stack
     */
    public static List<NodeBranch> buildEnchantingNodeBranches(ItemStack stack, EnchantingTableScreen screen) {
        List<NodeBranch> branches = new ArrayList<>();

        //Step 1. Find which enchantments are applicable.
        List<Holder<Enchantment>> applicableEnchantments = new ArrayList<>();
        Set<Holder<Enchantment>> unlockedEnchantments = screen.getMenu().getUnlockedEnchantments();
        List<Holder.Reference<Enchantment>> allEnchantments = EnchantmentUtil.getAllEnchantments(screen.player.level());

        List<Holder<Enchantment>> allEnchantmentsSorted = new ArrayList<>(allEnchantments);
        allEnchantmentsSorted.sort(Comparator.comparing(
                holder -> holder.getKey().location().toString()));

        //Iterate through the enchantment registry, see if the item support the enchantment.
        for (Holder<Enchantment> enchantmentHolder : allEnchantmentsSorted) {
            Set<Holder<Enchantment>> itemEnchantments = new HashSet<>(stack.getTagEnchantments().keySet());
            itemEnchantments.remove(enchantmentHolder); //Ignore the enchantment we're trying to check for
            if (!EnchantmentHelper.isEnchantmentCompatible(itemEnchantments, enchantmentHolder)) continue; //Skip if incompatible
            if (stack.getItem().supportsEnchantment(stack, enchantmentHolder)) applicableEnchantments.add(enchantmentHolder);
        }

        List<Float> angles = generateBranchAngles(applicableEnchantments.size());

        int i = 0;
        for (Holder<Enchantment> enchantmentHolder : applicableEnchantments) {
            boolean isUnlocked = unlockedEnchantments.contains(enchantmentHolder);
            AtomicInteger enchantmentLevel = new AtomicInteger();
            enchantmentLevel.set(stack.getItem().getEnchantmentLevel(stack, enchantmentHolder));

            if(ModCheck.Mod.RELIQUARY.isLoaded()) ModCompat.reliquaryMagicbaneFix(stack, enchantmentHolder, enchantmentLevel);

            branches.add(new EnchantingNodeBranch(
                    screen,
                    angles.get(i),
                    enchantmentHolder,
                    enchantmentLevel.get(),
                    isUnlocked,
                    screen.player));
            i++;
        }
        return branches;
    }

    /**
     * Special method to build the upgrade node for ancient book enchantment re-roll.
     */
    public static List<NodeBranch> buildAncientBookBranch(ItemStack currentItemStack, EnchantingTableScreen screen) {
        List<NodeBranch> branches = new ArrayList<>();

        Set<Holder<Enchantment>> unlockedEnchantments = screen.getMenu().getUnlockedEnchantments();
        if(!currentItemStack.has(DataComponents.STORED_ENCHANTMENTS)) return branches;
        Set<Holder<Enchantment>> ancientBookEnchantment = currentItemStack.get(DataComponents.STORED_ENCHANTMENTS).keySet();

        //Check if the enchantment stored in this ancient book is also unlocked (in the bookshelf)
        boolean canTransmute = !Collections.disjoint(unlockedEnchantments, ancientBookEnchantment);
        boolean isBookReplicated = ReplicatedDataComponent.isReplicated(currentItemStack);

        List<Float> angles = generateBranchAngles(2);

        if(ServerConfig.isAllowTransmute()) {
            branches.add(new TransmuteNodeBranch(
                    screen,
                    angles.get(0),
                    canTransmute,
                    isBookReplicated));
        }

        if(ServerConfig.isAllowReplicate()) {
            branches.add(new ReplicateNodeBranch(
                    screen,
                    angles.get(1)));
        }

        return branches;
    }


    /**
     * Generate a list of angles based on the total number of branches.
     *
     * @param totalBranches
     * @return
     */
    public static ArrayList<Float> generateBranchAngles(int totalBranches) {
        // No more than 16 branches
        ArrayList<Float> angles = new ArrayList<>();
        for (int i = 0; i < totalBranches; i++) {
            float angle = (float) (i * 2 * Math.PI / totalBranches); // evenly spaced
            angles.add(angle);
        }
        return angles;
    }
}
