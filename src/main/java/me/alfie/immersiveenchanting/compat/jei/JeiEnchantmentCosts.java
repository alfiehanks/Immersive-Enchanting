package me.alfie.immersiveenchanting.compat.jei;

import me.alfie.immersiveenchanting.datapack.enchantment_cost.codec.CostData;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.codec.CostLevels;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;

public record JeiEnchantmentCosts(Holder<Enchantment> enchantmentHolder, CostData costData) {
}
