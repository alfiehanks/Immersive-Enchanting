package me.alfie.immersiveenchanting.gui.core.tab.enchanting.node;

import me.alfie.immersiveenchanting.datapack.cost.CostEntry;
import me.alfie.immersiveenchanting.gui.EnchantingTableScreen;
import me.alfie.immersiveenchanting.gui.core.Sprite;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.tooltip.RenderDirection;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.tooltip.TooltipDescription;
import me.alfie.immersiveenchanting.gui.core.tab.enchanting.node.tooltip.TooltipTitle;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

public class NodeTooltip {
    RenderDirection renderDirection;
    public final Node node;
    public final EnchantingTableScreen screen;

    protected final TooltipTitle tooltipTitle;
    protected final TooltipDescription tooltipDescription;

    protected final int iconSize = 26;
    protected final int padding = 8;
    protected int titleBoxWidth;
    protected int titleBoxHeight;
    protected Vector2i titleBoxTopLeft = new Vector2i(0, 0);
    protected Vector2i descriptionBoxTopLeft = new Vector2i(0, 0);

    private String titleText;

    private final int costIconSize = 16;
    public List<CostEntry> validCosts;
    private CostEntry currentRenderedCost;
    private Vector2i costStackPos = new Vector2i(0, 0);
    public List<Component> costDescriptionComponents = new ArrayList<>() {{add(Component.empty());}};


    public NodeTooltip(Node node, EnchantingTableScreen screen, List<CostEntry> validCosts) {
        this.node = node;
        this.screen = screen;
        this.validCosts = validCosts;
        if(validCosts.isEmpty()) {
            validCosts.add(CostEntry.EMPTY);
        }

        //Create components
        tooltipTitle = new TooltipTitle(this, Sprite.TOOLTIP_UNOBTAINED.get());
        tooltipDescription = new TooltipDescription(this, Sprite.TOOLTIP_DESCRIPTION.get());
    }

    public Level getLevel() {
        return screen.player.level();
    }

    public void render(GuiGraphics graphics) {
        titleBoxHeight = Math.max(Minecraft.getInstance().font.lineHeight, Node.height);

        //Build description layout
        tooltipDescription.buildLayout();

        //Calculate width: Use title width, but if description extends it, go further.
        final int titleTextWidth = Minecraft.getInstance().font.width(tooltipTitle.getTitleText()) + Node.width + padding/2;
        final int descriptionTextWidth = Minecraft.getInstance().font.width(tooltipDescription.layout.getLongestString()) + padding;
        titleBoxWidth = Math.max(titleTextWidth, descriptionTextWidth+24); //Add a margin to prevent cost layout over expanding

        //Set sizes
        tooltipTitle.setBoxSize(titleBoxWidth, titleBoxHeight);
        tooltipDescription.setBoxSize(tooltipTitle.getBoxWidth(), tooltipDescription.layout.getRenderedHeight() + 20);
        setRenderDirection(); //Must call after setting sizes, but before setting positions!

        //Set positions
        tooltipTitle.setPos(titleBoxTopLeft.x+1, titleBoxTopLeft.y);
        tooltipDescription.setPos(descriptionBoxTopLeft.x, descriptionBoxTopLeft.y);

        //Title Settings
        ResourceLocation titleTexture = node.isObtained() ? Sprite.TOOLTIP_OBTAINED.get() : Sprite.TOOLTIP_UNOBTAINED.get();
        tooltipTitle.setSpriteTexture(titleTexture);

        //Draw
        tooltipDescription.draw(graphics);
        tooltipTitle.draw(graphics);
    }

    /**
     * If tooltip will render out of bounds, this function will set the render direction accordingly.
     * Automatically sets the correct positions for enchantmentNameBoxTopLeft and costBoxTopLeft.
     */
    protected void setRenderDirection() {
        boolean flipX = node.getViewportPosition(screen.getCanvas()).x + tooltipTitle.getBoxWidth() > screen.getCanvas().VIEWPORT_WIDTH;
        boolean flipY = node.getViewportPosition(screen.getCanvas()).y + tooltipDescription.getBoxHeight() + tooltipTitle.getBoxHeight() / 2 - padding / 2 > screen.getCanvas().VIEWPORT_HEIGHT;
        if (flipX && flipY) renderDirection = RenderDirection.LEFT_UP;
        else if (flipX) renderDirection = RenderDirection.LEFT_DOWN;
        else if (flipY) renderDirection = RenderDirection.RIGHT_UP;
        else renderDirection = RenderDirection.RIGHT_DOWN;

        int baseX = node.getRenderedPosition(screen.getCanvas()).x;
        int baseY = node.getRenderedPosition(screen.getCanvas()).y;
        switch (renderDirection) {
            case RIGHT_DOWN -> {
                titleBoxTopLeft = new Vector2i(baseX, baseY);
                descriptionBoxTopLeft = new Vector2i(titleBoxTopLeft.x + 1, titleBoxTopLeft.y + titleBoxHeight / 2);
            }

            case LEFT_DOWN -> {
                titleBoxTopLeft = new Vector2i(baseX - titleBoxWidth + iconSize - 2, baseY);
                descriptionBoxTopLeft = new Vector2i(titleBoxTopLeft.x + 1, titleBoxTopLeft.y + titleBoxHeight / 2);
            }

            case RIGHT_UP -> {
                titleBoxTopLeft = new Vector2i(baseX, baseY);
                descriptionBoxTopLeft = new Vector2i(titleBoxTopLeft.x + 1, (titleBoxTopLeft.y + titleBoxHeight / 2) - tooltipDescription.getBoxHeight() - padding/2);
            }

            case LEFT_UP -> {
                titleBoxTopLeft = new Vector2i(baseX - titleBoxWidth + iconSize - 2, baseY);
                descriptionBoxTopLeft = new Vector2i(titleBoxTopLeft.x + 1, (titleBoxTopLeft.y + titleBoxHeight / 2) - tooltipDescription.getBoxHeight() - padding/2);
            }
        }
    }

    public RenderDirection getRenderDirection() {
        return this.renderDirection;
    }

    public TooltipTitle getTooltipTitle() {
        return tooltipTitle;
    }

    public Vector2i getCostStackPos() {
        return costStackPos;
    }

    public void setCostStackPos(Vector2i costStackPos) {
        this.costStackPos = costStackPos;
    }

    public List<CostEntry> getValidCosts() {
        return this.validCosts;
    }

    /**
     * Returns the currently active element from a list, cycling through it
     * based on system time and a given interval in milliseconds.
     *
     * @param <T> the type of elements
     * @param list the list of elements to cycle through
     * @param intervalMillis how long each element is shown before moving to the next
     * @return the current element
     */
    public static <T> T getCycledElement(List<T> list, long intervalMillis) {
        if (list == null || list.isEmpty()) return null;

        long currentTime = System.currentTimeMillis();
        int index = (int)((currentTime / intervalMillis) % list.size());

        return list.get(index);
    }

    public void setCurrentRenderedCost(CostEntry currentRenderedCost) {
        this.currentRenderedCost = currentRenderedCost;

        CostEntry renderedCost = getCurrentRenderedCost();

        if(renderedCost.getCostItemTag().isPresent()) {
            String itemTag = renderedCost.getCostItemTag().get().itemTag();
            costDescriptionComponents.set(0, Component.translatable("gui.immersiveenchanting.accepts_any_tag", itemTag)
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }



    public CostEntry getCurrentRenderedCost() {
        return currentRenderedCost;
    }


}
