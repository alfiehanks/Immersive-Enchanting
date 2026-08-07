package me.alfie.immersiveenchanting.datapack.enchantment_cost.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.alfie.alfinolib.networking.codec.CommonCodecs;
import me.alfie.alfinolib.networking.codec.StreamCodec;
import me.alfie.alfinolib.networking.codec.StreamCodecBuilder;
import me.alfie.alfinolib.util.codec.ItemCost;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.List;

public record Cost(ItemCost itemCost, int xpLevels) {

    public static final Codec<Cost> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    ItemCost.CODEC.fieldOf("item_cost").forGetter(Cost::itemCost),
                    Codec.INT.optionalFieldOf("xp_levels", 0).forGetter(Cost::xpLevels)
            ).apply(instance, Cost::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, Cost> STREAM_CODEC =
            StreamCodecBuilder.<RegistryFriendlyByteBuf, Cost>create()
                    .add(ItemCost.STREAM_CODEC, Cost::itemCost)
                    .add(CommonCodecs.VAR_INT, Cost::xpLevels)
                    .build(Cost::new);

    public static final Cost EMPTY = new Cost(ItemCost.EMPTY, 0);

    /**Convenience method*/
    public List<ItemStack> getItemStacks() {
        return itemCost.getItemStacks();
    }

    /**
     * Returns this cost if it is valid, null if none found
     */
    public @Nullable Cost test(ItemStack testStack, Player player) {
        if( (itemCost().isValid(testStack) || itemCost().getItems().contains(Items.AIR)) && player.experienceLevel >= xpLevels) {
            return this;
        }
        return null;
    }
}
