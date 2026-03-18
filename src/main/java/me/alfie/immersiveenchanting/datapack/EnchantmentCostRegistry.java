package me.alfie.immersiveenchanting.datapack;

import me.alfie.immersiveenchanting.datapack.cost.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//This class will be instancable, so there will be a server-side one and a client-side one.

public class EnchantmentCostRegistry {
    private static EnchantmentCostRegistry serverEnchantmentCostRegistry; //Server-side access only
    private static EnchantmentCostRegistry clientEnchantmentCostRegistry; //Updated by server, safe to use on client

    public static EnchantmentCostRegistry getClientRegistry() {
        return clientEnchantmentCostRegistry;
    }

    public static EnchantmentCostRegistry getServerRegistry() {
        return serverEnchantmentCostRegistry;
    }

    /**
     * Returns the registry for the caller's side.
     */
    public static EnchantmentCostRegistry getRegistry(Level level) {
        if(level.isClientSide) {
            return getClientRegistry();
        } else {
            return getServerRegistry();
        }
    }

    public static void setClientRegistry(EnchantmentCostRegistry enchantmentCostRegistry) {
        clientEnchantmentCostRegistry = enchantmentCostRegistry;
    }

    public static void setServerRegistry(EnchantmentCostRegistry enchantmentCostRegistry) {
        serverEnchantmentCostRegistry = enchantmentCostRegistry;
    }

    //Maps the enchantment ResourceLocation (e.g., minecraft:efficiency) to its cost data
    private final Map<ResourceKey<Enchantment>, EnchantmentCost> COST_REGISTRY = new HashMap<>();
    public static final EnchantmentCost EMPTY = new EnchantmentCost(new HashMap<>());


    public enum InternalCosts {
        TRANSMUTE("immersiveenchanting:transmute"),
        REPLICATE("immersiveenchanting:replicate"),
        ENCHANTING_FUELS("immersiveenchanting:enchanting_fuels");

        private final String id;

        InternalCosts(String id) {
            this.id = id;
        }

        public String getId() {
            return this.id;
        }
    }

    //Stores costs for thing that are not enchantments i.e - transmute/replicate.
    private final Map<InternalCosts, EnchantmentCost> INTERNAL_REGISTRY = new HashMap<>();

    public Map<InternalCosts, EnchantmentCost> getInternalRegistry() {
        return INTERNAL_REGISTRY;
    }

    public EnchantmentCost getTransmuteCost() {
        return getInternalRegistry().get(InternalCosts.TRANSMUTE);
    }

    public EnchantmentCost getReplicateCost() {
        return getInternalRegistry().get(InternalCosts.REPLICATE);
    }

    public EnchantmentCost getEnchantingFuels() { return getInternalRegistry().get(InternalCosts.ENCHANTING_FUELS); }

    /**
     * Helper method to get enchantment cost from COST_REGISTRY from its resource location.
     * @param enchantment
     * @return
     */
    public EnchantmentCost getEnchantmentCost(ResourceKey<Enchantment> enchantment) {
        return this.COST_REGISTRY.getOrDefault(enchantment, EMPTY);
    }

    /**
     * Clear the cost registry
     */
    public void clear() {
        this.COST_REGISTRY.clear();
    }

    /**
     * Returns the integer of the highest level within the COST_REGISTRY.
     * @return
     */
    public int getHighestEnchantmentLevel() {
        return this.COST_REGISTRY.values().stream()
                .mapToInt(EnchantmentCost::getHighestLevel)
                .max()
                .orElse(0); // return 0 if there are no enchantments
    }

    /**
     * Returns the cost registry map <ResourceLocation, LegacyEnchantmentCost>
     * @return
     */
    public Map<ResourceKey<Enchantment>, EnchantmentCost> getCostRegistry() {
        return this.COST_REGISTRY;
    }

    /**
     * Returns a list of all enchantment IDs that are disabled.
     * @return
     */
    public List<String> getDisabledEnchantments() {
        List<String> disabledEnchantments = new ArrayList<>();
        for(Map.Entry<ResourceKey<Enchantment>, EnchantmentCost> entry : getCostRegistry().entrySet()) {
            EnchantmentCost cost = entry.getValue();
            if(!cost.enabled) {
                String enchantmentName = entry.getKey().location().toString();
                disabledEnchantments.add(enchantmentName);
            }
        }
        return disabledEnchantments;
    }

    /**
     * Returns a list of all enchantment IDs that are enabled.
     * @return
     */
    public List<String> getEnabledEnchantments() {
        List<String> enabledEnchantments = new ArrayList<>();
        for(Map.Entry<ResourceKey<Enchantment>, EnchantmentCost> entry : getCostRegistry().entrySet()) {
            EnchantmentCost cost = entry.getValue();
            if(cost.enabled) {
                String enchantmentName = entry.getKey().location().toString();
                enabledEnchantments.add(enchantmentName);
            }
        }
        return enabledEnchantments;
    }

    public String getName() {
        if(this == clientEnchantmentCostRegistry) {
            return "client";
        } else {
            return "server";
        }
    }
}
