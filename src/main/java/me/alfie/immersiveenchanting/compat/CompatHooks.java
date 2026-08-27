package me.alfie.immersiveenchanting.compat;

import me.alfie.immersiveenchanting.compat.ench_desc.EnchDescCompat;
import net.darkhax.enchdesc.common.impl.EnchdescMod;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.function.Consumer;

public final class CompatHooks {

    public static final class EnchantmentDescriptions {
        /**
         * Adds an enchantment description to a tooltip
         */
        public static void addEnchantmentDescription(Holder<Enchantment> enchantmentHolder, int level, Consumer<Component> consumer) {
            if(ModCompat.isModLoaded(ModCompat.Mods.ENCHANTMENT_DESCRIPTIONS)) {
                if(EnchDescCompat.config().only_in_enchanting_table) return;

                if(!EnchdescMod.getInstance().isKeybindConditionMet()) {
                    consumer.accept(EnchDescCompat.config().activate_text);
                } else {
                    consumer.accept(EnchDescCompat.getDescription(enchantmentHolder, level)
                            .withStyle(EnchDescCompat.config().style));
                }
            }
        }
    }


}
