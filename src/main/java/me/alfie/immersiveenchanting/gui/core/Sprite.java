package me.alfie.immersiveenchanting.gui.core;

import me.alfie.alfinolib.util.ResourceId;
import me.alfie.immersiveenchanting.ImmersiveEnchanting;

public enum Sprite {
    ENCHANTING_TABLE_GUI("textures/gui/container/enchanting_table.png", 256, 256),
    BACKGROUND_TILE("textures/gui/sprites/background_tile.png", 16, 16),
    ALT_BACKGROUND_TILE("textures/gui/sprites/alt_background_tile.png", 16, 16),


    //Enchanting tab
    BOOK_OPEN("textures/gui/sprites/enchantingtab/book_open.png", 32, 32),
    ENCHANTING_TABLE_TOP("textures/gui/sprites/enchantingtab/enchanting_table_top.png", 32, 32),
    BOOK_CLOSED("textures/gui/sprites/enchantingtab/book_closed.png", 32, 32),

    //Tooltip
    XP_LEVEL("textures/gui/sprites/tooltip/xp_level.png", 16, 16),
    MOUSE_HINT_OFF("textures/gui/sprites/tooltip/mouse_hint_off.png", 8, 8),
    MOUSE_HINT_ON("textures/gui/sprites/tooltip/mouse_hint_on.png", 8, 8),

    //Book Tab
    SCROLLBAR("textures/gui/sprites/booktab/scrollbar.png", 14, 114),
    SCROLLER("textures/gui/sprites/booktab/scroller.png", 12, 15),
    SEARCH("textures/gui/sprites/booktab/search.png", 100, 20),
    CHECKBOX_ON("textures/gui/sprites/booktab/checkbox_on.png", 16, 16),
    CHECKBOX_OFF("textures/gui/sprites/booktab/checkbox_off.png", 16, 16),
    ENCHANTMENT_BOX_UNLOCKED("textures/gui/sprites/booktab/enchantment_box_unlocked.png", 108, 19),
    ENCHANTMENT_BOX_LOCKED("textures/gui/sprites/booktab/enchantment_box_locked.png", 108, 19),
    LOCKED_ENCHANTMENT("textures/gui/sprites/booktab/locked_enchantment.png", 19, 19),

    //Enchanting Tab Nodes
    BASIC_NODE_UNOBTAINED("textures/gui/sprites/node/basic_node_unobtained.png", 26, 26),
    BASIC_NODE_OBTAINED("textures/gui/sprites/node/basic_node_obtained.png", 26, 26),
    ADVANCED_NODE_UNOBTAINED("textures/gui/sprites/node/advanced_node_unobtained.png", 26, 26),
    ADVANCED_NODE_OBTAINED("textures/gui/sprites/node/advanced_node_obtained.png", 26, 26),
    ELITE_NODE_UNOBTAINED("textures/gui/sprites/node/elite_node_unobtained.png", 26, 26),
    ELITE_NODE_OBTAINED("textures/gui/sprites/node/elite_node_obtained.png", 26, 26),
    LOCKED_NODE("textures/gui/sprites/node/locked_node.png", 26, 26),
    ALERT_NODE("textures/gui/sprites/node/alert_node.png", 26, 26),
    ERROR_NODE("textures/gui/sprites/node/error_node.png", 26, 26),

    //Jei
    JEI_ENCHANTING_ARROW("textures/gui/sprites/jei/jei_enchanting_arrow.png", 50, 8);
    private final ResourceId id;
    private final int width;
    private final int height;

    Sprite(String path, int width, int height) {
        this.id = new ResourceId(ImmersiveEnchanting.MODID, path);
        this.width = width;
        this.height = height;
    }

    public ResourceId id() {
        return id;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}