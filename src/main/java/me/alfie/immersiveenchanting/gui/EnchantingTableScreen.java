package me.alfie.immersiveenchanting.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.datapack.EnchantmentCostRegistry;
import me.alfie.immersiveenchanting.datapack.LevelCost;
import me.alfie.immersiveenchanting.networking.ModPacketHandler;
import me.alfie.immersiveenchanting.networking.packets.EnchantItemPacket;
import me.alfie.immersiveenchanting.networking.packets.UpdateToolSlotPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EnchantingTableScreen extends AbstractContainerScreen<EnchantingTableMenu> {

    public static final ResourceLocation ENCHANTING_TABLE_BACKGROUND_TEXTURE = ResourceLocation.fromNamespaceAndPath(ImmersiveEnchanting.MODID,
            "textures/gui/container/enchanting_table.png");
    public static final ResourceLocation TILE_TEXTURE = ResourceLocation.fromNamespaceAndPath(ImmersiveEnchanting.MODID,
            "textures/gui/container/background.png");
    public static final ResourceLocation BOOK_OPEN_TEXTURE = ResourceLocation.fromNamespaceAndPath(ImmersiveEnchanting.MODID,
            "textures/gui/container/book_open_shadow.png");
    public static final ResourceLocation ENCHANTING_TABLE_TOP_TEXTURE = ResourceLocation.fromNamespaceAndPath(ImmersiveEnchanting.MODID,
            "textures/gui/container/enchanting_table_top.png");
    public static final ResourceLocation BOOK_CLOSED_TEXTURE = ResourceLocation.fromNamespaceAndPath(ImmersiveEnchanting.MODID,
            "textures/gui/container/book_closed.png");
    public final List<EnchantingNodeBranch> branches = new ArrayList<>();


    protected final int VIEWPORT_WIDTH = 247; //Dimensions of viewport in the texture
    protected final int VIEWPORT_HEIGHT = 117; //Dimensions of viewport in the texture
    final List<EnchantingNode> rendered_nodes = new ArrayList<>();
    private final int TILE_TEXTURE_SIZE = 16;
    private final Vector2i VIEWPORT_TOP_LEFT = new Vector2i(5, 5); //Position that viewport starts on the texture (top left)
    private final boolean croppingEnabled = true; //Whether to crop the canvas outside of viewport - false for debugging.
    private final Player player;
    private final int VIRTUAL_SLOT_DIMENSIONS = 16;
    int scrollableCanvasWidth = TILE_TEXTURE_SIZE * 64; //Must be divisible by tileSize (16), otherwise rendered tiles/edge constraints will leave gaps
    int scrollableCanvasHeight = TILE_TEXTURE_SIZE * 64; //Must be divisible by tileSize (16), otherwise rendered tiles/edge constraints will leave gaps
    int canvasLeftPos;
    int canvasTopPos;
    double scrollX = 0;
    double scrollY = 0;
    private boolean dragging = false;
    private double dragStartMouseX = 0;
    private double dragStartMouseY = 0;
    private double dragStartScrollX = 0;
    private double dragStartScrollY = 0;
    private float currentBackgroundBrightness = 1f;
    private EnchantingNode hoveredNode;
    private EnchantingNode lastHoveredNode;
    private boolean lockHover;
    private EnchantingNodeTooltip enchantingNodeTooltip;
    private ItemStack lastStack = ItemStack.EMPTY; //Handling which tool in slot
    private Vector2i virtualSlotPos;

    public EnchantingTableScreen(EnchantingTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        this.titleLabelX = 10;
        this.inventoryLabelX = 10;
        this.imageHeight = 222;
        this.imageWidth = 256;

        // Center the canvas inside the viewport at start
        this.scrollX = (scrollableCanvasWidth / 2.0) - (VIEWPORT_WIDTH / 2.0);
        this.scrollY = (scrollableCanvasHeight / 2.0) - (VIEWPORT_HEIGHT / 2.0);

        //No branch shenanigans here, do it in init() pls <3
        this.player = playerInventory.player;
    }

    @Override //Init code when GUI is created.
    public void init() {
        super.init();

        initializeScreen();
        onToolSlotChanged(); //Updates if screen size is changed while screen open
    }

    private void initializeScreen() {
        calculateCanvasSize();

        //Prevent tearing
        branches.clear();
        rendered_nodes.clear();
    }

    /**
     * Call when tool slot changes. Refresh the screen.
     */
    public void onToolSlotChanged() {
        player.playSound(SoundEvents.BOOK_PAGE_TURN);

        //Clear all nodes
        branches.clear();
        rendered_nodes.clear();
        setLockHover(false);

        if (menu.isToolSlotEmpty()) {
            initializeScreen();
        }

        buildNodeBranches(menu.getToolSlotItem());

        //Update canvas size
        int largestBranchLevel = 1;
        for (EnchantingNodeBranch branch : branches) {
            largestBranchLevel = Math.max(largestBranchLevel, branch.getNodes().size());
        }

        EnchantingNodeBranch.calculateNodeAnglesAndStep(this);
        //Place nodes after size change
        for (EnchantingNodeBranch branch : branches) {
            branch.placeNodesAlongLine();
            for (EnchantingNode node : branch.getNodes()) {
                node.setScale(EnchantingNode.globalScale);
            }
        }
    }

    /**
     * Calculate the smallest possible canvas size to fit the highest level of available enchantment.
     */
    private void calculateCanvasSize() {
        //Setting size dynamically requires recentering each time.

        //Each NODE_STEP = 80 pixels, as canvas is centred 80 pixels * 2 reaches the centre of the first node
        int pixels = (EnchantingNodeBranch.node_step * 2) * EnchantmentCostRegistry.getClientRegistry().getHighestEnchantmentLevel();
        int margin = TILE_TEXTURE_SIZE * 4; //Add 4 tile margin
        int rounded = ((pixels + TILE_TEXTURE_SIZE - 1) / TILE_TEXTURE_SIZE) * TILE_TEXTURE_SIZE;

        scrollableCanvasWidth = rounded + margin;
        scrollableCanvasHeight = rounded + margin;

        scrollX = (scrollableCanvasWidth / 2.0) - (VIEWPORT_WIDTH / 2.0);
        scrollY = (scrollableCanvasHeight / 2.0) - (VIEWPORT_HEIGHT / 2.0);

        //These have to be updated here, idk why but it just breaks ok?
        canvasLeftPos = this.leftPos + VIEWPORT_TOP_LEFT.x;
        canvasTopPos = this.topPos + VIEWPORT_TOP_LEFT.y;
    }

    /**
     * Finds which enchantments are applicable for an item stack , generates branch angles, and creates the branches.
     *
     * @param currentItemStack
     */
    public void buildNodeBranches(ItemStack currentItemStack) {
        //Step 1. Find which enchantments are applicable.
        Set<Holder<Enchantment>> validEnchantments = new HashSet<>(); //Set of all enchantments that can be applied
        Map<Holder<Enchantment>, Integer> unlockedEnchantments = menu.getUnlockedEnchantments();

        Set<Holder<Enchantment>> allEnchantments = ImmersiveEnchanting.getEnchantmentRegistry(
                        player.level().registryAccess())
                .asLookup()
                .listElements()
                .collect(Collectors.toSet());

        //Iterate through the enchantment registry, see if the item support the enchantment.
        for (Holder<Enchantment> enchantmentHolder : allEnchantments) {
            //Skip cursed enchantments.
            if (enchantmentHolder.get().isCurse()) {
                continue;
            }

            ResourceKey<Enchantment> enchantmentKey = enchantmentHolder.unwrapKey().get();

            //If enchantment has DO_NOT_INCLUDE tag. (Empty json)
            if (EnchantmentCostRegistry.getClientRegistry().getCostRegistry().containsKey(enchantmentKey)) {
                if (EnchantmentCostRegistry.getClientRegistry().getCostRegistry()
                        .get(enchantmentKey)
                        .getLevel(-1).item().equals(LevelCost.DO_NOT_INCLUDE)) {
                    continue;
                }
            }

            //If this enchantment isn't compatible with any enchantments already applied to the item, then skip.
            Set<Enchantment> itemEnchantments = new HashSet<>(currentItemStack.getAllEnchantments().keySet());
            itemEnchantments.remove(enchantmentHolder.get()); //Ignore the enchantment we're trying to check for

            if (!EnchantmentHelper.isEnchantmentCompatible(itemEnchantments, enchantmentHolder.get())) {
                continue;
            }

            //Add enchantment
            if (enchantmentHolder.get().canEnchant(currentItemStack)) {
                validEnchantments.add(enchantmentHolder);
            }
        }

        // Step 2: Generate angles after filtering
        List<Float> angles = EnchantingNodeBranch.generateBranchAngles(validEnchantments.size());

        int i = 0;
        for (Holder<Enchantment> enchantmentHolder : validEnchantments) {
            // -1 means not unlocked at all; Integer.MAX_VALUE means all levels unlocked
            int maxUnlockedLevel = unlockedEnchantments.getOrDefault(enchantmentHolder, -1);
            boolean isUnlocked = maxUnlockedLevel >= 0;

            //Get the level of this enchantment already on the item
            int enchantmentLevel = currentItemStack.getItem().getEnchantmentLevel(currentItemStack, enchantmentHolder.get());

            branches.add(new EnchantingNodeBranch(
                    this,
                    angles.get(i),
                    enchantmentHolder,
                    enchantmentLevel,
                    isUnlocked,
                    maxUnlockedLevel,
                    player
            ));
            i++;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        //Render the item stack in the node tooltip when hovered.
        if (enchantingNodeTooltip != null) {
            if (mouseX >= enchantingNodeTooltip.getCostStackPos().x
                    && mouseX < enchantingNodeTooltip.getCostStackPos().x + 16
                    && mouseY >= enchantingNodeTooltip.getCostStackPos().y
                    && mouseY < enchantingNodeTooltip.getCostStackPos().y + 16) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(0, 0, 500);
                guiGraphics.renderTooltip(font,
                        enchantingNodeTooltip.getCostStack(),
                        mouseX, mouseY);
                guiGraphics.pose().popPose();
            }
        }


        //Draw item tooltips
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        //Inventory labels not required for this GUI, method has to be included though.
    }

    /**
     * Main render code.
     *
     * @param guiGraphics
     * @param partialTick
     * @param mouseX
     * @param mouseY
     */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        //Fade the background and nodes darker if hovering
        float targetBrightness = isHoveringAnyNode() ? 0.15f : 1f;
        float fadeSpeed = 0.3f;
        currentBackgroundBrightness += (targetBrightness - currentBackgroundBrightness) * fadeSpeed;
        guiGraphics.setColor(currentBackgroundBrightness, currentBackgroundBrightness, currentBackgroundBrightness, 1f);

        //Darken background if tool slot is empty
        if (menu.isToolSlotEmpty()) {
            guiGraphics.setColor(0.5F, 0.5F, 0.5F, 1f);
        }

        renderTiledBg(guiGraphics);
        renderBranchConnections(guiGraphics);
        renderCentralSprites(guiGraphics);
        renderNodes(guiGraphics);


        EnchantingNode nodeToRender;
        if (isLockHover()) {
            nodeToRender = lastHoveredNode;
        } else {
            setHoveredNode(mouseX, mouseY); //Update hovered node
            nodeToRender = getHoveredNode();
        }

        if (nodeToRender != null) {
            renderNodeTooltip(nodeToRender, guiGraphics);
            renderHoveredNode(nodeToRender, guiGraphics);
        } else {
            enchantingNodeTooltip = null;
        }


        //Disable scissor after drawing
        RenderSystem.disableScissor();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(ENCHANTING_TABLE_BACKGROUND_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        RenderSystem.disableBlend();
    }

    private boolean isHoveringAnyNode() {
        return hoveredNode != null;
    }

    /**
     * Render a tiled background and set up viewport culling.
     *
     * @param guiGraphics
     */
    private void renderTiledBg(GuiGraphics guiGraphics) {
        //Setup viewport culling
        int viewportLeft = this.leftPos + VIEWPORT_TOP_LEFT.x;
        int viewportTop = this.topPos + VIEWPORT_TOP_LEFT.y;
        int viewportRight = viewportLeft + VIEWPORT_WIDTH;
        int viewportBottom = viewportTop + VIEWPORT_HEIGHT;

        if (croppingEnabled) {
            double scale = this.minecraft.getWindow().getGuiScale();
            RenderSystem.enableScissor(
                    (int) (viewportLeft * scale),
                    (int) ((this.height - viewportBottom) * scale), // Y is flipped!
                    (int) (VIEWPORT_WIDTH * scale),
                    (int) (VIEWPORT_HEIGHT * scale)
            );
        }

        //Draw a tiled background using TILE_TEXTURE as a background
        int numberOfTilesX = (scrollableCanvasWidth / TILE_TEXTURE_SIZE);
        int numberOfTilesY = (scrollableCanvasHeight / TILE_TEXTURE_SIZE);

        for (int x = 0; x < numberOfTilesX; x++) {
            for (int y = 0; y < numberOfTilesY; y++) {
                // Calculate tile screen position
                int tileScreenX = canvasLeftPos + x * TILE_TEXTURE_SIZE - (int) scrollX;
                int tileScreenY = canvasTopPos + y * TILE_TEXTURE_SIZE - (int) scrollY;

                // Skip tiles completely outside the viewport
                if (tileScreenX + TILE_TEXTURE_SIZE < viewportLeft || tileScreenX > viewportRight ||
                        tileScreenY + TILE_TEXTURE_SIZE < viewportTop || tileScreenY > viewportBottom) {
                    continue;
                }

                guiGraphics.blit(
                        TILE_TEXTURE,
                        canvasLeftPos + x * TILE_TEXTURE_SIZE - (int) scrollX,
                        canvasTopPos + y * TILE_TEXTURE_SIZE - (int) scrollY,
                        0f, 0f,
                        TILE_TEXTURE_SIZE, TILE_TEXTURE_SIZE,
                        TILE_TEXTURE_SIZE, TILE_TEXTURE_SIZE
                );
            }
        }
        //Reset brightness
        guiGraphics.setColor(1f, 1f, 1f, 1f);
    }

    private void renderBranchConnections(GuiGraphics guiGraphics) {
        for (EnchantingNodeBranch branch : branches) {
            guiGraphics.setColor(currentBackgroundBrightness, currentBackgroundBrightness, currentBackgroundBrightness, 1f);

            //Don't darken the hovered node branch.
            if (branch.getNodes().contains(hoveredNode)) {
                guiGraphics.setColor(1f, 1f, 1f, 1f);
            }

            //branch.drawNodeConnections(guiGraphics);
            if (!branch.hasCalculatedConnections()) {
                branch.calculateNodeConnections();
            }

            branch.renderPrecomputedConnection(guiGraphics, (int) scrollX, (int) scrollY);

            //Reset color after darkening
            guiGraphics.setColor(1f, 1f, 1f, 1f);
        }
    }

    /**
     * Render the enchanting table, book, and item in the centre of the screen.
     *
     * @param guiGraphics
     */
    private void renderCentralSprites(GuiGraphics guiGraphics) {
        //Add background book and virtual slot
        guiGraphics.blit(
                ENCHANTING_TABLE_TOP_TEXTURE,
                centerOnCanvas(32, 32).x - (int) scrollX,
                centerOnCanvas(32, 32).y - (int) scrollY,
                0f, 0f, 32, 32,
                32, 32
        );
        virtualSlotPos = centerOnCanvas(VIRTUAL_SLOT_DIMENSIONS, VIRTUAL_SLOT_DIMENSIONS);

        if (this.menu.getSlot(0).getItem().isEmpty()) {
            guiGraphics.blit(
                    BOOK_CLOSED_TEXTURE,
                    centerOnCanvas(32, 32).x - (int) scrollX,
                    centerOnCanvas(32, 32).y - (int) scrollY,
                    0f, 0f, 32, 32,
                    32, 32
            );
        } else {
            guiGraphics.blit(
                    BOOK_OPEN_TEXTURE,
                    centerOnCanvas(32, 32).x - (int) scrollX,
                    centerOnCanvas(32, 32).y - (int) scrollY,
                    0f, 0f, 32, 32,
                    32, 32
            );
        }


        //Render the item in slot0 (tool slot)
        ItemStack stack = this.menu.getSlot(0).getItem(); //to get the ItemStack
        guiGraphics.renderItem(stack, centerOnCanvas(16, 16).x - (int) scrollX,
                centerOnCanvas(16, 16).y - (int) scrollY);
    }

    /**
     * Render nodes (excluding the current hovered node).
     *
     * @param guiGraphics
     */
    private void renderNodes(GuiGraphics guiGraphics) {
        for (EnchantingNode node : rendered_nodes) {
            guiGraphics.setColor(currentBackgroundBrightness, currentBackgroundBrightness, currentBackgroundBrightness, 1f);
            node.render(guiGraphics, this);

            node.setScale(EnchantingNode.globalScale);

            //Reset color after darkening
            guiGraphics.setColor(1f, 1f, 1f, 1f);
        }
    }

    public boolean isLockHover() {
        return lockHover;
    }

    private void setHoveredNode(double mouseX, double mouseY) {
        for (EnchantingNode node : rendered_nodes) {
            if (isMouseOverNode(node, mouseX, mouseY)) {
                hoveredNode = node;
                return;
            }
        }
        hoveredNode = null;
    }

    private EnchantingNode getHoveredNode() {
        return hoveredNode;
    }

    private void renderNodeTooltip(EnchantingNode node, GuiGraphics guiGraphics) {
        if (!node.equals(lastHoveredNode) || enchantingNodeTooltip == null) {
            enchantingNodeTooltip = new EnchantingNodeTooltip(
                    this.font,
                    node,
                    EnchantmentCostRegistry.getClientRegistry()
                            .getEnchantmentCost(node.getEnchantment())
                            .getLevel(node.getEnchantmentLevel())
                            .asItemStack(),
                    this
            );
            player.playSound(SoundEvents.CHISELED_BOOKSHELF_PICKUP_ENCHANTED);
            lastHoveredNode = node;
        }

        if (enchantingNodeTooltip != null) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0, 0, 200);
            enchantingNodeTooltip.renderEnchantmentTooltip(guiGraphics);
            guiGraphics.pose().popPose();
        }

    }

    /**
     * Render the node that is currently hovered over.
     *
     * @param guiGraphics
     */
    private void renderHoveredNode(EnchantingNode node, GuiGraphics guiGraphics) {
        //Render the hovered node.
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 500);
        node.setScale(1f);
        node.render(guiGraphics, this);
        guiGraphics.pose().popPose();
    }

    /**
     * Helper function to get the coordinate to center an object on the canvas.
     *
     * @param textureWidth
     * @param textureHeight
     * @return
     */
    public Vector2i centerOnCanvas(int textureWidth, int textureHeight) {
        int x = canvasLeftPos + scrollableCanvasWidth / 2 - textureWidth / 2;
        int y = canvasTopPos + scrollableCanvasHeight / 2 - textureHeight / 2;
        return new Vector2i(x, y);
    }

    /**
     * Helper function to check if a node is being moused over.
     *
     * @param node
     * @param mouseX
     * @param mouseY
     * @return
     */
    public boolean isMouseOverNode(EnchantingNode node, double mouseX, double mouseY) {
        return isMouseOverBoundingBox(new Vector2i(node.getX(), node.getY()),
                EnchantingNode.width, EnchantingNode.height, mouseX, mouseY);
    }

    /**
     * Check if mouse is over a bounding box. Automatically clips the bounding box if out of viewport bounds.
     *
     * @param topLeftPos Top Left position of the bounding box to detect
     * @param width      Width of the bounding box to detect
     * @param height     Height of the bounding box to detect
     * @param mouseX
     * @param mouseY
     * @return
     */
    public boolean isMouseOverBoundingBox(
            Vector2i topLeftPos,
            int width,
            int height,
            double mouseX,
            double mouseY
    ) {
        // Node's position on screen
        int drawX = topLeftPos.x - (int) scrollX;
        int drawY = topLeftPos.y - (int) scrollY;

        // Node bounds
        int boxLeft = drawX;
        int boxTop = drawY;
        int boxRight = drawX + width;
        int boxBottom = drawY + height;

        // Viewport bounds
        int viewportLeft = canvasLeftPos;
        int viewportTop = canvasTopPos;
        int viewportRight = viewportLeft + VIEWPORT_WIDTH;
        int viewportBottom = viewportTop + VIEWPORT_HEIGHT;

        // Clip node bounds to viewport
        int visibleLeft = Math.max(boxLeft, viewportLeft);
        int visibleTop = Math.max(boxTop, viewportTop);
        int visibleRight = Math.min(boxRight, viewportRight);
        int visibleBottom = Math.min(boxBottom, viewportBottom);

        // If the node is fully outside the viewport, return false
        if (visibleLeft >= visibleRight || visibleTop >= visibleBottom) return false;

        // Check if mouse is over the visible part
        return mouseX >= visibleLeft && mouseX < visibleRight
                && mouseY >= visibleTop && mouseY < visibleBottom;
    }

    private void setLockHover(boolean lockHover) {
        this.lockHover = lockHover;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            ItemStack carriedStack = menu.getCarried();
            if (!menu.isToolSlotEmpty()) {

                // 1. Check if hovered node was clicked
                if (hoveredNode != null) {
                    if (isMouseOverNode(hoveredNode, mouseX, mouseY)) {
                        this.onNodeClicked(hoveredNode);
                        return true;
                    }
                }


                // Check if the centre icon was clicked
                if (isMouseOverBoundingBox(virtualSlotPos, VIRTUAL_SLOT_DIMENSIONS, VIRTUAL_SLOT_DIMENSIONS, mouseX, mouseY)
                        && carriedStack.isEmpty()) {
                    ModPacketHandler.INSTANCE.sendToServer(new UpdateToolSlotPacket(UpdateToolSlotPacket.MODE.TAKE.ordinal()));
                    return true;
                }

                // 2. Start a drag if no node clicked
                // Compute viewport bounds in screen coordinates
                int viewportLeft = this.leftPos + VIEWPORT_TOP_LEFT.x;
                int viewportTop = this.topPos + VIEWPORT_TOP_LEFT.y;
                int viewportRight = viewportLeft + VIEWPORT_WIDTH;
                int viewportBottom = viewportTop + VIEWPORT_HEIGHT;

                // Only start dragging if the mouse is inside the viewport
                if (mouseX >= viewportLeft && mouseX < viewportRight &&
                        mouseY >= viewportTop && mouseY < viewportBottom) {
                    dragging = true;
                    dragStartMouseX = mouseX;
                    dragStartMouseY = mouseY;
                    dragStartScrollX = scrollX;
                    dragStartScrollY = scrollY;
                    return true; // consume the click
                }

            } else {
                //Send packet to place carried item in slot.
                //Check if mouse if over the virtual slot
                if (isMouseOverBoundingBox(virtualSlotPos, VIRTUAL_SLOT_DIMENSIONS, VIRTUAL_SLOT_DIMENSIONS, mouseX, mouseY)
                        && !carriedStack.isEmpty()) {
                    ModPacketHandler.INSTANCE.sendToServer(new UpdateToolSlotPacket(UpdateToolSlotPacket.MODE.PLACE.ordinal()));
                    return true;
                }
            }

        }

        if (button == 1) {
            if (isHoveringAnyNode()) { //Right click a node to lock it, right click anywhere to escape.
                setLockHover(!isLockHover());

                if (isLockHover()) {
                    player.playSound(SoundEvents.DISPENSER_FAIL, 1f, 2f);
                }

                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            scrollX = dragStartScrollX + (dragStartMouseX - mouseX);
            scrollY = dragStartScrollY + (dragStartMouseY - mouseY);

            //Left Edge
            if (scrollX + canvasLeftPos < canvasLeftPos) {
                scrollX = 0;
            }

            //Top Edge
            if (scrollY + canvasTopPos < canvasTopPos) {
                scrollY = 0;
            }

            // Right edge
            if (scrollX > scrollableCanvasWidth - VIEWPORT_WIDTH) {
                scrollX = scrollableCanvasWidth - VIEWPORT_WIDTH;
            }

            // Bottom edge
            if (scrollY > scrollableCanvasHeight - VIEWPORT_HEIGHT) {
                scrollY = scrollableCanvasHeight - VIEWPORT_HEIGHT;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);

    }

    /**
     * Detect if item changes in tool slot.
     */
    @Override
    public void containerTick() {
        super.containerTick();

        ItemStack current_item = this.menu.getToolSlotItem();
        if (!ItemStack.isSameItemSameTags(current_item, lastStack)) {
            this.lastStack = current_item.copy();
            onToolSlotChanged();
        }
    }

    /**
     * Handle left-click on node.
     *
     * @param node
     */
    private void onNodeClicked(EnchantingNode node) {
        if (!node.isObtained() && node.isBranchUnlocked) {
            //Get item at slot
            //player.level().playSound(player, player.blockPosition(),
            //SoundEvents.BEACON_POWER_SELECT, SoundSource.MASTER,
            //        1.0F, 1.0F);

            ItemStack stack = this.menu.getSlot(0).getItem();

            //Give server ResourceKey<Enchantment>.location().toString()
            //Server uses RESOURCE_KEY_MAP.get() to find the corresponding ResourceKey
            //Server enchants tool, client side cannot do it.
            ModPacketHandler.INSTANCE.sendToServer(new EnchantItemPacket(
                    node.getEnchantmentHolder().unwrapKey().get(),
                    node.getEnchantmentLevel()
            ));
        }

    }
}