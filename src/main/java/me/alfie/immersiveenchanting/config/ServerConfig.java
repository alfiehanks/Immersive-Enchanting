package me.alfie.immersiveenchanting.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ServerConfig {

    public static final ServerConfig CONFIG;
    public static final ForgeConfigSpec CONFIG_SPEC;

    // Store the config properties as public finals
    public final ForgeConfigSpec.ConfigValue<Boolean> disableAncientBookRequirement;
    public final ForgeConfigSpec.ConfigValue<Integer> bookshelfSearchHeight;
    public final ForgeConfigSpec.ConfigValue<Boolean> vanillaBookMode;
    public final ForgeConfigSpec.ConfigValue<Boolean> xpCostMode;
    public final ForgeConfigSpec.ConfigValue<Integer> bookUnlockMode;



    static {
        Pair<ServerConfig, ForgeConfigSpec> pair =
                new ForgeConfigSpec.Builder().configure(ServerConfig::new);

        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

    // Constructor takes only the builder
    public ServerConfig(ForgeConfigSpec.Builder builder) {
        builder.push("general"); // optional grouping

        // Define the config value here
        disableAncientBookRequirement = builder
                .comment("If enabled, Ancient Books are no longer required to unlock enchantments at the enchanting table. Enchantments still cost experience and materials as usual.")
                .translation("immersiveenchanting.config.disable_ancient_book_requirement")
                .define("disableAncientBookRequirement", false);

        bookshelfSearchHeight = builder
                .comment("The number of blocks in the Y-level that the enchanting table can detect chiseled bookshelves. Each 'row' can hold up to 96 books. If you have many enchantments, you may need to increase this value to provide more space.") // translatable comment
                .translation("immersiveenchanting.config.bookshelf_search_height") // translatable label
                .defineInRange("bookshelfSearchHeight", 3, 1, 5);

        vanillaBookMode = builder
                .comment("If enabled, Ancient Books are replaced by regular enchanted books. Loot pools are not modified, and regular enchanted books can be placed in chiseled bookshelves to unlock enchantments at the enchanting table.")
                .translation("immersiveenchanting.config.vanilla_book_mode")
                .define("vanillaBookMode", false);

        xpCostMode = builder
                .comment("If enabled, enchanting costs XP levels instead of materials. Each enchantment level costs (level * 3) XP levels (e.g. level 1 = 3 levels, level 2 = 6 levels, level 3 = 9 levels). Material costs from datapacks are ignored.")
                .translation("immersiveenchanting.config.xp_cost_mode")
                .define("xpCostMode", false);

        bookUnlockMode = builder
                .comment("Controls how many enchantment levels are unlocked when a book is placed in a nearby chiseled bookshelf.\n" +
                        "  Note: This setting only has a meaningful effect when vanillaBookMode is enabled, since vanilla enchanted books carry level information.\n" +
                        "  With Ancient Books (vanillaBookMode = false), all modes behave the same as mode 2, as Ancient Books carry no level information.\n" +
                        "  0 = Restrictive: only the exact level of the book is unlocked (e.g. Efficiency III unlocks only level 3).\n" +
                        "  1 = Default: all levels up to and including the book's level are unlocked (e.g. Efficiency III unlocks levels 1, 2, and 3).\n" +
                        "  2 = Permissive: all levels of the enchantment are unlocked regardless of the book's level (e.g. Efficiency III unlocks all Efficiency levels).")
                .translation("immersiveenchanting.config.book_unlock_mode")
                .defineInRange("bookUnlockMode", 1, 0, 2);

        builder.pop();
    }

    /**
     * Returns true if ancient books are required, false if the disabled option is true.
     * @return
     */
    public static boolean areAncientBooksRequired() {
        return !ServerConfig.CONFIG.disableAncientBookRequirement.get();
    }

    public static int getBookshelfSearchHeight() {
        return ServerConfig.CONFIG.bookshelfSearchHeight.get();
    }

    public static boolean isVanillaBookModeEnabled() {
        return ServerConfig.CONFIG.vanillaBookMode.get();
    }

    public static boolean isXpCostModeEnabled() {
        return ServerConfig.CONFIG.xpCostMode.get();
    }

    public static int getBookUnlockMode() {
        return ServerConfig.CONFIG.bookUnlockMode.get();
    }
}