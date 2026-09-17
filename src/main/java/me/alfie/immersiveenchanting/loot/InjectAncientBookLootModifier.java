package me.alfie.immersiveenchanting.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.alfie.immersiveenchanting.config.ServerConfig;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.CostRegistry;
import me.alfie.immersiveenchanting.item.ModItems;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class InjectAncientBookLootModifier extends LootModifier {

    public static final MapCodec<InjectAncientBookLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(instance ->
                    LootModifier.codecStart(instance).and(
                            instance.group(
                                    Codec.STRING.fieldOf("mode")
                                            .forGetter(e -> e.mode),

                                    Enchantment.CODEC
                                            .listOf()
                                            .optionalFieldOf("enchantments")
                                            .forGetter(e -> Optional.ofNullable(e.enchantments)),

                                    Codec.INT.fieldOf("min_rolls")
                                            .forGetter(e -> e.minRolls),

                                    Codec.INT.fieldOf("max_rolls").
                                            forGetter(e -> e.maxRolls)
                            )
                    ).apply(instance, InjectAncientBookLootModifier::new)
            );

    @Override public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }


    private final String mode;
    private final List<Holder<Enchantment>> enchantments;
    private final int minRolls;
    private final int maxRolls;

    public enum Mode {
        RANDOMLY_ENCHANT_FROM_ENABLED("random_enchantment_from_enabled"),
        RANDOMLY_ENCHANT_FROM_ENABLED_EXCEPT("random_enchantment_from_enabled_except"),
        RANDOMLY_ENCHANT_FROM_LIST("random_enchantment_from_list");

        private String string;
        Mode(String string) {
            this.string = string;
        }

        public String getString() {
            return this.string;
        }
    }

    protected InjectAncientBookLootModifier(LootItemCondition[] conditionsIn,
                                            int priority,
                                            String mode,
                                            Optional<List<Holder<Enchantment>>> enchantments,
                                            int minRolls,
                                            int maxRolls) {
        super(conditionsIn, 0);
        this.mode = mode;
        this.enchantments = enchantments.orElse(List.of());
        this.minRolls = minRolls;
        this.maxRolls = maxRolls;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(@NotNull ObjectArrayList<ItemStack> generatedLoot, @NotNull LootContext context) {
        int rolls = context.getRandom().nextIntBetweenInclusive(minRolls, maxRolls);
        for (int i = 0; i < rolls; i++) {
            ItemStack ancientBook = new ItemStack(ModItems.ANCIENT_BOOK.get());

            if(mode.equals(Mode.RANDOMLY_ENCHANT_FROM_ENABLED.getString())) {
                EnchantmentUtil.setStoredEnchantment(ancientBook,
                        CostRegistry.server().getRandomEnchantment(context.getRandom()));

            } else if(mode.equals(Mode.RANDOMLY_ENCHANT_FROM_ENABLED_EXCEPT.getString())) {
                List<Holder<Enchantment>> applicableEnchantments = CostRegistry.server().getAllEnabledEnchantmentHolders();
                applicableEnchantments.removeAll(enchantments);
                int randomIndex = context.getRandom().nextInt(applicableEnchantments.size());

                EnchantmentUtil.setStoredEnchantment(ancientBook,
                        applicableEnchantments.get(randomIndex));

            } else if (mode.equals(Mode.RANDOMLY_ENCHANT_FROM_LIST.getString())) {
                int randomIndex = context.getRandom().nextInt(enchantments.size());

                EnchantmentUtil.setStoredEnchantment(ancientBook,
                        enchantments.get(randomIndex));
            }

            generatedLoot.add(ancientBook);
        }


        return generatedLoot;
    }

}
