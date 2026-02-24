package me.alfie.immersiveenchanting.datapack;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashMap;
import java.util.Map;

//This class will be instancable, so there will be a server-side one and a client-side one.

public class EnchantmentCostRegistry {
    public static final EnchantmentCost EMPTY = new EnchantmentCost();
    private static EnchantmentCostRegistry serverEnchantmentCostRegistry; //Server-side access only
    private static EnchantmentCostRegistry clientEnchantmentCostRegistry; //Updated by server, safe to use on client
    // Maps the enchantment ResourceLocation (e.g., minecraft:efficiency) to its cost data
    private final Map<ResourceKey<Enchantment>, EnchantmentCost> COST_REGISTRY = new HashMap<>();
    private ItemStack lapisCost;
    private boolean xpCostMode = false;

    public static EnchantmentCostRegistry getClientRegistry() {
        return clientEnchantmentCostRegistry;
    }

    public static void setClientRegistry(EnchantmentCostRegistry enchantmentCostRegistry) {
        clientEnchantmentCostRegistry = enchantmentCostRegistry;
    }

    public static EnchantmentCostRegistry getServerRegistry() {
        return serverEnchantmentCostRegistry;
    }

    public static void setServerRegistry(EnchantmentCostRegistry enchantmentCostRegistry) {
        serverEnchantmentCostRegistry = enchantmentCostRegistry;
    }

    /**
     * Helper method to get enchantment cost from COST_REGISTRY from its resource key.
     *
     * @param enchantment
     * @return
     */
    public EnchantmentCost getEnchantmentCost(ResourceKey<Enchantment> enchantment) {
        return this.COST_REGISTRY.getOrDefault(enchantment, EMPTY);
    }

    /**
     * Helper method to get a specific level cost for an enchantment using its resource location.
     *
     * @param enchantment
     * @param level
     * @return
     */
    public LevelCost getLevelCost(ResourceKey<Enchantment> enchantment, int level) {
        EnchantmentCost data = this.COST_REGISTRY.get(enchantment);
        if (data == null) return null;
        return data.getLevel(level);
    }

    /**
     * Clear the cost registry
     */
    public void clear() {
        this.COST_REGISTRY.clear();
    }

    /**
     * Returns the integer of the highest level within the COST_REGISTRY.
     *
     * @return
     */
    public int getHighestEnchantmentLevel() {
        return this.COST_REGISTRY.values().stream()
                .mapToInt(EnchantmentCost::getHighestLevel)
                .max()
                .orElse(0); // return 0 if there are no enchantments
    }

    /**
     * Returns the cost registry map <ResourceLocation, EnchantmentCost>
     *
     * @return
     */
    public Map<ResourceKey<Enchantment>, EnchantmentCost> getCostRegistry() {
        return this.COST_REGISTRY;
    }

    public ItemStack getLapisCost() {
        return lapisCost;
    }

    public void setLapisCost(ItemStack lapisCost) {
        this.lapisCost = lapisCost;
    }

    public boolean isXpCostMode() {
        return xpCostMode;
    }

    public void setXpCostMode(boolean xpCostMode) {
        this.xpCostMode = xpCostMode;
    }
}
