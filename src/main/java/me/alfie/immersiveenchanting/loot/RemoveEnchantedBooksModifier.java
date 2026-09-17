package me.alfie.immersiveenchanting.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.alfie.immersiveenchanting.config.ServerConfig;
import me.alfie.immersiveenchanting.item.ModItems;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.Iterator;

/**
 * Loot modifier that removes enchanted books from loot when disabled in config.
 */
public class RemoveEnchantedBooksModifier extends LootModifier {

    public static final MapCodec<RemoveEnchantedBooksModifier> CODEC = RecordCodecBuilder
            .mapCodec(inst ->
                    LootModifier.codecStart(inst).apply(inst, RemoveEnchantedBooksModifier::new));


    public RemoveEnchantedBooksModifier(LootItemCondition[] conditions, int priority) {
        super(conditions, 0);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }


    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {

        if(ServerConfig.isEnchantedBookLootMode(ServerConfig.EnchantedBookLootMode.REPLACE)) {
            for (Iterator<ItemStack> it = generatedLoot.iterator(); it.hasNext();) {
                ItemStack stack = it.next();

                if (stack.is(Items.ENCHANTED_BOOK)) {
                    ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
                    it.remove();

                    if (enchantments == null) continue;

                    for (Holder<Enchantment> enchantment : enchantments.keySet()) {
                        ItemStack ancientBook = new ItemStack(ModItems.ANCIENT_BOOK.get());
                        EnchantmentUtil.setStoredEnchantment(ancientBook, enchantment);
                        generatedLoot.add(ancientBook);
                    }
                }
            }
        } else if(ServerConfig.isEnchantedBookLootMode(ServerConfig.EnchantedBookLootMode.REMOVE)) {
            generatedLoot.removeIf(stack -> stack.is(Items.ENCHANTED_BOOK));
        }

        return generatedLoot;
    }
}
