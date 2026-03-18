package me.alfie.immersiveenchanting.gui;

import me.alfie.immersiveenchanting.datapack.EnchantmentCostRegistry;
import me.alfie.immersiveenchanting.item.ModItems;
import me.alfie.immersiveenchanting.util.CostHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EnchantingTableMenu extends AbstractContainerMenu {

    private final BlockPos blockPos;
    private final ContainerLevelAccess access;
    private final Container container;

    private Set<String> unlockedEnchantmentResourceIds;

    private Set<Holder<Enchantment>> unlockedEnchantments = new HashSet<>();

    // --------------------
    // Game constructor
    // --------------------
    public EnchantingTableMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, playerInventory.player.level(), buf.readBlockPos());
    }

    // --------------------
    // My constructor
    // --------------------
    public EnchantingTableMenu(int containerId, Inventory playerInventory, Level level, BlockPos pos) {
        super(ModMenus.ENCHANTING_TABLE_MENU.get(), containerId);
        this.container = new SimpleContainer(3);
        this.blockPos = pos;
        this.access = ContainerLevelAccess.create(level, pos);
        setupSlots(playerInventory);
    }

    //Helper methods
    public void setupSlots(Inventory playerInventory) {
        //Tool slot
        this.addSlot(new Slot(this.container, 0, 233, 141) {
            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        //Enchanting fuel slot
        this.addSlot(new Slot(this.container, 1, 233, 199));

        //Cost slot
        this.addSlot(new Slot(this.container, 2, 233, 170));

        //Add player inventory slots
        int startX = 17;
        int startY = 140;

        //Player inventory 3 rows of 9
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, startX + col * 18, startY + row * 18));
            }
        }

        //Hotbar slots
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, startX + col * 18, startY + 58));
        }


    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            originalStack = stackInSlot.copy();

            // ---- TOOL SLOT ----
            if (index == SLOTS.TOOL.ordinal()) {
                // Move tool back to player inventory
                if (!this.moveItemStackTo(stackInSlot, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // ---- ENCHANTING FUEL SLOT ----
            else if (index == SLOTS.ENCHANTING_FUEL.ordinal()) {
                // Move enchanting fuel back to player inventory
                if (!this.moveItemStackTo(stackInSlot, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // ---- COST SLOT ----
            else if (index == SLOTS.COST.ordinal()) {
                // Move cost item back to player inventory
                if (!this.moveItemStackTo(stackInSlot, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
            }
            // ---- PLAYER INVENTORY ----
            else {
                List<Item> enchantingFuels = CostHelper.getItems(EnchantmentCostRegistry
                        .getRegistry(player.level())
                        .getEnchantingFuels().levels.values().stream().toList());

                // Try tool → slot 0
                if (stackInSlot.getItem().isEnchantable(stackInSlot) || stackInSlot.is(ModItems.ANCIENT_BOOK.get())) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) return ItemStack.EMPTY;
                }
                // Try enchanting fuel → slot 1 (Uses #neoforge:enchanting_fuels tag)
                else if (enchantingFuels.contains(stackInSlot.getItem())) {
                    if (!this.moveItemStackTo(stackInSlot, 1, 2, false)) return ItemStack.EMPTY;
                }
                // Everything else → cost slot (slot 2)
                else {
                    if (!this.moveItemStackTo(stackInSlot, 2, 3, false)) return ItemStack.EMPTY;
                }
            }

            // Update slot
            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return originalStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (!player.level().isClientSide) {
            // Loop through your container slots
            for (int i = 0; i < this.container.getContainerSize(); i++) {
                ItemStack stack = this.container.removeItemNoUpdate(i); // removes without triggering slot updates
                if (!stack.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(stack); // tries to return to inventory, drops if full
                }
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(this.access, player, Blocks.ENCHANTING_TABLE);
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public Set<Holder<Enchantment>> getUnlockedEnchantments() {
        return unlockedEnchantments;
    }

    public void setUnlockedEnchantments(Set<Holder<Enchantment>> unlockedEnchantments) {
        this.unlockedEnchantments = unlockedEnchantments;
    }

    public boolean isToolSlotEmpty() {
        return getToolSlotItem().isEmpty();
    }

    public ItemStack getToolSlotItem() {
        return getItemInSlot(SLOTS.TOOL);
    }

    public ItemStack getCostSlotItem() {
        return getItemInSlot(SLOTS.COST);
    }

    public ItemStack getEnchantingFuelSlotItem() {
        return getItemInSlot(SLOTS.ENCHANTING_FUEL);
    }

    /**
     * Helper to get item in slot using SLOTS enum.
     *
     * @param slot
     * @return
     */
    private ItemStack getItemInSlot(SLOTS slot) {
        return getSlot(slot.ordinal()).getItem();
    }

    public enum SLOTS {
        TOOL,
        ENCHANTING_FUEL, //Any item in the #neoforge:enchanting_fuels tag.
        COST
    }

    public boolean isEnchantmentUnlocked(Holder<Enchantment> enchantmentHolder) {
        return unlockedEnchantments.contains(enchantmentHolder);
    }

}
