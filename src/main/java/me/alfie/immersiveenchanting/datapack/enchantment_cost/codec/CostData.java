package me.alfie.immersiveenchanting.datapack.enchantment_cost.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.alfie.alfinolib.networking.codec.CommonCodecs;
import me.alfie.alfinolib.networking.codec.StreamCodec;
import me.alfie.alfinolib.networking.codec.StreamCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.HashMap;
import java.util.Optional;

public record CostData(boolean enabled, Optional<Boolean> transmutable, Optional<Boolean> replicable, CostLevels levelCosts) {

    /**
     * Optional per-field override for whether this enchantment can be produced as the
     * result of a Transmute action. When absent, {@link #isTransmutable()} falls back to
     * {@link #enabled()} so existing datapacks that don't set this field keep their current
     * behaviour unchanged.
     */
    public boolean isTransmutable() {
        return transmutable.orElse(enabled);
    }

    /**
     * Optional per-field override for whether an Ancient Book carrying this enchantment can
     * be duplicated via the Replicate action. When absent, {@link #isReplicable()} falls back
     * to {@link #enabled()} so existing datapacks that don't set this field keep their current
     * behaviour unchanged.
     */
    public boolean isReplicable() {
        return replicable.orElse(enabled);
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, Optional<Boolean>> OPTIONAL_BOOL_STREAM_CODEC = StreamCodec.of(
            (buf, value) -> {
                buf.writeBoolean(value.isPresent());
                value.ifPresent(buf::writeBoolean);
            },
            buf -> buf.readBoolean() ? Optional.of(buf.readBoolean()) : Optional.empty()
    );

    public static final Codec<CostData> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("enabled", true).forGetter(CostData::enabled),
                    Codec.BOOL.optionalFieldOf("transmutable").forGetter(CostData::transmutable),
                    Codec.BOOL.optionalFieldOf("replicable").forGetter(CostData::replicable),
                    CostLevels.CODEC.fieldOf("levels").forGetter(CostData::levelCosts)
            ).apply(instance, CostData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CostData> STREAM_CODEC =
            StreamCodecBuilder.<RegistryFriendlyByteBuf, CostData>create()
                    .add(CommonCodecs.BOOL, CostData::enabled)
                    .add(OPTIONAL_BOOL_STREAM_CODEC, CostData::transmutable)
                    .add(OPTIONAL_BOOL_STREAM_CODEC, CostData::replicable)
                    .add(CostLevels.STREAM_CODEC, CostData::levelCosts)
                    .build(CostData::new);

    public static final CostData EMPTY = new CostData(true, Optional.empty(), Optional.empty(), new CostLevels(new HashMap<>()));
}
