package me.alfie.immersiveenchanting.compat.ench_desc;

import me.alfie.alfinolib.util.ResourceId;
import me.alfie.immersiveenchanting.api.description.RegisterDescriptionLayoutEvent;
import me.alfie.immersiveenchanting.compat.ench_desc.mixin.EnchdescModAccessor;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import net.darkhax.enchdesc.common.impl.Config;
import net.darkhax.enchdesc.common.impl.EnchdescMod;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

public final class EnchDescCompat {

    public static void registerTooltipExtensions(RegisterDescriptionLayoutEvent event) {
        event.register(new EnchDescLayoutExtension());
    }

    public static Config config() {
        return ((EnchdescModAccessor) (Object) EnchdescMod.getInstance()).getConfig();
    }

    /**
     * Method adapted from EnchDescMod as methods not exposed.
     * @param enchantmentHolder Enchantment to get description for
     * @param level Level of enchantment
     * @return Component text (or Component.empty() if not found)
     */
    public static MutableComponent getDescription(Holder<Enchantment> enchantmentHolder, int level) {
        ResourceId id = EnchantmentUtil.toId(enchantmentHolder);

        MutableComponent desc = getDescription("enchantment." + id.namespace() + "." + id.path() + ".", level);

        if (desc.equals(Component.empty())) {
            ComponentContents contents = enchantmentHolder.value().description().getContents();
            if (contents instanceof TranslatableContents translatable) {
                desc = getDescription(translatable.getKey() + ".", level);
            }
        }

        return desc;
    }

    /**
     * Method adapted from EnchDescMod as methods not exposed.
     * Finds component from translation key, returns Component.emtpy() if not found
     */
    private static MutableComponent getDescription(String baseKey, int level) {
        final String[] KEY_TYPES = new String[]{"desc", "description", "info"};

        for(String keyType : KEY_TYPES) {
            String key = baseKey + keyType;
            if (I18n.exists(key)) {
                return Component.translatable(key);
            }

            key = key + "." + level;
            if (I18n.exists(key)) {
                return Component.translatable(key);
            }
        }

        return Component.empty();
    }
}
