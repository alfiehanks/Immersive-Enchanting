package me.alfie.immersiveenchanting.gui;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EnchantingTableMenu extends AbstractContainerMenu {

    private BlockPos blockPos;
    private ContainerLevelAccess access;
    private Level level;
    private IItemHandler containerInventory;
    /** Maps each unlocked enchantment to its max unlocked level. Integer.MAX_VALUE means all levels. */
    private Map<Holder<Enchantment>, Integer> unlockedEnchantments = new HashMap<>();


    //My constructor
    public EnchantingTableMenu(int containerId, Inventory inventory, IItemHandler containerInventory, Level level, BlockPos pos) {
        super(ModMenus.ENCHANTING_TABLE_MENU.get(), containerId);
        this.containerInventory = containerInventory;
        this.blockPos = pos;
        this.access = ContainerLevelAccess.create(level, pos);
        setupSlots(inventory);
    }

    //Game constructor
    public EnchantingTableMenu(int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(containerId, inventory, new ItemStackHandler(3), inventory.player.level(), buf.readBlockPos());
    }

    //Helper methods
    public void setupSlots(Inventory playerInventory) {
        //Tool slot
        this.addSlot(new SlotItemHandler(this.containerInventory, 0, 233, 141));

        //Lapis slot
        this.addSlot(new SlotItemHandler(this.containerInventory,1, 233, 199));

        //Cost slot
        this.addSlot(new SlotItemHandler(this.containerInventory,2, 233, 170));

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
            // ---- LAPIS SLOT ----
            else if (index == SLOTS.LAPIS.ordinal()) {
                // Move lapis back to player inventory
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
                // Try tool → slot 0
                if (stackInSlot.getItem().isEnchantable(stackInSlot)) {
                    if (!this.moveItemStackTo(stackInSlot, 0, 1, false)) return ItemStack.EMPTY;
                }
                // Try lapis → slot 1
                else if (stackInSlot.is(Items.LAPIS_LAZULI)) {
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
            for (int i = 0; i < this.containerInventory.getSlots(); i++) {
                ItemStack stack = this.containerInventory.extractItem(
                        i,
                        this.containerInventory.getStackInSlot(i).getCount(),
                        false
                ); // removes without triggering slot updates
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

    public Map<Holder<Enchantment>, Integer> getUnlockedEnchantments() {
        return unlockedEnchantments;
    }

    public void setUnlockedEnchantments(Map<Holder<Enchantment>, Integer> unlockedEnchantments) {
        this.unlockedEnchantments = unlockedEnchantments;
    }

    public boolean isToolSlotEmpty() {
        return getToolSlotItem().isEmpty();
    }

    public ItemStack getToolSlotItem() {
        return getItemInSlot(SLOTS.TOOL);
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
        LAPIS, //By default, this is the lapis slot, but it can be configured to other itemIds.
        COST
    }

}
