package me.alfie.immersiveenchanting.gui.tab.enchanting.node;

import me.alfie.alfinolib.datapacks.client.ClientDatapackManager;
import me.alfie.alfinolib.util.ResourceId;
import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.api.node.*;
import me.alfie.immersiveenchanting.api.node.internal.EnchantmentNodeData;
import me.alfie.immersiveenchanting.api.node.internal.ModFilterNodeData;
import me.alfie.immersiveenchanting.api.node.internal.ReplicateNodeData;
import me.alfie.immersiveenchanting.api.node.internal.TransmuteNodeData;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.CostRegistry;
import me.alfie.immersiveenchanting.datapack.mod_icons.ModIconsDatapack;
import me.alfie.immersiveenchanting.gui.canvas.Canvas;
import me.alfie.immersiveenchanting.gui.tab.enchanting.EnchantingTab;
import me.alfie.immersiveenchanting.item.ModItems;
import me.alfie.immersiveenchanting.util.EnchantmentTextureHelper;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.common.NeoForge;

import java.util.*;

/**
 * Functions to create branches pre-filled with nodes.
 */
public class BranchFactory {

    public static List<NodeBranch> buildBranches(ItemStack stack, CostRegistry costRegistry, Canvas canvas) {
        BuildBranchesEvent event = new BuildBranchesEvent(stack, costRegistry, canvas);

        addDefaultBranches(event);

        NeoForge.EVENT_BUS.post(event);

        List<NodeBranch> branches = event.getBranches();
        List<Float> angles = generateBranchAngles(branches.size());

        for (int i = 0; i < branches.size(); i++) {
            branches.get(i).setAngle(angles.get(i));
        }

        return branches;
    }

    private static void addDefaultBranches(BuildBranchesEvent event) {
        if(event.getStack().is(ModItems.ANCIENT_BOOK.get())) {
            buildAncientBookBranches(event);
        } else {
            buildEnchantingBranches(event);
        }
    }

    private static void buildAncientBookBranches(BuildBranchesEvent event) {
        buildTransmuteBranch(event);
        buildReplicateBranch(event);
    }

    private static void buildEnchantingBranches(BuildBranchesEvent event) {
        List<Holder<Enchantment>> allEnchantments = EnchantmentUtil.getAllEnchantmentsInRegistry(event.getCanvas().screen().registryAccess());
        List<Holder<Enchantment>> applicableEnchantments = getApplicableEnchantments(event.getStack(), allEnchantments);

        if(event.getStack().is(ModItems.CREATIVE_BOOKSHELF_ITEM) || event.getStack().is(Items.BOOK)) {
            applicableEnchantments = allEnchantments;
        }

        if(event.getCanvas().screen().enchantingTab().isDisplay(EnchantingTab.Display.ENCHANTMENTS)) {

            for (Holder<Enchantment> applicableEnchantment : applicableEnchantments) {
                String modid = applicableEnchantment.getKey().location().getNamespace();
                String filteredModid = event.getCanvas().screen().enchantingTab().getFilteredModid();
                if(filteredModid == null || filteredModid.equals(modid)) {
                    if(!event.costRegistry().isRegistered(applicableEnchantment)
                        || event.costRegistry().get(applicableEnchantment).enabled()) {

                        buildEnchantingBranch(event, applicableEnchantment);
                    }
                }
            }

        } else if(event.getCanvas().screen().enchantingTab().isDisplay(EnchantingTab.Display.MOD_FILTERS)) {
            buildModFilterBranches(event, applicableEnchantments);
        }

    }

    private static ResourceId createModFilterBranchId(String modid) {
        final String modFilterStem = "mod_filter/";
        return new ResourceId(ImmersiveEnchanting.MODID, modFilterStem + modid);
    }

    private static void buildModFilterBranches(BuildBranchesEvent event, List<Holder<Enchantment>> applicableEnchantments) {
        Set<String> modids = new HashSet<>();

        for(Holder<Enchantment> enchantmentHolder : applicableEnchantments) {
            String modid = enchantmentHolder.getKey().location().getNamespace();
            if(modids.contains(modid)) continue;
            modids.add(modid);


            Component title = Component.literal(ImmersiveEnchanting.getModName(modid));
            buildModFilterBranch(event, title, modid);
        }

        ItemIcon itemIcon = new ItemIcon(ClientDatapackManager.get(ModIconsDatapack.KEY)
                .getAsItemStack(ModFilterNodeData.ALL_MODS));

        NodeState state =
                Objects.equals(event.getCanvas().screen().enchantingTab().getFilteredModid(), null)
                        ? NodeState.OBTAINED
                        : NodeState.UNOBTAINED;

        event.addBranch(BranchBuilder.of(event.getCanvas(), createModFilterBranchId(ModFilterNodeData.ALL_MODS))
                .node(new NodeTemplate(
                        Component.translatable("immersiveenchanting.mod_filter.all"),
                        0,
                        state,
                        NodeTier.ELITE,
                        itemIcon,
                        ModFilterNodeData.create(ModFilterNodeData.ALL_MODS)
                )).build());
    }



    private static void buildModFilterBranch(BuildBranchesEvent event, Component modTitle, String modid) {
        ItemIcon itemIcon = new ItemIcon(ClientDatapackManager.get(ModIconsDatapack.KEY)
                .getAsItemStack(modid));

        NodeState state =
                Objects.equals(event.getCanvas().screen().enchantingTab().getFilteredModid(), modid)
                        ? NodeState.OBTAINED
                        : NodeState.UNOBTAINED;

        event.addBranch(BranchBuilder.of(event.getCanvas(), createModFilterBranchId(modid))
                .node(new NodeTemplate(
                        modTitle,
                        0,
                        state,
                        NodeTier.BASIC,
                        itemIcon,
                        ModFilterNodeData.create(modid)
                )).build());
    }

