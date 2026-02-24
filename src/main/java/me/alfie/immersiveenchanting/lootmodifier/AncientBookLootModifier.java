package me.alfie.immersiveenchanting.lootmodifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.alfie.immersiveenchanting.config.ServerConfig;
import me.alfie.immersiveenchanting.datapack.EnchantmentCostRegistry;
import me.alfie.immersiveenchanting.datapack.LevelCost;
import me.alfie.immersiveenchanting.item.AncientBook;
import me.alfie.immersiveenchanting.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;


public class AncientBookLootModifier extends LootModifier {

    public static final Codec<AncientBookLootModifier> CODEC =
            RecordCodecBuilder.create(inst ->
                    LootModifier.codecStart(inst).and(inst.group(
                            Codec.INT.fieldOf("count").forGetter(e -> e.count),
                            ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(e -> e.item),
                            Codec.FLOAT.fieldOf("chance").forGetter(e -> e.chance)
                    )).apply(inst, AncientBookLootModifier::new)
            );

    // Our extra properties.
    private final int count;
    private final Item item;
    private final float chance;

    // First constructor parameter is the list of conditions. The rest is our extra properties.
    public AncientBookLootModifier(LootItemCondition[] conditions, int count, Item item, float chance) {
        super(conditions);
        this.count = count;
        this.item = item;
        this.chance = chance;
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
        if (context.getRandom().nextFloat() < chance) { // chance from JSON
            ItemStack lootItem = new ItemStack(item, count);

            if (lootItem.getItem() == ModItems.ANCIENT_BOOK.get()) {
                // Get all available types
                RegistryAccess registryAccess = context.getLevel().registryAccess();
                HolderLookup.RegistryLookup<Enchantment> lookup = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
                List<Holder.Reference<Enchantment>> allEnchantments = lookup.listElements().toList();

                // Filter out MENDING if disabled
                List<Holder.Reference<Enchantment>> filteredEnchantments = allEnchantments.stream()
                        .filter(enchantment -> {
                            ResourceLocation keyLocation = enchantment.key().location();

                            // Remove disabled enchantments, such as mending
                            //If enchantment has DO_NOT_INCLUDE tag. (Empty json)
                            if (EnchantmentCostRegistry.getServerRegistry().getCostRegistry().containsKey(enchantment.key().location())) {
                                if (EnchantmentCostRegistry.getServerRegistry().getCostRegistry()
                                        .get(enchantment.key())
                                        .getLevel(-1).item().equals(LevelCost.DO_NOT_INCLUDE)) {
                                    return false;
                                }
                            }

                            // Skip cursed enchantments
                            return !enchantment.get().isCurse();// include everything else
                        })
                        .toList();


                // Pick a random enchantment type from the filtered list
                if (!filteredEnchantments.isEmpty()) {
                    Holder<Enchantment> randomEnchantment = filteredEnchantments.get(context.getRandom().nextInt(filteredEnchantments.size()));
                    AncientBook.setStoredEnchantment(lootItem, randomEnchantment);
                }
            }
            generatedLoot.add(lootItem);
        }
        return generatedLoot;
    }
}
