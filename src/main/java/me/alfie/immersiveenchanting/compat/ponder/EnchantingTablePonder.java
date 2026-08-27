package me.alfie.immersiveenchanting.compat.ponder;

import me.alfie.alfinolib.util.ResourceId;
import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.CostRegistry;
import me.alfie.immersiveenchanting.item.ModItems;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class EnchantingTablePonder implements PonderPlugin {

    @Override
    public String getModId() {
        return ImmersiveEnchanting.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(ResourceId.parse("minecraft:enchanting_table").mc())
                .addStoryBoard("enchanting_table", EnchantingTablePonder::storyboard);
    }

    private static void storyboard(SceneBuilder builder, SceneBuildingUtil util) {
        builder.title("enchanting_table", "Enchanting");
        builder.configureBasePlate(0, 0, 5);
        builder.showBasePlate();

        builder.world().showSection(util.select().position(2, 1, 2), Direction.DOWN);
        builder.overlay().showOutlineWithText(util.select().position(2,1,2), 60)
                .text("Immersive Enchanting changes enchanting mechanics")
                .colored(PonderPalette.BLUE);
        builder.idle(60);

        builder.addLazyKeyframe();
        for (int i = 0; i < 3; i++) {
            builder.world().showSection(util.select().column(4, i+1), Direction.DOWN);
            builder.idle(5);
        }

        builder.overlay().showOutlineWithText(util.select().everywhere(), 60)
                .text("Place Chiseled Bookshelves (not regular bookshelves!) in the same arrangement as vanilla")
                .colored(PonderPalette.BLUE);

        for (int i = 0; i < 3; i++) {
            builder.world().showSection(util.select().column(3-i, 4), Direction.DOWN);
            builder.idle(5);
        }
        for (int i = 0; i < 3; i++) {
            builder.world().showSection(util.select().column(0, 3-i), Direction.DOWN);
            builder.idle(5);
        }
        for (int i = 0; i < 3; i++) {
            builder.world().showSection(util.select().column(i+1, 0), Direction.DOWN);
            builder.idle(5);
        }
        builder.idle(30);

        builder.world().hideSection(util.select().fromTo(0, 1, 1, 0, 2, 3), Direction.WEST);
        builder.world().hideSection(util.select().fromTo(1, 1, 0, 3, 2, 0), Direction.NORTH);

        builder.idle(10);
        builder.addLazyKeyframe();
        builder.idle(10);


        builder.overlay().showOutlineWithText(util.select().position(1,2,4), 90)
                .text("Ancient Books can be found throughout the world. Place them into your Chiseled Bookshelves")
                .colored(PonderPalette.BLUE);
        builder.idle(30);

        ItemStack book = new ItemStack(ModItems.ANCIENT_BOOK.get());
        EnchantmentUtil.setStoredEnchantment(book, CostRegistry.client().getAllEnchantmentHolders().getFirst());
        builder.overlay().showControls(util.select().position(1, 2, 4).getCenter(), Pointing.DOWN, 60)
                .withItem(new ItemStack(ModItems.ANCIENT_BOOK.get())).rightClick();
        builder.idle(75);

        builder.overlay().showOutlineWithText(util.select().position(2, 1, 2), 60)
                .text("Ancient Books unlock enchantments in the table, which can be crafted using a specific ingredient")
                .colored(PonderPalette.BLUE);
        builder.idle(60);
    }
}
