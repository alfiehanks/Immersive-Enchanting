package me.alfie.immersiveenchanting.compat.ench_desc;

import me.alfie.alfinolib.gui.GuiGraphicsX;
import me.alfie.alfinolib.gui.util.MousePos;
import me.alfie.immersiveenchanting.api.description.DescriptionHelper;
import me.alfie.immersiveenchanting.api.description.DescriptionLayout;
import me.alfie.immersiveenchanting.api.description.DescriptionLayoutExtension;
import me.alfie.immersiveenchanting.api.description.DescriptionLine;
import me.alfie.immersiveenchanting.api.node.NodeData;
import me.alfie.immersiveenchanting.api.node.NodePayload;
import me.alfie.immersiveenchanting.api.node.internal.EnchantmentNodeData;
import me.alfie.immersiveenchanting.gui.tab.enchanting.tooltip.NodeTooltip;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchDescLayoutExtension implements DescriptionLayoutExtension {

    @SuppressWarnings("unchecked")
    @Override
    public void extendLayout(DescriptionLayout description, NodeTooltip tooltip) {
        if(!tooltip.node().isDataType(EnchantmentNodeData.TYPE)) return;
        if(EnchDescCompat.config().only_on_books) return;

        NodeData<EnchantmentNodeData> nodeData = (NodeData<EnchantmentNodeData>) tooltip.node().data();

        Holder<Enchantment> enchantmentHolder = EnchantmentUtil.toHolder(nodeData.value().enchantmentId(), tooltip.screen().registryAccess());
        Component component = EnchDescCompat.getDescription(enchantmentHolder, nodeData.value().level())
                .withColor(0xD6D4EA);

        DescriptionHelper.lineWrapComponent(component, DescriptionHelper.DEFAULT_LINE_WIDTH, description, 0);
    }
}
