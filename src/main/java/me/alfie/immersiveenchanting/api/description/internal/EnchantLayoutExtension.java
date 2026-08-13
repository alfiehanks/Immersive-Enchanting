package me.alfie.immersiveenchanting.api.description.internal;

import me.alfie.immersiveenchanting.api.description.DescriptionHelper;
import me.alfie.immersiveenchanting.api.description.DescriptionLayout;
import me.alfie.immersiveenchanting.api.description.DescriptionLayoutExtension;
import me.alfie.immersiveenchanting.api.description.internal.lines.*;
import me.alfie.immersiveenchanting.api.node.internal.EnchantmentNodeData;
import me.alfie.immersiveenchanting.gui.EnchantingTableScreen;
import me.alfie.immersiveenchanting.gui.tab.enchanting.node.Node;
import me.alfie.immersiveenchanting.gui.tab.enchanting.node.NodeState;
import me.alfie.immersiveenchanting.gui.tab.enchanting.tooltip.NodeTooltip;

/**
 * Populates the tooltip description for enchantment nodes based on their current state:
 * <ul>
 *   <li>UNOBTAINED – shows the enchantment cost (materials, fuel, XP levels)</li>
 *   <li>OBTAINED – shows "Equipped" or, while the remove key is held, a removal progress bar</li>
 *   <li>LOCKED – shows "Unavailable Enchantment"</li>
 * </ul>
 * No-ops for non-enchantment nodes.
 */
public class EnchantLayoutExtension implements DescriptionLayoutExtension {

    @Override
    public void extendLayout(DescriptionLayout description, NodeTooltip tooltip) {
        if(!tooltip.node().isDataType(EnchantmentNodeData.TYPE)) return;

        description.widthPadding = 16;

        Node node = tooltip.node();
        EnchantingTableScreen screen = tooltip.screen();

        if(node.isState(NodeState.UNOBTAINED)) {
            DescriptionHelper.insertCostLines(tooltip, description, 0);
        } else if(node.isState(NodeState.OBTAINED)) {
            if(screen.tooltipManager().isHoldingTooltip()) {
                description.insertLine(0, new RemovingLine(tooltip));
                description.insertLine(1, new RemoveProgressLine(tooltip));
            } else {
                description.insertLine(0, new EquippedLine(tooltip));

                if(tooltip.node().canRemove()) {
                    description.insertLine(1, new RemoveHintLine(tooltip));
                }

            }
        } else if(node.isState(NodeState.LOCKED)) {
            description.insertLine(0, new UnavailableEnchantmentLine(tooltip));
        }
    }
}