    private static void buildEnchantingBranch(BuildBranchesEvent event, Holder<Enchantment> enchantmentHolder) {
        List<NodeTemplate> nodeTemplates = new ArrayList<>();

        int maxLevel;
        if(event.costRegistry().isRegistered(enchantmentHolder)) {
            //Use the max level from the cost registry, which may be higher than the enchantment's inherent max level if the datapack adds extra levels.
            maxLevel = event.costRegistry().get(enchantmentHolder).levelCosts().maxLevel();
        } else {
            //Use the max level from the enchantment itself as a fallback if it's not in the cost registry. This allows enchantments added by datapacks to still show up.
            maxLevel = enchantmentHolder.value().getMaxLevel();
        }

        for (int enchantmentLevel = 0; enchantmentLevel < maxLevel; enchantmentLevel++) {
            int equippedLevel = EnchantmentUtil.getEnchantmentLevel(event.getStack(), enchantmentHolder);
            NodeState state = equippedLevel > enchantmentLevel ? NodeState.OBTAINED : NodeState.UNOBTAINED;
            NodeTier tier = enchantmentLevel+1 == maxLevel ? NodeTier.ELITE : NodeTier.BASIC;

            if(!event.getCanvas().screen().getMenu().isEnchantmentAvailable(enchantmentHolder))
                state = NodeState.LOCKED;


            ResourceId enchantmentId = EnchantmentUtil.toId(enchantmentHolder);
            nodeTemplates.add(new NodeTemplate(
                    Enchantment.getFullname(enchantmentHolder, enchantmentLevel+1),
                    enchantmentLevel,
                    state,
                    tier,
                    new SpriteIcon(EnchantmentTextureHelper.getTexture(enchantmentId)),
                    EnchantmentNodeData.create(enchantmentId, enchantmentLevel+1)
            ));


            if(equippedLevel < enchantmentLevel+1) break;
        }

        event.addBranch(BranchBuilder.of(event.getCanvas(), EnchantmentUtil.toId(enchantmentHolder))
                .nodes(nodeTemplates)
                .build());
    }

    private static void buildTransmuteBranch(BuildBranchesEvent event) {
        NodeState state = event.getCanvas().screen().getMenu().isEnchantmentAvailable(EnchantmentUtil.getStoredEnchantment(event.getStack())) ?
                NodeState.UNOBTAINED : NodeState.LOCKED;
        if(EnchantmentUtil.isReplicated(event.getStack())) state = NodeState.ALERT;

        NodeTemplate transmuteNode = new NodeTemplate(
                Component.translatable("immersiveenchanting.tooltip.title.transmute"),
                0,
                state,
                NodeTier.ADVANCED,
                new SpriteIcon(EnchantmentTextureHelper.getTexture(CostRegistry.TRANSMUTE)),
                TransmuteNodeData.create()
        );

        if(!event.costRegistry().isRegistered(CostRegistry.TRANSMUTE)
            || event.costRegistry().get(CostRegistry.TRANSMUTE).enabled())
            event.addBranch(BranchBuilder.of(event.getCanvas(), CostRegistry.TRANSMUTE)
                    .node(transmuteNode)
                    .build());

    }

    private static void buildReplicateBranch(BuildBranchesEvent event) {
        NodeTemplate replicateNode = new NodeTemplate(
                Component.translatable("immersiveenchanting.tooltip.title.replicate"),
                0,
                NodeState.UNOBTAINED,
                NodeTier.ADVANCED,
                new SpriteIcon(EnchantmentTextureHelper.getTexture(CostRegistry.REPLICATE)),
                ReplicateNodeData.create()
        );

        if(!event.costRegistry().isRegistered(CostRegistry.REPLICATE)
            || event.costRegistry().get(CostRegistry.REPLICATE).enabled())
            event.addBranch(BranchBuilder.of(event.getCanvas(), CostRegistry.REPLICATE)
                    .node(replicateNode)
                    .build());
    }

    /**
     * Returns all enchantments from {@code allEnchantments} that are compatible with {@code stack}
     * (i.e. supported by the item and not conflicting with its other enchantments), sorted alphabetically.
     * @param stack The item stack for which to find applicable enchantments.
     * @param allEnchantments A list of enchantments to filter, typically all enchantments in the cost registry or all enchantments in the game.
     * @return A list of enchantments from {@code allEnchantments} that are compatible with {@code stack}, sorted alphabetically.
     */
    private static List<Holder<Enchantment>> getApplicableEnchantments(ItemStack stack, List<Holder<Enchantment>> allEnchantments) {
        List<Holder<Enchantment>> applicableEnchantments = new ArrayList<>();

        for(Holder<Enchantment> enchantment : allEnchantments) {
            Set<Holder<Enchantment>> itemEnchantments = new HashSet<>(stack.getTagEnchantments().keySet());
            itemEnchantments.remove(enchantment);
            if(!EnchantmentHelper.isEnchantmentCompatible(itemEnchantments, enchantment)) continue;
            if(stack.supportsEnchantment(enchantment)) applicableEnchantments.add(enchantment);
        }
        return applicableEnchantments;
    }

    /**
     * Generates evenly-spaced angles (in radians) for {@code totalBranches} branches,
     * spread uniformly around a full circle.
     */
    private static ArrayList<Float> generateBranchAngles(int totalBranches) {
        // No more than 16 branches
        ArrayList<Float> angles = new ArrayList<>();
        for (int i = 0; i < totalBranches; i++) {
            float angle = (float) (i * 2 * Math.PI / totalBranches); // evenly spaced
            angles.add(angle);
        }
        return angles;
    }
}
