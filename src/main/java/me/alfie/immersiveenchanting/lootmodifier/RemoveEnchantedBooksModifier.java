package me.alfie.immersiveenchanting.lootmodifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.alfie.immersiveenchanting.config.ServerConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;

public class RemoveEnchantedBooksModifier extends LootModifier {

    public static final Codec<RemoveEnchantedBooksModifier> CODEC =
            RecordCodecBuilder.create(inst ->
                    LootModifier.codecStart(inst).apply(inst, RemoveEnchantedBooksModifier::new)
            );


    // First constructor parameter is the list of conditions. The rest is our extra properties.
    public RemoveEnchantedBooksModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    // Return our codec here.
    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    /**
     * Apply loot modifier.
     *
     * @param generatedLoot
     * @param context
     * @return
     */
    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (ServerConfig.isVanillaBookModeEnabled()) return generatedLoot;
        generatedLoot.removeIf(stack -> stack.getItem() == Items.ENCHANTED_BOOK);
        return generatedLoot;
    }
}
