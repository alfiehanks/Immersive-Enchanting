package me.alfie.immersiveenchanting.datapack.enchantment_cost;

import me.alfie.alfinolib.datapacks.client.ClientDatapackManager;
import me.alfie.alfinolib.datapacks.server.ServerDatapackManager;
import me.alfie.alfinolib.networking.codec.StreamCodec;
import me.alfie.alfinolib.util.ResourceId;
import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.codec.CostData;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CostRegistry {

    private final Map<Holder<Enchantment>, CostData> ENCHANTMENT_HOLDER_REGISTRY = new HashMap<>();
    private final Map<ResourceId, CostData> ID_REGISTRY = new HashMap<>();

    public static final StreamCodec<RegistryFriendlyByteBuf, CostRegistry> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, CostRegistry>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, CostRegistry costRegistry) {
            Map<ResourceId, CostData> map = costRegistry.ID_REGISTRY;

            buf.writeInt(map.size());

            for (var entry : map.entrySet()) {
                buf.writeIdentifier(entry.getKey().mc());
                CostData.STREAM_CODEC.encode(buf, entry.getValue());
            }
        }

        @Override
        public CostRegistry decode(RegistryFriendlyByteBuf buf) {
            CostRegistry registry = new CostRegistry();

            int size = buf.readInt();

            for (int i = 0; i < size; i++) {
                Identifier id = buf.readIdentifier();
                CostData data = CostData.STREAM_CODEC.decode(buf);

                registry.ID_REGISTRY.put(ResourceId.parse(id.toString()), data);
            }

            return registry;
        }
    };

    public static final ResourceId TRANSMUTE = new ResourceId(ImmersiveEnchanting.MODID, "transmute");
    public static final ResourceId REPLICATE = new ResourceId(ImmersiveEnchanting.MODID, "replicate");
    public static final ResourceId ENCHANTING_FUELS = new ResourceId(ImmersiveEnchanting.MODID, "enchanting_fuels");

    public static CostRegistry client() {
        return ClientDatapackManager.get(CostDatapack.KEY);
    }
    public static CostRegistry server() {
        return ServerDatapackManager.get(CostDatapack.KEY);
    }

    public void resolveEnchantmentHolders(HolderLookup.Provider lookup) {
        HolderLookup.RegistryLookup<Enchantment> registry = lookup.lookupOrThrow(Registries.ENCHANTMENT);

        for (ResourceId id : getAllEnchantmentIds()) {
            ResourceKey<Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT, id.mc());

            registry.get(key).ifPresentOrElse(
                    this::register,
                    () -> ImmersiveEnchanting.LOGGER.warn("Datapack contains {} but couldn't find enchantment with this id.", id)
            );
        }
    }

    public void register(ResourceId id, CostData data) {
        ID_REGISTRY.put(id, data);
    }
    public void register(Holder<Enchantment> enchantmentHolder) {
        ResourceId id = ResourceId.parse(enchantmentHolder.getRegisteredName());
        CostData data = get(id);

        if(data == null) {
            throw new IllegalStateException("No CostData registered for id '" + id +
                    "'. You must call register(id, data) before registering the enchantment.");
        }

        ENCHANTMENT_HOLDER_REGISTRY.put(enchantmentHolder, data);
    }

    public boolean isRegistered(Holder<Enchantment> enchantmentHolder) {
        return ENCHANTMENT_HOLDER_REGISTRY.containsKey(enchantmentHolder);
    }
    public boolean isRegistered(ResourceId id) {
        return ID_REGISTRY.containsKey(id);
    }

    public CostData get(Holder<Enchantment> enchantmentHolder) {
        return ENCHANTMENT_HOLDER_REGISTRY.get(enchantmentHolder);
    }
    public CostData get(ResourceId id) {
        CostData data = ID_REGISTRY.get(id);
        return data != null ? data : CostData.EMPTY;
    }

    public void clear() {
        ID_REGISTRY.clear();
        ENCHANTMENT_HOLDER_REGISTRY.clear();
    }
    public void printRegistry() {
        ImmersiveEnchanting.LOGGER.debug("ID Registry: {}", ID_REGISTRY);
        ImmersiveEnchanting.LOGGER.debug("Holder registry (resolved) {}: ", ENCHANTMENT_HOLDER_REGISTRY);
    }

    public List<ResourceId> getAllEnchantmentIds() {
        List<ResourceId> result = new ArrayList<>();

        for (ResourceId id : ID_REGISTRY.keySet()) {
            if(!id.namespace().equals(ImmersiveEnchanting.MODID)) result.add(id);
        }

        return result;
    }
    public List<Holder<Enchantment>> getAllEnchantmentHolders() {
        return new ArrayList<>(ENCHANTMENT_HOLDER_REGISTRY.keySet());
    }
    public List<Holder<Enchantment>> getAllEnabledEnchantmentHolders() {
        List<Holder<Enchantment>> result = getAllEnchantmentHolders();
        result.removeIf(holder -> !get(holder).enabled());
        return result;
    }

    public int getHighestLevel() {
        int highestLevel = 0;
        for (ResourceId id : getAllEnchantmentIds()) {
            int level = get(id).levelCosts().maxLevel();
            if(level > highestLevel) highestLevel = level;
        }
        return highestLevel;
    }

    public Holder<Enchantment> getRandomEnchantment(RandomSource randomSource) {
        return getAllEnabledEnchantmentHolders().get(randomSource.nextInt(getAllEnabledEnchantmentHolders().size()));
    }
}
