package me.alfie.immersiveenchanting.networking;

import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.datapack.EnchantmentCostRegistry;
import me.alfie.immersiveenchanting.gui.EnchantingTableMenu;
import me.alfie.immersiveenchanting.networking.packets.EnchantmentCostRegistrySyncPacket;
import me.alfie.immersiveenchanting.networking.packets.UnlockedEnchantmentsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Server -> Client
 */
@OnlyIn(Dist.CLIENT)
public class ClientPayloadHandler {

    /**
     * Receive the enchantment cost registry from the server and store on the client.
     *
     * @param packet
     * @param context
     */
    public static void onEnchantmentCostRegistrySync(final EnchantmentCostRegistrySyncPacket packet, final NetworkEvent.Context context) {
        //Build a SerializedEnchantmentCostRegistry
        SerializedEnchantmentCostRegistry serializedRegistry = new SerializedEnchantmentCostRegistry(
                packet.enchantmentNamespaces,
                packet.levels,
                packet.itemIds,
                packet.amounts,
                packet.lapisCostItemId,
                packet.lapisCostAmount
        );

        EnchantmentCostRegistry.setClientRegistry(
                EnchantmentCostRegistrySyncPacket.deserialize(serializedRegistry, packet.xpCostMode)
        );
    }

    /**
     * Send which enchantments are unlocked to the client.
     *
     * @param packet
     * @param context
     */
    public static void onUnlockedEnchantments(final UnlockedEnchantmentsPacket packet, final NetworkEvent.Context context) {
        LocalPlayer player = Minecraft.getInstance().player;

        Map<Holder<Enchantment>, Integer> unlockedEnchantments = new HashMap<>();
        for (int i = 0; i < packet.enchantments.size(); i++) {
            ResourceKey<Enchantment> enchantmentKey = packet.enchantments.get(i);
            int maxLevel = packet.maxLevels.get(i);
            ImmersiveEnchanting.getEnchantmentHolder(player.level().registryAccess(), enchantmentKey)
                    .ifPresent(holder -> unlockedEnchantments.put(holder, maxLevel));
        }

        if (player.containerMenu instanceof EnchantingTableMenu menu) {
            menu.setUnlockedEnchantments(unlockedEnchantments);
        }
    }
}
