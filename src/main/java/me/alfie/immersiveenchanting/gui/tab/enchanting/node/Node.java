package me.alfie.immersiveenchanting.gui.tab.enchanting.node;

import me.alfie.alfinolib.gui.GuiGraphicsX;
import me.alfie.alfinolib.gui.util.MousePos;
import me.alfie.alfinolib.util.ResourceId;
import me.alfie.immersiveenchanting.api.node.*;
import me.alfie.immersiveenchanting.api.node.internal.EnchantmentNodeData;
import me.alfie.immersiveenchanting.config.ServerConfig;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.CostRegistry;
import me.alfie.immersiveenchanting.gui.canvas.Canvas;
import me.alfie.immersiveenchanting.gui.canvas.CanvasRenderable;
import me.alfie.immersiveenchanting.gui.core.Sprite;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

import javax.annotation.Nullable;

/**
 * Represents a single node in the enchanting tree UI.
 *
 * <p>A {@code Node} is a renderable element within the {@link Canvas} that can represent
 * either a real enchantment or a special non-enchantment action (e.g. transmute, replicate).
 * Each node has a {@link NodeState} (e.g. locked, available) and a {@link NodeTier}
 * which determines its visual appearance.</p>
 *
 * <p>Nodes are rendered in canvas space and are responsible for:
 * <ul>
 *     <li>Displaying their background sprite based on state and tier</li>
 *     <li>Displaying an optional icon</li>
 *     <li>Triggering tooltip rendering when hovered</li>
 * </ul>
 * </p>
 *
 * <p>Actual interaction (e.g. clicking) is typically handled at the screen level.</p>
 */
public class Node extends CanvasRenderable {

    public static final int WIDTH = 26;
    public static final int HEIGHT = 26;
    public static final float DEFAULT_SCALE = 0.8f;

    private final NodeState state;
    private final NodeTier tier;

    @Nullable
    private NodeIcon icon;
    private final Component title;

    private final NodeBranch parentBranch;
    private final int position;
    private final NodeData<?> data;

     /**
     * Constructs a node that represents a real enchantment with a specific level.
     *
     * <p>This constructor should be used for standard enchantment nodes that
     * correspond to a valid enchantment ID and level.</p>
     *
     * @param position The level of the enchantment (must be > 0)
     * @param canvas The canvas this node belongs to
     * @param state The current state of the node (e.g. locked, unlocked)
     * @param tier The visual tier of the node
     * @param parentBranch The branch this node belongs to
     */
    public Node(Component title,
                int position,
                Canvas canvas,
                NodeState state,
                NodeTier tier,
                @Nullable NodeIcon icon,
                NodeBranch parentBranch,
                NodeData<?> data) {
        super(canvas);

        if(position < 0) throw new IllegalStateException("Node position can't be less than 0!");
        this.position = position;
        this.state = state;
        this.tier = tier;
        this.parentBranch = parentBranch;
        this.title = title;
        this.data = data;

        setIcon(icon);
    }

    public void click() {
        data.onClick(new NodeClickContext(canvas().screen(), this));
    }

    public ResourceId dataType() {
        return data.type();
    }

    public NodeData<?> data() {
        return data;
    }

    public boolean isDataType(ResourceId id) {
        return data.type() == id;
    }

    /**
     * Returns {@code true} if this node's enchantment can currently be removed.
     * Removal is only permitted when the node's position matches the highest level on the item
     * (i.e. it is the top-most equipped level) and removal is enabled in server config.
     */
    public boolean canRemove() {
        return getPosition() + 1 == EnchantmentUtil.getEnchantmentLevel(
                canvas().screen()
                        .getMenu()
                        .getToolSlot()
                        .getItem(),
                EnchantmentUtil.toHolder(branchId(), canvas().screen().registryAccess()))
                && ServerConfig.isEnchantmentRemovalAllowed();
    }

    public NodeBranch getParentBranch() {
        return parentBranch;
    }


    @Override
    public void render(GuiGraphicsX gx, MousePos mousePos) {
        setScaleKeepPos(BranchManager.getNodeBranchScale(), state.getSpriteForTier(tier));

        if(canvas().isMouseOver(canvasX(), canvasY(), Node.WIDTH, Node.HEIGHT, mousePos)) {
            if(!canvas().screen().camera().isDragging()) {
                int priority = canvas().screen()
                        .enchantingTab()
                        .branchManager()
                        .getAllNodes()
                        .indexOf(this);

                canvas().screen().tooltipManager().requestTooltip(this, priority);
            }
        }


        if(canvas().screen().tooltipManager().isActiveTooltipFor(this)) return;
        blit(gx, state.getSpriteForTier(tier), canvas().getCurrentBrightness());

        if(data().value() instanceof EnchantmentNodeData enchantmentData) {
            Holder<Enchantment> enchantmentHolder = EnchantmentUtil.toHolder(enchantmentData.enchantmentId(), canvas().screen().registryAccess());

            if(!CostRegistry.client().isRegistered(enchantmentHolder)) {
                //Red error node for enchantments that failed to load costs
                blit(gx, Sprite.ERROR_NODE, canvas().getCurrentBrightness());
            }

        }


        if(getIcon() instanceof SpriteIcon sprite) {
            blit(gx, sprite.id(), 16, 16, 4, 4, canvas().getCurrentBrightness());
        } else if(getIcon() instanceof ItemIcon item) {
            item(gx, item.stack(), 4, 4, canvas().getCurrentBrightness());
        }
    }

    /**
     * Determines and assigns the icon texture for this node.
     *
     * <p>Defaults to an "ancient book" texture unless the node is in a locked state,
     * in which case no icon is rendered.</p>
     */
    private void setIcon(NodeIcon icon) {
        this.icon = icon;

        if(isState(NodeState.LOCKED) || isState(NodeState.ALERT)) this.icon = null;
    }

    /**
     * @return The visual tier of this node
     */
    public NodeTier getTier() {
        return tier;
    }

    /**
     * @return The current state of this node
     */
    public NodeState getState() {
        return state;
    }

    /**
     * @return The icon texture
     */
    public @Nullable NodeIcon getIcon() {
        return icon;
    }

    /**
     * @return The identifier associated with this node
     */
    public ResourceId branchId() {
        return getParentBranch().id();
    }

    /**
     * Gets the localized display title for this node.
     *
     * <p>For enchantment nodes, this includes the enchantment name and level.
     * For non-enchantment nodes, this resolves based on the node ID.</p>
     *
     * @return A {@link Component} representing the node title
     */
    public Component getTitle() {
        return title;
    }

    /**
     * Gets the enchantment level of this node.
     *
     * @return The enchantment level
     * @throws IllegalStateException if this node does not represent an enchantment
     */
    public int getPosition() {
        return position;
    }

    /**
     * Checks if this node is in a given state.
     *
     * @param state The state to compare against
     * @return {@code true} if the node is in the specified state
     */
    public boolean isState(NodeState state) {
        return this.state == state;
    }
}
