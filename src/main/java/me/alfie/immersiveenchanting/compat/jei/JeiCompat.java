package me.alfie.immersiveenchanting.compat.jei;

import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.CostRegistry;
import me.alfie.immersiveenchanting.item.ModItems;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.vanilla.IJeiIngredientInfoRecipe;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public final class JeiCompat implements IModPlugin {

    public static final RecipeType<JeiEnchantmentCosts> ENCHANTMENT_COSTS = RecipeType.create(
            ImmersiveEnchanting.MODID,
            "enchanting",
            JeiEnchantmentCosts.class
    );



    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ImmersiveEnchanting.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new JeiEnchantingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        //Client registry seems safe to access here
        CostRegistry costRegistry = CostRegistry.client();

        List<JeiEnchantmentCosts> jeiEnchantmentCosts = new ArrayList<>();
        for (Holder<Enchantment> enchantmentHolder : costRegistry.getAllEnchantmentHolders()) {
            JeiEnchantmentCosts jeiCost = new JeiEnchantmentCosts(enchantmentHolder, costRegistry.get(enchantmentHolder));
            jeiEnchantmentCosts.add(jeiCost);
        }

        registration.addRecipes(ENCHANTMENT_COSTS, jeiEnchantmentCosts);

        registration.addItemStackInfo(new ItemStack(Items.ENCHANTING_TABLE), Component.translatable("immersiveenchanting.jei.enchanting_table_info"));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Items.ENCHANTING_TABLE), ENCHANTMENT_COSTS);
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(ModItems.ANCIENT_BOOK.get(), new ISubtypeInterpreter<ItemStack>() {
            @Override
            public @Nullable Object getSubtypeData(ItemStack itemStack, UidContext uidContext) {
                return EnchantmentUtil.getStoredEnchantment(itemStack);
            }

            //Deprecated
            @Override public @NotNull String getLegacyStringSubtypeInfo(ItemStack itemStack, UidContext uidContext) {return "";}
        });
    }
}