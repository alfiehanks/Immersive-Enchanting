package me.alfie.immersiveenchanting.api.node.internal;

import me.alfie.alfinolib.networking.Networking;
import me.alfie.alfinolib.util.ResourceId;
import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.api.node.NodeData;
import me.alfie.immersiveenchanting.api.node.NodePayload;
import me.alfie.immersiveenchanting.gui.EnchantingTableScreen;
import me.alfie.immersiveenchanting.gui.tab.enchanting.node.Node;
import me.alfie.immersiveenchanting.gui.tab.enchanting.node.NodeState;
import me.alfie.immersiveenchanting.networking.EnchantPacket;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

public record EnchantmentNodeData(ResourceId enchantmentId, int level) implements NodePayload {

    public static final ResourceId TYPE = new ResourceId(ImmersiveEnchanting.MODID, "enchantment");
    @Override public ResourceId type() {
        return TYPE;
    }

    /**
     * Creates a {@link NodeData} for an enchantment node.
     *
     * <p>On click:
     * <ul>
     *   <li>If the node is OBTAINED and {@link me.alfie.immersiveenchanting.gui.tab.enchanting.node.Node#canRemove()}
     *       is true, starts the hold-to-remove gesture.</li>
     *   <li>Otherwise sends an {@link EnchantPacket} to apply the enchantment at the given level.</li>
     * </ul>
     */
    public static NodeData<EnchantmentNodeData> create(ResourceId enchantmentId, int level) {
        return new NodeData<>(
                TYPE,
                new EnchantmentNodeData(enchantmentId, level),
                (data, context) -> {
                    Node node = context.node();
                    EnchantingTableScreen screen = context.screen();

                    if (node.isState(NodeState.OBTAINED) && node.canRemove()) {
                        screen.tooltipManager().startHold(node);
                    } else {
                        Holder<Enchantment> enchantmentHolder = EnchantmentUtil.toHolder(data.enchantmentId(), screen.registryAccess());
                        Networking.sendToServer(new EnchantPacket(enchantmentHolder.getKey(), data.level()));
                    }

                }
        );
    }

}
