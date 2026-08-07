package me.alfie.immersiveenchanting.datapack.node_sounds;

import me.alfie.alfinolib.datapacks.DatapackKey;
import me.alfie.alfinolib.datapacks.DatapackRegistry;
import me.alfie.alfinolib.datapacks.ModDatapack;
import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import java.util.HashMap;
import java.util.Map;

public class NodeSoundsDatapack extends ModDatapack<NodeSoundMap, NodeSoundMap> {

    public static DatapackKey<NodeSoundMap> KEY = new DatapackKey<>(ImmersiveEnchanting.MODID, "sounds");
    public static DatapackDefinition<NodeSoundMap> DEFINITION = new DatapackDefinition<>(KEY, NodeSoundMap.STREAM_CODEC);

    private NodeSoundMap DATA = new NodeSoundMap(new HashMap<>());


    public NodeSoundsDatapack(RegistryAccess registryAccess) {
        super(NodeSoundMap.CODEC, DEFINITION, registryAccess);
    }

    @Override public NodeSoundMap getData() {
        return DATA;
    }


    @Override
    protected void apply(Map<Identifier, NodeSoundMap> input, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        Identifier key = Identifier.fromNamespaceAndPath(ImmersiveEnchanting.MODID, "node_sounds");
        DATA = input.getOrDefault(key, new NodeSoundMap(new HashMap<>()));

        ImmersiveEnchanting.LOGGER.debug("Found {}", String.valueOf(DATA));
    }
}
