package me.alfie.immersiveenchanting;

import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import me.alfie.immersiveenchanting.block.ModBlocks;
import me.alfie.immersiveenchanting.config.ClientConfig;
import me.alfie.immersiveenchanting.config.ServerConfig;
import me.alfie.immersiveenchanting.creativetab.ModCreativeTab;
import me.alfie.immersiveenchanting.datapack.EnchantmentCostDatapackHandler;
import me.alfie.immersiveenchanting.gui.ModMenus;
import me.alfie.immersiveenchanting.item.ModItems;
import me.alfie.immersiveenchanting.lootmodifier.ModLootModifiers;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.Optional;

@Mod(ImmersiveEnchanting.MODID)
public class ImmersiveEnchanting {
    public static final String MODID = "immersiveenchanting";
    public static final Logger LOGGER = LogUtils.getLogger();

    protected static final EnchantmentCostDatapackHandler ENCHANTMENT_COST_DATAPACK_HANDLER = new EnchantmentCostDatapackHandler(
            new Gson(), EnchantmentCostDatapackHandler.DIRECTORY);

    public ImmersiveEnchanting(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        ImmersiveEnchantingEvents events = new ImmersiveEnchantingEvents();
        modEventBus.addListener(events::onClientStart);
        modEventBus.addListener(events::buildCreativeTab);
        modEventBus.addListener(events::commonSetup);
        MinecraftForge.EVENT_BUS.register(events);

        ModItems.register(modEventBus);
        ModMenus.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        ModCreativeTab.register(modEventBus);
        ModBlocks.register(modEventBus);

        context.registerConfig(ModConfig.Type.CLIENT, ClientConfig.CONFIG_SPEC);
        context.registerConfig(ModConfig.Type.COMMON, ServerConfig.CONFIG_SPEC);
    }

    public static HolderLookup<Enchantment> getEnchantmentHolderLookup(RegistryAccess access) {
        return getEnchantmentRegistry(access).asLookup();
    }

    /**
     * Return the enchantment registry from a RegistryAccess.
     *
     * @param access
     * @return
     */
    public static Registry<Enchantment> getEnchantmentRegistry(RegistryAccess access) {
        return access.registryOrThrow(Registries.ENCHANTMENT);
    }

    /**
     * Get an enchantment holder using a holder lookup.
     *
     * @param lookup
     * @param enchantmentResourceId
     * @return
     */
    private static Optional<Holder.Reference<Enchantment>> getEnchantmentHolder(HolderLookup<Enchantment> lookup, String enchantmentResourceId) {
        try {
            ResourceLocation location = ResourceLocation.tryParse(enchantmentResourceId);
            if (location == null) return Optional.empty();

            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, location);
            return lookup.get(key);
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Automatically get an enchantment holder using RegistryAccess.
     *
     * @param access
     * @param enchantment
     * @return
     */
    public static Optional<Holder.Reference<Enchantment>> getEnchantmentHolder(RegistryAccess access, ResourceKey<Enchantment> enchantment) {
        return access.registryOrThrow(Registries.ENCHANTMENT).getHolder(enchantment);
    }

    public static ResourceLocation getEnchantmentHolderRL(Holder<Enchantment> enchantmentHolder) {
        return ResourceLocation.tryParse(enchantmentHolder.unwrapKey().get().location().toString());
    }

}
