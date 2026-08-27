package me.alfie.immersiveenchanting;

import me.alfie.immersiveenchanting.client.EnchantingTableItemRenderer;
import me.alfie.immersiveenchanting.compat.ponder.EnchantingTablePonder;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = ImmersiveEnchanting.MODID, dist = Dist.CLIENT)
public class ImmersiveEnchantingClient {

    public ImmersiveEnchantingClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        PonderIndex.addPlugin(new EnchantingTablePonder());
    }

    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BlockEntityType.ENCHANTING_TABLE, EnchantingTableItemRenderer::new);
    }
}