package me.alfie.immersiveenchanting.compat.jei;

import me.alfie.alfinolib.gui.GuiGraphicsX;
import me.alfie.alfinolib.gui.util.GuiGraphicsApi;
import me.alfie.alfinolib.gui.util.MousePos;
import me.alfie.alfinolib.util.ResourceId;
import me.alfie.immersiveenchanting.api.node.SpriteIcon;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.codec.Cost;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.codec.CostHolder;
import me.alfie.immersiveenchanting.gui.core.Sprite;
import me.alfie.immersiveenchanting.item.ModItems;
import me.alfie.immersiveenchanting.util.EnchantmentTextureHelper;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.common.gui.textures.JeiSpriteUploader;
import mezz.jei.gui.elements.IconButton;
import mezz.jei.library.gui.helpers.GuiHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.GuiSpriteManager;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JeiEnchantingCategory implements IRecipeCategory<JeiEnchantmentCosts> {
    private final IGuiHelper guiHelper;

    private static final int START_Y = 16;
    private static final int ROW_HEIGHT = 18;

    private final int ARROW_CENTER_X;
    private final int COST_X;
    private final int ENCHANTMENT_X;

    public JeiEnchantingCategory(IGuiHelper guiHelper) {
        this.guiHelper = guiHelper;
        ARROW_CENTER_X = (getWidth()/2 - Sprite.JEI_ENCHANTING_ARROW.width()/2);

        int spriteCenter = (getWidth()/2 - 16/2);
        COST_X = spriteCenter - Sprite.JEI_ENCHANTING_ARROW.width()/2 - 16;
        ENCHANTMENT_X = spriteCenter + Sprite.JEI_ENCHANTING_ARROW.width()/2 + 16;
    }


    @Override
    public @NotNull RecipeType<JeiEnchantmentCosts> getRecipeType() {
        return JeiCompat.ENCHANTMENT_COSTS;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("immersiveenchanting.tab.enchanting");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return guiHelper.createDrawableItemStack(new ItemStack(Items.ENCHANTING_TABLE));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, JeiEnchantmentCosts jeiCost, IFocusGroup focus) {
        int y = START_Y;
        for (int level = 1; level < jeiCost.costData().levelCosts().maxLevel()+1; level++) {
            CostHolder costHolder = jeiCost.costData().levelCosts().getLevel(level);

            for (Cost cost : costHolder.costs()) {
                builder.addInputSlot(COST_X, y).addItemStacks(cost.getItemStacks());
            }

            y += ROW_HEIGHT;
        }

        ItemStack book = new ItemStack(ModItems.ANCIENT_BOOK.get());
        EnchantmentUtil.setStoredEnchantment(book, jeiCost.enchantmentHolder());

        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST).addItemStack(book);
        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST).addItemStack(new ItemStack(Items.ENCHANTING_TABLE));
    }

    @Override
    public int getWidth() {
        return 160;
    }

    @Override
    public int getHeight() {
        return 160;
    }

    @Override
    public void draw(JeiEnchantmentCosts recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        //Using AlfinoLib
        GuiGraphicsX gx = new GuiGraphicsX(guiGraphics);
        MousePos mousePos = new MousePos((int) mouseX, (int) mouseY);

        //Draw background
        gx.graphics().setColor(0.4f, 0.4f, 0.4f, 1);
        for (int x = 0; x < getWidth()/Sprite.BACKGROUND_TILE.width(); x++) {
            for (int y = 0; y < getHeight()/Sprite.BACKGROUND_TILE.height(); y++) {
                GuiGraphicsApi.blit(gx,
                        Sprite.BACKGROUND_TILE.id(),
                        x * Sprite.BACKGROUND_TILE.width(),
                        y * Sprite.BACKGROUND_TILE.height(),
                        Sprite.BACKGROUND_TILE.width(), Sprite.BACKGROUND_TILE.height());
            }
        }

        GuiGraphicsApi.blit(gx, Sprite.ENCHANTING_TABLE_TOP.id(),
                getWidth()/2-Sprite.ENCHANTING_TABLE_TOP.width()/2,
                getHeight()/2-Sprite.ENCHANTING_TABLE_TOP.height()/2,
                Sprite.ENCHANTING_TABLE_TOP.width(),
                Sprite.ENCHANTING_TABLE_TOP.height());

        GuiGraphicsApi.blit(gx, Sprite.BOOK_OPEN.id(),
                getWidth()/2-Sprite.BOOK_OPEN.width()/2,
                getHeight()/2-Sprite.BOOK_OPEN.height()/2,
                Sprite.BOOK_OPEN.width(),
                Sprite.BOOK_OPEN.height());
        gx.graphics().setColor(1,1,1,1);

        Component enchantmentName = recipe.enchantmentHolder().value().description();

        if(!recipe.costData().enabled()) {
            enchantmentName = enchantmentName.copy().withStyle(ChatFormatting.RED).withStyle(ChatFormatting.STRIKETHROUGH);
        }

        int textCenterX = (getWidth()/2 - Minecraft.getInstance().font.width(enchantmentName)/2);
        GuiGraphicsApi.text(gx, Minecraft.getInstance().font, enchantmentName, textCenterX, 4, true);


        for (int i = 0; i < recipe.costData().levelCosts().maxLevel(); i++) {
            int y = START_Y + i * ROW_HEIGHT;

            //Center Arrows
            GuiGraphicsApi.blit(gx,
                    Sprite.JEI_ENCHANTING_ARROW.id(),
                    ARROW_CENTER_X,
                    y + Sprite.JEI_ENCHANTING_ARROW.height()/2,
                    Sprite.JEI_ENCHANTING_ARROW.width(), Sprite.JEI_ENCHANTING_ARROW.height());

            //Enchantment Icon
            ResourceId icon = EnchantmentTextureHelper.getTexture(EnchantmentUtil.toId(recipe.enchantmentHolder()));
            GuiGraphicsApi.blit(gx,
                    icon,
                    ENCHANTMENT_X,
                    y,
                    16, 16
            );

            //Draw numeral
            GuiGraphicsApi.text(gx, Minecraft.getInstance().font, Component.literal(toRoman(i+1)), ENCHANTMENT_X + 18, y + 4, true);
        }


    }

    private static String toRoman(int number) {
        String[] numerals = {
                "M", "CM", "D", "CD",
                "C", "XC", "L", "XL",
                "X", "IX", "V", "IV", "I"
        };

        int[] values = {
                1000, 900, 500, 400,
                100, 90, 50, 40,
                10, 9, 5, 4, 1
        };

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < values.length; i++) {
            while (number >= values[i]) {
                number -= values[i];
                result.append(numerals[i]);
            }
        }

        return result.toString();
    }



}