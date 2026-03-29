package me.alfie.immersiveenchanting.gui.core;

import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import net.minecraft.resources.ResourceLocation;

public enum Sprite {
    ENCHANTING_TABLE_GUI("textures/gui/container/enchanting_table.png"),
    BACKGROUND_TILE("textures/gui/sprites/background_tile.png"),

    //Enchanting tab
    BOOK_OPEN("textures/gui/sprites/enchantingtab/book_open.png"),
    ENCHANTING_TABLE_TOP("textures/gui/sprites/enchantingtab/enchanting_table_top.png"),
    BOOK_CLOSED("textures/gui/sprites/enchantingtab/book_closed.png"),



    //Tooltip
    XP_LEVEL("textures/gui/sprites/tooltip/xp_level.png"),
    MOUSE_HINT_OFF("textures/gui/sprites/tooltip/mouse_hint_off.png"),
    MOUSE_HINT_ON("textures/gui/sprites/tooltip/mouse_hint_on.png"),
    TOOLTIP_WIDGETS("textures/gui/sprites/tooltip/tooltip_widgets.png"),

    //Book Tab
    SCROLLBAR("textures/gui/sprites/booktab/scrollbar.png"),
    SCROLLER("textures/gui/sprites/booktab/scroller.png"),
    SEARCH("textures/gui/sprites/booktab/search.png"),
    CHECKBOX_ON("textures/gui/sprites/booktab/checkbox_on.png"),
    CHECKBOX_OFF("textures/gui/sprites/booktab/checkbox_off.png"),
    ENCHANTMENT_BOX_UNLOCKED("textures/gui/sprites/booktab/enchantment_box_unlocked.png"),
    ENCHANTMENT_BOX_LOCKED("textures/gui/sprites/booktab/enchantment_box_locked.png"),
    LOCKED_ENCHANTMENT("textures/gui/sprites/booktab/locked_enchantment.png");

    private final ResourceLocation location;

    Sprite(String path) {
        this.location = new ResourceLocation(ImmersiveEnchanting.MODID, path);
    }

    public ResourceLocation get() {
        return location;
    }
}