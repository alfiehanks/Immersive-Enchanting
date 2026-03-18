package me.alfie.immersiveenchanting.gui.core.tab.enchanting;

import me.alfie.immersiveenchanting.config.ServerConfig;
import me.alfie.immersiveenchanting.datapack.EnchantmentCostRegistry;
import me.alfie.immersiveenchanting.datapack.cost.EnchantmentCost;
import me.alfie.immersiveenchanting.gui.EnchantingTableScreen;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.BranchFactory;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.Node;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.NodeBranch;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.NodeTooltip;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.enchanting.EnchantingNode;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.enchanting.EnchantingNodeTooltip;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.replicate.ReplicateNode;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.replicate.ReplicateNodeTooltip;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.transmute.TransmuteNode;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.transmute.TransmuteNodeTooltip;
import me.alfie.immersiveenchanting.item.ModItems;
import me.alfie.immersiveenchanting.networking.packet.removeenchantment.RemoveEnchantmentPacket;
import me.alfie.immersiveenchanting.util.FxHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EnchantingTab {

    public final EnchantingTableScreen screen;
    public List<NodeBranch> branches = new ArrayList<>();

    private boolean lockHover;
    private float brightness;
    private Node heldNode;
    private Node hoveredNode;
    private Node lastHoveredNode;
    private NodeTooltip nodeTooltip;
    private final List<Node> rendered_nodes = new ArrayList<>();
    private ItemStack lastStack = ItemStack.EMPTY;

    public final long HOLD_THRESHOLD = 1000;

    public final CentralSlot centralSlot;

    public EnchantingTab(EnchantingTableScreen screen) {
        this.screen = screen;
        this.centralSlot = new CentralSlot(screen);
    }

    public void init() {
        screen.getCanvas().calculateSize();

        //Prevent tearing
        branches.clear();
        rendered_nodes.clear();
    }

    /**
     * Call when tool slot changes. Refresh the screen.
     */
    public void onToolSlotUpdate() {
        FxHelper.playEnchantingTableToolSlotSound(screen.player);

        //Clear all nodes
        branches.clear();
        rendered_nodes.clear();
        setLockHover(false);

        if (screen.getMenu().isToolSlotEmpty()) {
            init();
            screen.getCanvas().setDraggingEnabled(false);
        } else screen.getCanvas().setDraggingEnabled(true);

        if(screen.getMenu().getToolSlotItem().is(ModItems.ANCIENT_BOOK.get()))
            branches = BranchFactory.buildAncientBookBranch(screen.getMenu().getToolSlotItem(), screen);
        else
            branches = BranchFactory.buildEnchantingNodeBranches(screen.getMenu().getToolSlotItem(), screen);

        NodeBranch.calculateNodeAnglesAndStep(screen);

        //Place nodes after size change
        for (NodeBranch branch : branches) {
            branch.placeNodesAlongLine();
            for (Node node : branch.getNodes()) {
                node.setScale(EnchantingNode.globalScale);
            }
        }
    }

    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        //Fade the background and nodes darker if hovering
        float targetBrightness = isHoveringAnyNode() ? 0.15f : 1f;
        float fadeSpeed = 0.3f;
        brightness += (targetBrightness - brightness) * fadeSpeed;
        guiGraphics.setColor(brightness, brightness, brightness, 1f);

        //Darken background if tool slot is empty
        if (screen.getMenu().isToolSlotEmpty()) guiGraphics.setColor(0.5F, 0.5F, 0.5F, 1f);
        screen.getCanvas().renderTiledBg(guiGraphics);

        renderBranchConnections(guiGraphics);
        centralSlot.render(guiGraphics, mouseX, mouseY);
        renderNodes(guiGraphics);

        Node renderedNode;
        if (isLockHover()) renderedNode = lastHoveredNode;
        else {
            setHoveredNode(mouseX, mouseY);
            renderedNode = getHoveredNode();
        }

        if (renderedNode != null) {
            renderNodeTooltip(renderedNode, guiGraphics);
            renderHoveredNode(renderedNode, guiGraphics);
        } else nodeTooltip = null;

        if(heldNode != null) {
            screen.mouseHeld();
        }
    }

    private void renderBranchConnections(GuiGraphics guiGraphics) {
        for (NodeBranch branch : branches) {
            guiGraphics.setColor(brightness, brightness, brightness, 1f);

            //Don't darken the hovered node branch.
            if (branch.getNodes().contains(hoveredNode)) guiGraphics.setColor(1f, 1f, 1f, 1f);
            if (!branch.hasCalculatedConnections()) branch.calculateNodeConnections();

            branch.renderPrecomputedConnection(guiGraphics,
                    (int) screen.getCanvas().getScrollX(),
                    (int) screen.getCanvas().getScrollY());

            guiGraphics.setColor(1f, 1f, 1f, 1f);
        }
    }

    /**
     * Render nodes (excluding the current hovered node).
     *
     * @param guiGraphics
     */
    private void renderNodes(GuiGraphics guiGraphics) {
        for (Node node : rendered_nodes) {
            guiGraphics.setColor(brightness, brightness, brightness, 1f);
            node.render(guiGraphics, screen.getCanvas());

            node.setScale(EnchantingNode.globalScale);

            //Reset color after darkening
            guiGraphics.setColor(1f, 1f, 1f, 1f);
        }
    }

    private boolean isHoveringAnyNode() {
        return hoveredNode != null;
    }

    public void setLockHover(boolean lockHover) {
        this.lockHover = lockHover;
    }

    public boolean isLockHover() {
        return lockHover;
    }

    private void setHoveredNode(double mouseX, double mouseY) {
        for (Node node : rendered_nodes) {
            if (node.isMouseOver(screen.getCanvas(), mouseX, mouseY)) {
                hoveredNode = node;
                return;
            }
        }
        hoveredNode = null;
        heldNode = null;
    }

    public List<Node> getRenderedNodes() {
        return rendered_nodes;
    }

    public Node getHoveredNode() {
        return hoveredNode;
    }

    private void renderNodeTooltip(Node node, GuiGraphics guiGraphics) {
        if (!node.equals(lastHoveredNode) || nodeTooltip == null) {
            switch (node) {
                case EnchantingNode enchantingNode -> nodeTooltip = new EnchantingNodeTooltip(
                        enchantingNode,
                        screen,
                        EnchantmentCost.getRenderableAnyOfCosts(
                                EnchantmentCostRegistry.getClientRegistry()
                                        .getEnchantmentCost(enchantingNode.getEnchantment())
                                        .getCostForLevel(enchantingNode.getEnchantmentLevel())
                        ));
                case TransmuteNode transmuteNode -> nodeTooltip = new TransmuteNodeTooltip(
                        transmuteNode,
                        screen,
                        EnchantmentCost.getRenderableAnyOfCosts(
                                EnchantmentCostRegistry.getClientRegistry()
                                        .getInternalRegistry()
                                        .get(EnchantmentCostRegistry.InternalCosts.TRANSMUTE)
                                        .getCostForLevel(1)));
                case ReplicateNode replicateNode -> nodeTooltip = new ReplicateNodeTooltip(
                        replicateNode,
                        screen,
                        EnchantmentCost.getRenderableAnyOfCosts(
                                EnchantmentCostRegistry.getClientRegistry()
                                        .getInternalRegistry()
                                        .get(EnchantmentCostRegistry.InternalCosts.REPLICATE)
                                        .getCostForLevel(1)));
                default -> {
                }
            }

            FxHelper.playNodeHoverSound(node, screen.player);
            lastHoveredNode = node;
        }

        //Render it
        if (nodeTooltip != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);
            nodeTooltip.render(guiGraphics);
            guiGraphics.pose().popPose();
        }

    }

    public void renderTooltipItemStackCost(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if(nodeTooltip == null) return;
        //Cost stack
        if (screen.isMouseOver(
                mouseX, mouseY,
                nodeTooltip.getCostStackPos().x, nodeTooltip.getCostStackPos().y,
                16, 16)) {

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 500);

            List<Component> lines = nodeTooltip.getCurrentRenderedCost().
                    asItemStack()
                    .getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.ADVANCED);

            if(!(Objects.equals(nodeTooltip.costDescriptionComponents.getFirst(), Component.empty()))) {
                lines.add(1, nodeTooltip.costDescriptionComponents.getFirst());
            }

            guiGraphics.renderTooltip(Minecraft.getInstance().font,
                    lines,
                    nodeTooltip.getCurrentRenderedCost().asItemStack().getTooltipImage(),
                    nodeTooltip.getCurrentRenderedCost().asItemStack(),
                    mouseX,
                    mouseY);

            guiGraphics.pose().popPose();
        }

        //Fuel stack
        if(nodeTooltip instanceof EnchantingNodeTooltip enchantingNodeTooltip) {
            if (screen.isMouseOver(
                    mouseX, mouseY,
                    enchantingNodeTooltip.getFuelStackPos().x, enchantingNodeTooltip.getFuelStackPos().y,
                    16, 16)) {

                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 500);

                List<Component> lines = enchantingNodeTooltip.getCurrentRenderedFuel().
                        asItemStack()
                        .getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.ADVANCED);

                if(!(Objects.equals(enchantingNodeTooltip.fuelDescriptionComponents.getFirst(), Component.empty()))) {
                    lines.add(1, enchantingNodeTooltip.fuelDescriptionComponents.getFirst());
                }

                guiGraphics.renderTooltip(Minecraft.getInstance().font,
                        lines,
                        enchantingNodeTooltip.getCurrentRenderedFuel().asItemStack().getTooltipImage(),
                        enchantingNodeTooltip.getCurrentRenderedFuel().asItemStack(),
                        mouseX,
                        mouseY);

                guiGraphics.pose().popPose();
            }
        }

    }

    private void renderHoveredNode(Node node, GuiGraphics guiGraphics) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500);
        node.setScale(1f);
        node.render(guiGraphics, screen.getCanvas());
        guiGraphics.pose().popPose();
    }

    public void checkToolSlotUpdated() {
        ItemStack item = screen.getMenu().getToolSlotItem();
        if (!ItemStack.isSameItemSameComponents(item, lastStack)) {
            lastStack = item.copy();
            onToolSlotUpdate();
        }
    }

    public void setHeldNode(Node node) {
        this.heldNode = node;
    }

    public long getNodeHeldTime() {
        if(heldNode == null) return 0;
        return System.currentTimeMillis() - screen.getHoldStartTime();
    }

    public void onNodeHeld() {
        long heldTime = getNodeHeldTime();

        if(heldNode instanceof EnchantingNode enchantingNode && heldTime > HOLD_THRESHOLD && ServerConfig.isEnchantmentRemovalAllowed()) {
            int nodeEnchantmentLevel = enchantingNode.getEnchantmentLevel();
            int highestUnlockedLevel = screen.getMenu().getToolSlotItem().getEnchantmentLevel(enchantingNode.getEnchantmentHolder());

            if (nodeEnchantmentLevel == highestUnlockedLevel) {
                heldNode = null;
                PacketDistributor.sendToServer(new RemoveEnchantmentPacket(
                        enchantingNode.getEnchantmentHolder().getKey(),
                        enchantingNode.getEnchantmentLevel())
                );
            }
        }
    }

    public boolean onRightClick(int mouseX, int mouseY) {
        if (isHoveringAnyNode()) {
            setLockHover(!isLockHover());
            if (isLockHover()) FxHelper.playTooltipLockSound(screen.player);
            return true;
        }
        return false;
    }
}
