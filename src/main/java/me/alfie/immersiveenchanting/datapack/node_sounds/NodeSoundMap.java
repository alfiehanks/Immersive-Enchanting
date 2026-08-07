package me.alfie.immersiveenchanting.datapack.node_sounds;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.alfie.alfinolib.datapacks.client.ClientDatapackManager;
import me.alfie.alfinolib.networking.codec.StreamCodec;
import me.alfie.alfinolib.util.ResourceId;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;

public record NodeSoundMap(Map<Identifier, NodeSound> enchantments) {

    public static NodeSoundMap client() {
        return ClientDatapackManager.get(NodeSoundsDatapack.KEY);
    }

    public static final Codec<NodeSoundMap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Identifier.CODEC, NodeSound.CODEC).fieldOf("enchantments").forGetter(NodeSoundMap::enchantments)
    ).apply(instance, NodeSoundMap::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, NodeSoundMap> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, NodeSoundMap>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, NodeSoundMap nodeSoundMap) {
            buf.writeInt(nodeSoundMap.enchantments.size());

            for (var entry : nodeSoundMap.enchantments.entrySet()) {
                buf.writeIdentifier(entry.getKey());
                NodeSound.STREAM_CODEC.encode(buf, entry.getValue());
            }
        }

        @Override
        public NodeSoundMap decode(RegistryFriendlyByteBuf buf) {
            int size = buf.readInt();
            Map<Identifier, NodeSound> map = new HashMap<>();

            for (int i = 0; i < size; i++) {
                Identifier id = buf.readIdentifier();
                NodeSound sound = NodeSound.STREAM_CODEC.decode(buf);
                map.put(id, sound);
            }

            return new NodeSoundMap(map);
        }
    };

    public boolean containsKey(ResourceId id) {
        return enchantments().containsKey(id.mc());
    }

    public NodeSound get(ResourceId id) {
        return enchantments.get(id.mc());
    }
}
