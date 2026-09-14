package me.alfie.immersiveenchanting.item;

import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.compat.CompatHooks;
import me.alfie.immersiveenchanting.config.ClientConfig;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class AncientBook extends Item {

    public AncientBook(Properties properties) {
        super(properties
                .stacksTo(16)
                .rarity(Rarity.UNCOMMON));
    }

    @Override
    public boolean isFoil(@NotNull ItemStack itemStack) {
        return true;
    }

    public static void makeTooltip(ItemStack stack, Consumer<Component> componentOps) {
        ItemEnchantments enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);

        if (enchantments != null) {
            addTooltipForEnchantments(enchantments, componentOps);
        }

        if(EnchantmentUtil.isReplicated(stack)) componentOps.accept(
                Component.translatable("item.immersiveenchanting.ancient_book.desc.replicated")
                        .withStyle(ChatFormatting.GRAY)
        );
    }

    private static void addTooltipForEnchantments(ItemEnchantments enchantData, Consumer<Component> componentOps) {
        if (enchantData.keySet().size() == 1) {
            forSingleEnchantment(enchantData, componentOps);
        } else {
            forMultipleEnchantments(enchantData, componentOps);
        }
    }

    private static void forSingleEnchantment(ItemEnchantments enchantData, Consumer<Component> componentOps) {
        Holder<Enchantment> holder = enchantData.keySet().iterator().next();
        MutableComponent component = Component.empty();
        component
                .append(Component.translatable("item.immersiveenchanting.ancient_book.desc.enchantment"))
                .append(" ")
                .append(holder.value().description());
        componentOps.accept(component.withStyle(ChatFormatting.GOLD));

        garnish(enchantData, holder, componentOps, false);
    }

    private static void forMultipleEnchantments(ItemEnchantments enchantData, Consumer<Component> componentOps) {
        componentOps.accept(Component.translatable("item.immersiveenchanting.ancient_book.desc.enchantment_multiple").withStyle(ChatFormatting.GOLD));
        for (Holder<Enchantment> holder : enchantData.keySet()) {
            MutableComponent component = Component.empty();
            component
                    .append("- ")
                    .append(holder.value().description());
            componentOps.accept(component.withStyle(ChatFormatting.GOLD));

            garnish(enchantData, holder, componentOps, true);
        }
    }

    private static void garnish(ItemEnchantments enchantData, Holder<Enchantment> holder, Consumer<Component> componentOps, boolean addSpace) {
        if (ClientConfig.isShowAddedByTooltipEnabled()) {
            String modName = ImmersiveEnchanting.getModName(holder.getKey()
                    .location()
                    .getNamespace());

            componentOps.accept(
                    Component.literal(addSpace ? "- " : "").append(
                    Component.translatable("item.immersiveenchanting.ancient_book.desc.enchantment_added_by", modName))
                            .withStyle(ChatFormatting.BLUE)
            );
        }

        CompatHooks.EnchantmentDescriptions.addEnchantmentDescription(holder, enchantData.getLevel(holder), componentOps);
    }
}
