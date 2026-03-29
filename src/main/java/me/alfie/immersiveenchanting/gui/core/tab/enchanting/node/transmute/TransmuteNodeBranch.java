package me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.transmute;

import me.alfie.immersiveenchanting.gui.EnchantingTableScreen;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.NodeBranch;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.NodeType;
import net.minecraft.resources.ResourceLocation;

public class TransmuteNodeBranch extends NodeBranch {

    public TransmuteNodeBranch(EnchantingTableScreen screen, float branchAngle,
                               boolean canTransmute,
                               boolean isBookReplicated) {
        super(screen, branchAngle);


        ResourceLocation iconTexture = new ResourceLocation("immersiveenchanting", "textures/item/ancient_book.png");

        NodeType nodeType = NodeType.ADVANCED;
        boolean nodeUnlocked = true;
        if(!canTransmute || isBookReplicated) {
            nodeUnlocked = false;
        }

        TransmuteNode node = new TransmuteNode(
                nodeType,
                iconTexture,
                nodeUnlocked,
                isBookReplicated);

        addNode(node);
    }
}
