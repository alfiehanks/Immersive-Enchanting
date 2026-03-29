package me.alfie.immersiveenchanting.datapack;

import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Client only!
 */
public class EnchantmentMetadataRegistry {
    private static final Map<ResourceLocation, ResourceLocation> ICONS = new HashMap<>();

    public static ResourceLocation getIconTexture(ResourceLocation enchantmentResourceLocation) {
        return ICONS.get(enchantmentResourceLocation);
    }

    public static Map<ResourceLocation, ResourceLocation> getIcons() {
        return ICONS;
    }

    /**
     * Load icons from client resources into ICONS map.
     * Uses resource pack.
     * @param resourceManager
     */
    public static void loadIcons(ResourceManager resourceManager) {
        ICONS.clear();

        // Iterate over all namespaces (mods) in assets/
        for (String namespace : resourceManager.getNamespaces()) {
            if (!namespace.equals("immersiveenchanting")) continue; // only your mod's assets

            Map<ResourceLocation, Resource> iconFiles = resourceManager.listResources(
                    "textures/gui/enchantment_icons", // relative to assets/immersiveenchanting/
                    rl -> rl.getPath().endsWith(".png")
            );

            for (ResourceLocation fileRL : iconFiles.keySet()) {
                // fileRL path: textures/gui/enchantment_icons/minecraft/power.png
                String path = fileRL.getPath();

                // Remove the base folder
                String relative = path.substring("textures/gui/enchantment_icons/".length()); // minecraft/power.png

                // Split namespace / file
                String[] parts = relative.split("/", 2);
                if (parts.length != 2) continue; // malformed path

                String enchantmentNamespace = parts[0]; // e.g., minecraft
                String fileName = parts[1]; // e.g., power.png

                String enchantmentPath = fileName.replace(".png", ""); // remove extension

                ResourceLocation enchantmentRL = new ResourceLocation(enchantmentNamespace, enchantmentPath);

                ICONS.put(enchantmentRL, fileRL);
            }
        }
        ImmersiveEnchanting.LOGGER.info("Loaded " + ICONS.size() + " enchantment icon textures.");
    }

}
