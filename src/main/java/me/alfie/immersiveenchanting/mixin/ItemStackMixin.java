package me.alfie.immersiveenchanting.mixin;

import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.compat.CompatHooks;
import me.alfie.immersiveenchanting.compat.ModCompat;
import me.alfie.immersiveenchanting.compat.ench_desc.EnchDescCompat;
import me.alfie.immersiveenchanting.config.ClientConfig;
import me.alfie.immersiveenchanting.item.ModItems;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import net.darkhax.enchdesc.common.impl.Config;
import net.darkhax.enchdesc.common.impl.EnchdescMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * Mixin that customizes tooltip rendering for {@link ItemStack}.
 *
 * <p>Overrides how stored enchantments are displayed for specific modded items,
 * particularly {@link ModItems#ANCIENT_BOOK}.</p>
 *
 * <p>Replaces vanilla enchantment tooltip formatting with a custom presentation
 * and optionally appends mod attribution and replication status.</p>
 *
 * <p>This is purely client-side presentation logic and does not modify item data.</p>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    /**
     * Overrides tooltip rendering for stored enchantments on ancient books.
     *
     * <p>When the item is an ancient book, the vanilla {@code STORED_ENCHANTMENTS}
     * tooltip is replaced with a custom formatted display.</p>
     *
     * <p>Each enchantment is rendered with a custom label and styled output.
     * Optionally appends the originating mod name if enabled in client config.</p>
     *
     * <p>Also displays replication status when applicable.</p>
     *
     * <p>This injection cancels vanilla tooltip processing for stored enchantments
     * on affected items.</p>
     *
     * @param type data component type being processed
     * @param context tooltip rendering context
     * @param consumer output consumer for tooltip lines
     * @param flag tooltip visibility flags
     */
    @Inject(
            method = "addToTooltip(Lnet/minecraft/core/component/DataComponentType;Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/function/Consumer;Lnet/minecraft/world/item/TooltipFlag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private <T extends net.minecraft.world.item.component.TooltipProvider>
    void immersiveenchanting$addToTooltip(DataComponentType<T> type,
                                          Item.TooltipContext context,
                                          Consumer<Component> consumer,
                                          TooltipFlag flag,
                                          CallbackInfo ci) {
        ItemStack self = (ItemStack)(Object)this;
        if (type == DataComponents.STORED_ENCHANTMENTS && self.is(ModItems.ANCIENT_BOOK.get())) {
            ItemEnchantments enchantments = self.get(DataComponents.STORED_ENCHANTMENTS);

            MutableComponent component = Component.empty();
            if (enchantments != null) {
                for(Holder<Enchantment> enchantmentHolder : enchantments.keySet()) {
                    component
                            .append(Component.translatable("item.immersiveenchanting.ancient_book.desc.enchantment"))
                            .append(" ")
                            .append(enchantmentHolder.value().description());
                    consumer.accept(component.withStyle(ChatFormatting.GOLD));

                    if(ClientConfig.isShowAddedByTooltipEnabled()) {
                        String modName = ImmersiveEnchanting.getModName(enchantmentHolder.getKey()
                                .location()
                                .getNamespace());

                        consumer.accept(
                                Component.translatable("item.immersiveenchanting.ancient_book.desc.enchantment_added_by", modName)
                                        .withStyle(ChatFormatting.BLUE)
                        );
                    }

                    CompatHooks.EnchantmentDescriptions.addEnchantmentDescription(enchantmentHolder, enchantments.getLevel(enchantmentHolder), consumer);
                }
            }

            if(EnchantmentUtil.isReplicated(self)) consumer.accept(
                    Component.translatable("item.immersiveenchanting.ancient_book.desc.replicated")
                            .withStyle(ChatFormatting.GRAY)
            );



            ci.cancel(); //Prevent DataComponents.STORED_ENCHANTMENTS tooltip being applied normally to ancient books.
        }
    }
}
