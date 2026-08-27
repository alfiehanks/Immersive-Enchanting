package me.alfie.immersiveenchanting.compat;

import me.alfie.immersiveenchanting.compat.ench_desc.EnchDescCompat;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;

public final class ModCompat {

    public enum Mods {
        ENCHANTMENT_DESCRIPTIONS("enchdesc");

        private final String modid;

        Mods(String modid) {
            this.modid = modid;
        }

        public String modid() {
            return modid;
        }
    }

    public static boolean isModLoaded(String modid) {
        return ModList.get().isLoaded(modid);
    }

    public static boolean isModLoaded(Mods mod) {
        return isModLoaded(mod.modid());
    }

    public static void registerCompatEvents(IEventBus modEventBus) {
        if(isModLoaded(ModCompat.Mods.ENCHANTMENT_DESCRIPTIONS)) modEventBus.addListener(EnchDescCompat::registerTooltipExtensions);
    }
}
