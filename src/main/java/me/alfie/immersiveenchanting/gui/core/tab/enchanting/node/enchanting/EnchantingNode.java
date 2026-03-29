package me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.enchanting;

import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.Node;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.NodeType;
import me.alfie.immersiveenchanting.networking.ModPackets;
import me.alfie.immersiveenchanting.networking.packet.enchantitem.EnchantItemPacket;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantingNode extends Node {

    public static float globalScale = 1.0f;
    public final boolean isBranchUnlocked;


    private final int enchantmentLevel;
    private final ResourceKey<Enchantment> enchantment;
    private final Holder<Enchantment> enchantmentHolder;
    private final ResourceLocation LOCKED_ICON = new ResourceLocation("immersiveenchanting", "textures/gui/enchantment_icons/locked_enchantment.png");
    public final EnchantingNodeBranch branch;

    public EnchantingNode(NodeType nodeType, ResourceLocation iconTexture,
                          int enchantmentLevel,
                          Holder<Enchantment> enchantmentHolder,
                          boolean isBranchUnlocked,
                          EnchantingNodeBranch branch) {
        super(nodeType, iconTexture);

        this.enchantmentLevel = enchantmentLevel;
        this.enchantmentHolder = enchantmentHolder;
        this.isBranchUnlocked = isBranchUnlocked;
        this.branch = branch;

        this.enchantment = enchantmentHolder.unwrapKey().get();

        if (!isBranchUnlocked) {
            setNodeType(NodeType.LOCKED);
            setIconTexture(null);
        }
    }


    public int getEnchantmentLevel() {
        return enchantmentLevel;
    }

    public Holder<Enchantment> getEnchantmentHolder() {
        return enchantmentHolder;
    }

    public ResourceKey<Enchantment> getEnchantment() {
        return enchantment;
    }

    @Override
    public boolean onClicked(int mouseX, int mouseY) {
        if(isBranchUnlocked) {
            if(!isObtained()) {
                ModPackets.INSTANCE.sendToServer(new EnchantItemPacket(
                        getEnchantmentHolder().unwrapKey().get(),
                        getEnchantmentLevel()
                ));
            } else {
                branch.screen.enchantingTab.setHeldNode(this);
                branch.screen.setHoldStartTime(System.currentTimeMillis());
            }
            return true;
        }
        return false;
    }
}
