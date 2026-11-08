package me.alfie.immersiveenchanting.networking;

import me.alfie.alfinolib.networking.NetworkPacket;
import me.alfie.alfinolib.networking.codec.StreamCodec;
import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.CostRegistry;
import me.alfie.immersiveenchanting.gui.EnchantingTableMenu;
import me.alfie.immersiveenchanting.util.CostHelper;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import me.alfie.immersiveenchanting.util.FxHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Client-to-server packet requesting enchantment transmutation.
 *
 * <p>Replaces the stored enchantment on an ancient book with a new randomly selected one,
 * excluding currently available bookshelf enchantments where applicable, and excluding any
 * enchantment whose cost data marks it as not {@code transmutable} (see {@link me.alfie.immersiveenchanting.datapack.enchantment_cost.codec.CostData#isTransmutable()}).</p>
 *
 * <p>The operation is validated server-side and requires:
 * <ul>
 *     <li>Transmutation to be enabled in configuration</li>
 *     <li>Valid cost and fuel requirements</li>
 *     <li>A valid target enchantment selection</li>
 * </ul>
 *
 * <p>On success, the item is updated, feedback is shown to the player, and
 * visual/audio effects are played at the enchanting table.</p>
 */
public record TransmutePacket() implements NetworkPacket<TransmutePacket> {

    public static final Type<@NotNull TransmutePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImmersiveEnchanting.MODID, "transmute"));
    @Override public Type<@NotNull TransmutePacket> type() {
        return TYPE;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, TransmutePacket> STREAM_CODEC = StreamCodec.unit(new TransmutePacket());

    @Override
    public void exec(IPayloadContext context) {
        Player player = context.player();
        Level level = player.level();
        if(!(player.containerMenu instanceof EnchantingTableMenu menu)) return;

        ItemStack ancientBookStack = menu.getToolSlot().getItem();
        List<Holder<Enchantment>> availableEnchantments = menu.getAvailableEnchantments();
        List<Holder<Enchantment>> allEnchantments = CostRegistry.server().getAllTransmutableEnchantmentHolders();

        allEnchantments.removeIf(holder ->
                availableEnchantments.stream().anyMatch(av -> av.value().equals(holder.value())));
        if(allEnchantments.isEmpty()) allEnchantments = CostRegistry.server().getAllTransmutableEnchantmentHolders();

        RandomSource random = level.getRandom();
        int randomIndex = random.nextInt(allEnchantments.size());
        Holder<Enchantment> newEnchantment = allEnchantments.get(randomIndex);
        Holder<Enchantment> oldEnchantment = EnchantmentUtil.getStoredEnchantment(ancientBookStack);

        if(CostHelper.canTransmute(menu, oldEnchantment, player)) {
            EnchantmentUtil.setStoredEnchantment(ancientBookStack, newEnchantment);
            BlockPos tablePos = menu.getBlockPos();

            ItemStack newBookStack = menu.getToolSlot().getItem().copyAndClear();
            menu.getToolSlot().setChanged();

            ItemEntity itemEntity = new ItemEntity(
                    level,
                    tablePos.getX() + 0.5,
                    tablePos.getY() + 1,
                    tablePos.getZ() + 0.5,
                    newBookStack);
            itemEntity.setPickUpDelay(40);
            itemEntity.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(itemEntity);

            Component newEnchantmentName = newEnchantment.value().description().copy().withStyle(ChatFormatting.GOLD);
            Component actionBarMessage = Component.translatable("immersiveenchanting.action_bar.transmute_success",
                    newEnchantmentName).withStyle(ChatFormatting.GRAY);

            player.sendSystemMessage(actionBarMessage);
            player.closeContainer();

            FxHelper.playTransmute((ServerLevel) level, tablePos);
        }
    }
}
