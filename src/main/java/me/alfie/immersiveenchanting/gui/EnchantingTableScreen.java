package me.alfie.immersiveenchanting.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import me.alfie.alfinolib.gui.CommonAbstractContainerScreen;
import me.alfie.alfinolib.gui.GuiGraphicsX;
import me.alfie.alfinolib.gui.util.GuiGraphicsApi;
import me.alfie.alfinolib.gui.util.MousePos;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.CostRegistry;
import me.alfie.immersiveenchanting.gui.canvas.Canvas;
import me.alfie.immersiveenchanting.gui.canvas.CanvasCamera;
import me.alfie.immersiveenchanting.gui.core.ScreenState;
import me.alfie.immersiveenchanting.gui.core.Sprite;
import me.alfie.immersiveenchanting.gui.tab.TabButton;
import me.alfie.immersiveenchanting.gui.tab.book.BookTab;
import me.alfie.immersiveenchanting.gui.tab.book.FilterCheckbox;
import me.alfie.immersiveenchanting.gui.tab.enchanting.EnchantingTab;
import me.alfie.immersiveenchanting.gui.tab.enchanting.node.Node;
import me.alfie.immersiveenchanting.gui.tab.enchanting.tooltip.CostRenderer;
import me.alfie.immersiveenchanting.gui.tab.enchanting.tooltip.TooltipManager;
import me.alfie.immersiveenchanting.item.ModItems;
import me.alfie.immersiveenchanting.util.FxHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

/**
 * Custom enchanting table screen handling both the enchanting and book browsing interfaces.
 *
 * <p>Manages rendering, input handling, and state switching between multiple UI modes,
 * including the enchantment tree view and the book library view.</p>
 *
 * <p>Also coordinates camera movement, tool slot updates, tooltip management,
 * and canvas-based node rendering for the enchanting system.</p>
 *
 * <p>This screen acts as the central controller for all client-side enchanting UI logic.</p>
 */
public class EnchantingTableScreen extends CommonAbstractContainerScreen<@NotNull EnchantingTableMenu> {
    private CanvasCamera camera;
    private final Canvas scrollableCanvas;
    private ScreenState screenState;

    private final EnchantingTab enchantingTab;
    private final BookTab bookTab;
    private final TabButton tabButton;

    private ItemStack lastToolSlotStack = ItemStack.EMPTY;

    private final Player player;
    private final RegistryAccess registryAccess;

    private final CostRenderer enchantmentCostRenderer;
    private final TooltipManager tooltipManager;

    private boolean isTabKeyDown;

    public EnchantingTableScreen(EnchantingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 256, 256);
        registryAccess = inventory.player.registryAccess();
        this.player = inventory.player;
        this.scrollableCanvas = new Canvas(this);
        setState(ScreenState.ENCHANTING);

        enchantingTab = new EnchantingTab(this);
        bookTab = new BookTab(this);
        tabButton = new TabButton(this);

        this.enchantmentCostRenderer = new CostRenderer(CostRegistry.client());
        this.tooltipManager = new TooltipManager(this);

