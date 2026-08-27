package me.alfie.immersiveenchanting;

import com.mojang.logging.LogUtils;
import me.alfie.alfinolib.datapacks.DatapackRegistry;
import me.alfie.immersiveenchanting.block.ModBlocks;
import me.alfie.immersiveenchanting.compat.ponder.EnchantingTablePonder;
import me.alfie.immersiveenchanting.config.ClientConfig;
import me.alfie.immersiveenchanting.config.ServerConfig;
import me.alfie.immersiveenchanting.creativetab.ModCreativeTab;
import me.alfie.immersiveenchanting.datacomponent.ModDataComponents;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.CostDatapack;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.codec.CostData;
import me.alfie.immersiveenchanting.datapack.mod_icons.ModIconsDatapack;
import me.alfie.immersiveenchanting.datapack.node_sounds.NodeSoundsDatapack;
import me.alfie.immersiveenchanting.event.ModEvents;
import me.alfie.immersiveenchanting.gui.ModMenus;
import me.alfie.immersiveenchanting.item.ModItems;
import me.alfie.immersiveenchanting.loot.ModGlobalLootModifiers;
import me.alfie.immersiveenchanting.sound.ModSounds;
import me.alfie.immersiveenchanting.structure.ModStructureProcessors;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import org.slf4j.Logger;

@Mod(ImmersiveEnchanting.MODID)
public class ImmersiveEnchanting {
    public static final String MODID = "immersiveenchanting";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ImmersiveEnchanting(IEventBus modEventBus, ModContainer modContainer) {
        ModEvents.register(modEventBus);
        ModMenus.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModSounds.register(modEventBus);
        ModCreativeTab.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModGlobalLootModifiers.register(modEventBus);
        ModStructureProcessors.register(modEventBus);

        DatapackRegistry.register(CostDatapack.DEFINITION, CostDatapack::new);
        DatapackRegistry.register(NodeSoundsDatapack.DEFINITION, NodeSoundsDatapack::new);
        DatapackRegistry.register(ModIconsDatapack.DEFINITION, ModIconsDatapack::new);

        modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CONFIG_SPEC);
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.CONFIG_SPEC);


    }

    /**
     * Returns a copy of the component styled with the galactic alphabet font.
     */
    public static Component styleWithAltFont(Component component) {
        ResourceLocation fontStyle = ResourceLocation.withDefaultNamespace("alt");
        return component.copy().withStyle(Style.EMPTY.withFont(fontStyle));
    }

    /**
     * Resolves a human-readable mod name from its namespace/mod ID.
     * Falls back to the raw namespace if the mod is not loaded or has no display name,
     * and capitalizes the first character of the result.
     */
    public static String getModName(String namespace) {
        ModInfo modInfo = (ModInfo) ModList.get().getModContainerById(namespace)
                .map(ModContainer::getModInfo)
                .orElse(null);

        String name = modInfo != null ? modInfo.getDisplayName() : namespace;

        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
