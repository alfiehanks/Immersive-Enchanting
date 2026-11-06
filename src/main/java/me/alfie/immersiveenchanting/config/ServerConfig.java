package me.alfie.immersiveenchanting.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3i;

public class ServerConfig {

    public static final ServerConfig CONFIG;
    public static final ForgeConfigSpec CONFIG_SPEC;

    public final ForgeConfigSpec.ConfigValue<Boolean> DISABLE_ANCIENT_BOOK_REQUIREMENT;

    public final ForgeConfigSpec.ConfigValue<Integer> BOOKSHELF_SEARCH_X;
    public final ForgeConfigSpec.ConfigValue<Integer> BOOKSHELF_SEARCH_Y;
    public final ForgeConfigSpec.ConfigValue<Integer> BOOKSHELF_SEARCH_Z;

    public final ForgeConfigSpec.ConfigValue<Boolean> ALLOW_ENCHANTMENT_REMOVAL;

    public final ForgeConfigSpec.ConfigValue<Boolean> OBFUSCATE_LOCKED_ENCHANTMENTS;

    public final ForgeConfigSpec.ConfigValue<Boolean> DISABLE_ENCHANTED_BOOK_TRADES;

    public enum EnchantedBookLootMode {
        REPLACE,
        REMOVE,
        NOTHING
    }

    public final ForgeConfigSpec.EnumValue<EnchantedBookLootMode> ENCHANTED_BOOK_LOOT_MODE;

    static {
        Pair<ServerConfig, ForgeConfigSpec> pair =
                new ForgeConfigSpec.Builder().configure(ServerConfig::new);

        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

    // Constructor takes only the builder
    public ServerConfig(ForgeConfigSpec.Builder builder) {
        builder.push("bookshelves");
        BOOKSHELF_SEARCH_X = builder
                .comment("The number of blocks in the X-position that the enchanting table can detect chiseled bookshelves. Each 'row' can hold up to 96 books. If you have many enchantments, you may need to increase this value to provide more space.") // translatable comment
                .translation("immersiveenchanting.config.bookshelf_search_x") // translatable label
                .defineInRange("bookshelfSearchX", 2, 2, 8);
        BOOKSHELF_SEARCH_Y = builder
                .comment("The number of blocks in the Y-position that the enchanting table can detect chiseled bookshelves. Each 'row' can hold up to 96 books. If you have many enchantments, you may need to increase this value to provide more space.") // translatable comment
                .translation("immersiveenchanting.config.bookshelf_search_y") // translatable label
                .defineInRange("bookshelfSearchY", 3, 1, 8);
        BOOKSHELF_SEARCH_Z = builder
                .comment("The number of blocks in the Z-position that the enchanting table can detect chiseled bookshelves. Each 'row' can hold up to 96 books. If you have many enchantments, you may need to increase this value to provide more space.") // translatable comment
                .translation("immersiveenchanting.config.bookshelf_search_z") // translatable label
                .defineInRange("bookshelfSearchZ", 2, 2, 8);
        builder.pop();



        builder.push("enchantingtable");
        ALLOW_ENCHANTMENT_REMOVAL = builder
                .comment("Allow enchantments to be removed in the enchanting table.")
                .translation("immersiveenchanting.config.allow_enchantment_removal")
                .define("allowEnchantmentRemoval", true);

        OBFUSCATE_LOCKED_ENCHANTMENTS = builder
                .comment("If enabled, enchantments that have not been found will be obfuscated in the enchanting table.")
                .translation("immersiveenchanting.config.obfuscate_locked_enchantments")
                .define("obfuscateLockedEnchantments", true);

        DISABLE_ANCIENT_BOOK_REQUIREMENT = builder
                .comment("If enabled, Ancient Books are no longer required to unlock enchantments at the enchanting table. Enchantments still cost experience and materials as usual.")
                .translation("immersiveenchanting.config.disable_ancient_book_requirement")
                .define("disableAncientBookRequirement", false);
        builder.pop();

        builder.push("enchantedbooks");
        ENCHANTED_BOOK_LOOT_MODE = builder
                .comment("Choose whether enchanted books should be removed completely or replaced with ancient books. Ancient books still spawn as normal.")
                .translation("immersiveenchanting.config.enchanted_book_loot_mode")
                .defineEnum("enchantedBookLootMode", EnchantedBookLootMode.REPLACE);

        DISABLE_ENCHANTED_BOOK_TRADES = builder
                .comment("If enabled, vanilla enchanted books will not appear in villager trades.")
                .translation("immersiveenchanting.config.disable_enchanted_book_trades")
                .define("disableEnchantedBookTrades", true);
    }

    public static boolean areAncientBooksRequired() {
        return !ServerConfig.CONFIG.DISABLE_ANCIENT_BOOK_REQUIREMENT.get();
    }

    public static Vector3i getBookshelfSearchRadius() {
        return new Vector3i(ServerConfig.CONFIG.BOOKSHELF_SEARCH_X.get(),
                ServerConfig.CONFIG.BOOKSHELF_SEARCH_Y.get(),
                ServerConfig.CONFIG.BOOKSHELF_SEARCH_Z.get());
    }

    public static boolean isEnchantmentRemovalAllowed() {
        return ServerConfig.CONFIG.ALLOW_ENCHANTMENT_REMOVAL.get();
    }

    public static boolean isEnchantedBookLootMode(EnchantedBookLootMode mode) {
        return ServerConfig.CONFIG.ENCHANTED_BOOK_LOOT_MODE.get() == mode;
    }

    public static boolean areEnchantedBookTradesDisabled() {
        return ServerConfig.CONFIG.DISABLE_ENCHANTED_BOOK_TRADES.get();
    }

    public static boolean isObfuscateLockedEnchantments() {
        return ServerConfig.CONFIG.OBFUSCATE_LOCKED_ENCHANTMENTS.get();
    }
}