        onToolSlotUpdate(ItemStack.EMPTY);
    }

    /**
     * Initializes the screen camera and centers it on the canvas.
     */
    @Override
    protected void init() {
        super.init();
        this.camera = new CanvasCamera(this,getGuiLeft()+4, getGuiTop()+4);
        camera.centerCameraOnCanvas();
    }

    @Override
    public void renderBackground(GuiGraphicsX gx, MousePos mousePos, float partialTick) {
        super.renderBackground(gx, mousePos, partialTick);

        if(!canvas().DEBUG_DISABLE_CULLING) gx.graphics().enableScissor(
                camera.VIEWPORT_X, camera.VIEWPORT_Y,
                camera.VIEWPORT_X + camera.VIEWPORT_WIDTH, camera.VIEWPORT_Y + camera.VIEWPORT_HEIGHT);

        gx.graphics().pose().pushPose();
        gx.graphics().pose().translate(camera.VIEWPORT_X, camera.VIEWPORT_Y, 1);
        gx.graphics().pose().scale(camera.zoom(), camera().zoom(), 1);
        gx.graphics().pose().translate(-camera.x(), -camera.y(), 1);

        canvas().render(gx);

        if(isState(ScreenState.ENCHANTING)) enchantingTab.render(gx, mousePos);
        gx.graphics().pose().popPose();

        gx.graphics().pose().pushPose();
        gx.graphics().pose().translate(0, 0, 2);
        if(isState(ScreenState.BOOKS)) bookTab.render(gx, mousePos);
        gx.graphics().pose().popPose();

        if(!canvas().DEBUG_DISABLE_CULLING) gx.graphics().disableScissor();

        RenderSystem.enableBlend();
        GuiGraphicsApi.blit(
                gx,
                Sprite.ENCHANTING_TABLE_GUI.id(),
                getGuiLeft(), getGuiTop(),
                imageWidth, imageHeight
        );
        RenderSystem.disableBlend();


        tabButton.render(gx, mousePos);
    }

    @Override
    public void render(GuiGraphicsX gx, MousePos mousePos, float partialTick) {
        super.render(gx, mousePos, partialTick);
        
        //Update tooltip manager
        List<Node> renderedNodes = enchantingTab.branchManager().getAllNodes();

        gx.graphics().pose().pushPose();
        gx.graphics().pose().translate(0, 0, 400);
        if(tooltipManager.hasActiveTooltip() && !renderedNodes.contains(tooltipManager.getActiveTooltipNode()))
            tooltipManager.clearActiveTooltip();

        if(tooltipManager.hasActiveTooltip())
            tooltipManager.getActiveTooltip().render(gx, mousePos);

        if(!tooltipManager.isTooltipLocked()) this.renderTooltip(gx.graphics(), mousePos.x(), mousePos.y());

        if(!tooltipManager.isTooltipRequestedThisFrame() && !tooltipManager.isTooltipLocked())
            tooltipManager.clearActiveTooltip();

        tooltipManager.resetFrameState();
        gx.graphics().pose().popPose();
    }

    @Override
    public void renderLabels(GuiGraphicsX gx, MousePos mousePos) {
        this.inventoryLabelX = 16;
        GuiGraphicsApi.text(gx, this.font, this.playerInventoryTitle.copy().withColor(-12566464), this.inventoryLabelX, this.inventoryLabelY - 33, false);
    }

    @Override
    public boolean onMouseClick(MousePos mousePos, int button) {
        if(tabButton.onMouseClick(mousePos, button)) return true;

        if(isState(ScreenState.ENCHANTING)) {
            if(enchantingTab.centralSlot().onMouseClick(mousePos, button)) return true;

            if(tooltipManager.hasActiveTooltip() && tooltipManager.getActiveTooltip().onMouseClick(mousePos, button)) return true;

            if(camera.onMouseClick(mousePos, button)) return true;
        } else if(isState(ScreenState.BOOKS)) {
            if(bookTab.scrollbar().onMouseClick(mousePos, button)) return true;

            for(FilterCheckbox checkbox : bookTab.filterCheckboxes()) if(checkbox.onMouseClick(mousePos, button)) return true;
        }

        return super.onMouseClick(mousePos, button);
    }

    @Override
    public boolean onMouseDrag(MousePos mousePos, int button, double dx, double dy) {
        if(camera.onMouseDrag(mousePos, button, dx / camera().zoom(), dy / camera().zoom())) return true;

        if(bookTab.scrollbar().onMouseDrag(mousePos, button, dx, dy)) return true;

        return super.onMouseDrag(mousePos, button, dx, dy);
    }

    @Override
    public boolean onMouseRelease(MousePos mousePos, int button) {
        if(tooltipManager().hasActiveTooltip() && tooltipManager().getActiveTooltip().onMouseRelease(mousePos, button)) return true;

        if(camera.onMouseRelease(mousePos, button)) return true;

        if(bookTab.scrollbar().onMouseRelease(mousePos, button)) return true;

        return super.onMouseRelease(mousePos, button);
    }

    @Override
    public boolean onMouseScrolled(MousePos mousePos, double scrollY) {
        if(isState(ScreenState.ENCHANTING)) {
            if(camera.onMouseScrolled(mousePos, scrollY)) return true;
        } else if(isState(ScreenState.BOOKS)) {
            if(bookTab.scrollbar().onMouseScrolled(mousePos, scrollY)) return true;
        }

        return super.onMouseScrolled(mousePos, scrollY);
    }

    @Override
    public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        if(isState(ScreenState.BOOKS)) {
            if(keyCode == InputConstants.KEY_BACKSPACE) {
                bookTab.searchbar().removeCharFromSearch();
            }

            if(keyCode == InputConstants.KEY_ESCAPE) {
                return super.onKeyPress(keyCode, scanCode, modifiers);
            } else {
                return true;
            }
        }

        if(isState(ScreenState.ENCHANTING)) {
            if(keyCode == InputConstants.KEY_TAB) {
                isTabKeyDown = true;
                return true;
            }
        }

        return super.onKeyPress(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if(keyCode == InputConstants.KEY_TAB) {
            isTabKeyDown = false;
            return true;
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean onCharTyped(char codePoint, int modifiers) {
        if(isState(ScreenState.BOOKS)) {
            bookTab.searchbar().addCharToSearch(codePoint);
        }

        return super.onCharTyped(codePoint, modifiers);
    }

    /**
     * Periodically checks for changes in the tool slot and triggers updates
     * when the item changes.
     */
    @Override
    protected void containerTick() {
        ItemStack stack = getMenu().getToolSlot().getItem();

        if(!ItemStack.isSameItemSameComponents(stack, lastToolSlotStack)) {
            onToolSlotUpdate(stack);
            lastToolSlotStack = stack.copy();
        }

        if(isTabKeyDown) {
            if (enchantingTab().isDisplay(EnchantingTab.Display.ENCHANTMENTS)
            && getMenu().getToolSlot().hasItem()
            && !getMenu().getToolSlot().getItem().is(ModItems.ANCIENT_BOOK.get())) {
                enchantingTab.setDisplay(EnchantingTab.Display.MOD_FILTERS);
                FxHelper.playTabDown(player());
                rebuildBranches();
                resetCamera();
                tooltipManager().unlockTooltip();
            }
        } else {
            if(enchantingTab().isDisplay(EnchantingTab.Display.MOD_FILTERS)) {
                enchantingTab.setDisplay(EnchantingTab.Display.ENCHANTMENTS);
                FxHelper.playTabUp(player());
                rebuildBranches();
                resetCamera();
            }
        }



    }

    /**
     * Handles updates to the tool slot item.
     *
     * <p>Rebuilds enchantment branches, updates canvas layout, plays UI feedback,
     * and adjusts camera behavior based on the new item state.</p>
     *
     * @param newStack the new item in the tool slot
     */
    private void onToolSlotUpdate(ItemStack newStack) {
        enchantingTab.setDisplay(EnchantingTab.Display.ENCHANTMENTS);
        rebuildBranches(newStack);
        if (!menu.getToolSlot().getItem().isEmpty()) FxHelper.playToolSlotChanged(player());

        if(camera() == null) return;
        if(newStack.getItem().equals(lastToolSlotStack.getItem())) return;
        camera().setDraggingEnabled(!newStack.isEmpty());
        resetCamera();
    }

    public void rebuildBranches() {
        rebuildBranches(lastToolSlotStack);
    }

    private void rebuildBranches(ItemStack newStack) {
        enchantingTab.branchManager().buildBranches(newStack);
        canvas().setSizeToFitNodes(CostRegistry.client().getHighestLevel());
        enchantingTab.branchManager().positionBranches();
    }

    /**
     * Reset camera zoom and recenter
     */
    private void resetCamera() {
        if(camera() == null) return;
        camera().setZoom(1f);
        camera().centerCameraOnCanvas();
    }

    /**
     * Sets the current screen state (e.g. enchanting or books view).
     *
     * @param state new screen state
     */
    public void setState(ScreenState state) {
        this.screenState = state;
    }

    /**
     * Checks whether the screen is currently in the given state.
     *
     * @param state state to check
     * @return true if active
     */
    public boolean isState(ScreenState state) {
        return this.screenState == state;
    }

    /** @return the main canvas used for node rendering */
    public Canvas canvas() {
        return scrollableCanvas;
    }

    /** @return the UI camera controller */
    public CanvasCamera camera() {
        return camera;
    }

    /** @return registry access for game data lookups */
    public RegistryAccess registryAccess() {
        return registryAccess;
    }

    /** @return the local player */
    public Player player() {
        return player;
    }

    /** @return renderer for enchantment costs */
    public CostRenderer enchantmentCostRenderer() {
        return enchantmentCostRenderer;
    }

    /** @return tooltip manager for interactive UI elements */
    public TooltipManager tooltipManager() {
        return tooltipManager;
    }

    /** @return book tab UI controller */
    public BookTab bookTab() {
        return bookTab;
    }

    public EnchantingTab enchantingTab() {
        return enchantingTab;
    }

    public Font getFont() {
        return font;
    }
}
