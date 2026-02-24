package me.alfie.immersiveenchanting.networking.packets;

import me.alfie.immersiveenchanting.networking.ClientPayloadHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class UnlockedEnchantmentsPacket {

    /**
     * Parallel lists mapping each enchantment key to its max unlocked level.
     * A maxLevel of Integer.MAX_VALUE means all levels are unlocked.
     */
    public final List<ResourceKey<Enchantment>> enchantments;
    public final List<Integer> maxLevels;

    public UnlockedEnchantmentsPacket(List<ResourceKey<Enchantment>> enchantments, List<Integer> maxLevels) {
        this.enchantments = enchantments;
        this.maxLevels = maxLevels;
    }

    public static void encode(UnlockedEnchantmentsPacket packet, FriendlyByteBuf buf) {
        buf.writeCollection(packet.enchantments, FriendlyByteBuf::writeResourceKey);
        buf.writeCollection(packet.maxLevels, FriendlyByteBuf::writeInt);
    }

    public static UnlockedEnchantmentsPacket decode(FriendlyByteBuf buf) {
        List<ResourceKey<Enchantment>> enchantments = buf.readList(b -> b.readResourceKey(Registries.ENCHANTMENT));
        List<Integer> maxLevels = buf.readList(FriendlyByteBuf::readInt);
        return new UnlockedEnchantmentsPacket(enchantments, maxLevels);
    }

    public static void handle(UnlockedEnchantmentsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(
                () -> {
                    ClientPayloadHandler.onUnlockedEnchantments(packet, contextSupplier.get());
                });
        contextSupplier.get().setPacketHandled(true);
    }
}
