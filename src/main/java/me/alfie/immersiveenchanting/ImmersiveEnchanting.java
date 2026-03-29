package me.alfie.immersiveenchanting;

import com.mojang.logging.LogUtils;
import me.alfie.immersiveenchanting.block.ModBlocks;
import me.alfie.immersiveenchanting.config.ClientConfig;
import me.alfie.immersiveenchanting.config.ServerConfig;
import me.alfie.immersiveenchanting.creativetab.ModCreativeTab;
import me.alfie.immersiveenchanting.events.ModEvents;
import me.alfie.immersiveenchanting.gui.ModMenus;
import me.alfie.immersiveenchanting.item.ModItems;
import me.alfie.immersiveenchanting.lootmodifier.ModLootModifiers;
import me.alfie.immersiveenchanting.networking.ModPackets;
import me.alfie.immersiveenchanting.sound.ModSounds;
import me.alfie.immersiveenchanting.structure.ModStructureProcessors;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ImmersiveEnchanting.MODID)
public class ImmersiveEnchanting {
    public static final String MODID = "immersiveenchanting";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ImmersiveEnchanting() {
        ModLoadingContext context = ModLoadingContext.get();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEvents.register(modEventBus);
        ModPackets.register();

        ModSounds.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModCreativeTab.register(modEventBus);

        ModMenus.register(modEventBus);

        ModLootModifiers.register(modEventBus);
        ModStructureProcessors.register(modEventBus);

        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CONFIG_SPEC);
        context.registerConfig(ModConfig.Type.SERVER, ServerConfig.CONFIG_SPEC);
    }

    public static Component styleWithAltFont(Component component) {
        ResourceLocation fontStyle = new ResourceLocation("alt");
        MutableComponent styledComponent = component.copy().withStyle(Style.EMPTY.withFont(fontStyle));
        return styledComponent;
    }
}
