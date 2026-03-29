package me.alfie.immersiveenchanting.networking;

import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.networking.packet.enchantitem.EnchantItemPacket;
import me.alfie.immersiveenchanting.networking.packet.enchantmentcostregistrysync.EnchantmentCostRegistrySyncPacket;
import me.alfie.immersiveenchanting.networking.packet.removeenchantment.RemoveEnchantmentPacket;
import me.alfie.immersiveenchanting.networking.packet.replicatebookpacket.ReplicateBookPacket;
import me.alfie.immersiveenchanting.networking.packet.transmutebookpacket.TransmuteBookPacket;
import me.alfie.immersiveenchanting.networking.packet.unlockedenchantments.UnlockedEnchantmentsPacket;
import me.alfie.immersiveenchanting.networking.packet.updatetoolslot.UpdateToolSlotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;


public class ModPackets {
    //Client to server: ModPackets.INSTANCE.sendToServer(new MyPacket(123));
    //Server to client: ModPackets.INSTANCE.send(
    //    PacketDistributor.PLAYER.with(() -> serverPlayer),
    //    new MyPacket(456)
    //);

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ImmersiveEnchanting.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        INSTANCE.registerMessage(
                id++,
                EnchantItemPacket.class,
                EnchantItemPacket::encode,
                EnchantItemPacket::decode,
                EnchantItemPacket::handle
        );

        INSTANCE.registerMessage(
                id++,
                EnchantmentCostRegistrySyncPacket.class,
                EnchantmentCostRegistrySyncPacket::encode,
                EnchantmentCostRegistrySyncPacket::decode,
                EnchantmentCostRegistrySyncPacket::handle
        );

        INSTANCE.registerMessage(
                id++,
                RemoveEnchantmentPacket.class,
                RemoveEnchantmentPacket::encode,
                RemoveEnchantmentPacket::decode,
                RemoveEnchantmentPacket::handle
        );

        INSTANCE.registerMessage(
                id++,
                TransmuteBookPacket.class,
                TransmuteBookPacket::encode,
                TransmuteBookPacket::decode,
                TransmuteBookPacket::handle
        );

        INSTANCE.registerMessage(
                id++,
                ReplicateBookPacket.class,
                ReplicateBookPacket::encode,
                ReplicateBookPacket::decode,
                ReplicateBookPacket::handle
        );

        INSTANCE.registerMessage(
                id++,
                UnlockedEnchantmentsPacket.class,
                UnlockedEnchantmentsPacket::encode,
                UnlockedEnchantmentsPacket::decode,
                UnlockedEnchantmentsPacket::handle
        );

        INSTANCE.registerMessage(
                id++,
                UpdateToolSlotPacket.class,
                UpdateToolSlotPacket::encode,
                UpdateToolSlotPacket::decode,
                UpdateToolSlotPacket::handle
        );

        //... register more packets.
    }
}
