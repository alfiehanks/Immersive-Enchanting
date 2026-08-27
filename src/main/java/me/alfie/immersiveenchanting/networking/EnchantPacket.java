package me.alfie.immersiveenchanting.networking;

import me.alfie.alfinolib.networking.NetworkPacket;
import me.alfie.alfinolib.networking.codec.StreamCodec;
import me.alfie.alfinolib.util.ResourceId;
import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.compat.CompatHooks;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.CostRegistry;
import me.alfie.immersiveenchanting.gui.EnchantingTableMenu;
import me.alfie.immersiveenchanting.util.CostHelper;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import me.alfie.immersiveenchanting.util.FxHelper;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * Client-to-server packet requesting an enchantment to be applied.
 *
 * <p>Validates the request against server-side rules before applying:
 * <ul>
 *     <li>Enchantment compatibility</li>
 *     <li>Level progression rules</li>
 *     <li>Bookshelf availability</li>
 *     <li>Cost and fuel requirements</li>
 * </ul>
 *
 * <p>If valid, the enchantment is applied to the item and resources are consumed.
 * On success, visual and audio feedback is triggered on the client.</p>
 */
public record EnchantPacket(ResourceKey<Enchantment> enchantmentKey, int level) implements NetworkPacket<EnchantPacket> {

    public static final Type<@NotNull EnchantPacket> TYPE = new Type<>(new ResourceId(ImmersiveEnchanting.MODID, "enchant_item").mc());
    @Override public Type<@NotNull EnchantPacket> type() {
        return TYPE;
    }

    public static StreamCodec<RegistryFriendlyByteBuf, EnchantPacket> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, EnchantPacket>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, EnchantPacket packet) {
            ModPackets.ENCHANTMENT_CODEC.encode(buf, packet.enchantmentKey);
            buf.writeInt(packet.level);
        }

        @Override
        public EnchantPacket decode(RegistryFriendlyByteBuf buf) {
            return new EnchantPacket(ModPackets.ENCHANTMENT_CODEC.decode(buf), buf.readInt());
        }
    };

    @Override
    public void exec(IPayloadContext context) {
        Player player = context.player();
        if(!(player.containerMenu instanceof EnchantingTableMenu menu)) return;

        Holder<Enchantment> enchantmentHolder = EnchantmentUtil.toHolder(enchantmentKey,
                player.registryAccess());

        ItemStack stack = menu.getToolSlot().getItem();

        if(!CompatHooks.EnchantLimiter.canApplyEnchantment(stack, enchantmentHolder)) return;

        if(CostHelper.canEnchant(menu, enchantmentHolder, level, player)) {
            stack.enchant(enchantmentHolder, level);
            menu.getToolSlot().setChanged();

            boolean isHighestTier = level == CostRegistry.server()
                    .get(enchantmentHolder)
                    .levelCosts()
                    .maxLevel();

            menu.getToolSlot().set(EnchantmentUtil.tryConvertVanillaBook(stack));

            FxHelper.playEnchantSuccess(context.player(), menu.getBlockPos(), isHighestTier);
        }
    }
}
