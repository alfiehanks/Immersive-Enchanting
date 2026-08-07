package me.alfie.immersiveenchanting.datapack.enchantment_cost;

import me.alfie.alfinolib.datapacks.*;
import me.alfie.alfinolib.datapacks.client.ClientDatapackUpdatedEvent;
import me.alfie.alfinolib.datapacks.server.ServerDatapackUpdatedEvent;
import me.alfie.alfinolib.util.ResourceId;
import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.codec.Cost;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.codec.CostData;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.codec.CostLevels;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class CostDatapack extends ModDatapack<CostData, CostRegistry> {

    public static final DatapackKey<CostRegistry> KEY = new DatapackKey<>(ImmersiveEnchanting.MODID, "enchantment_costs");
    public static final DatapackDefinition<CostRegistry> DEFINITION = new DatapackDefinition<>(KEY, CostRegistry.STREAM_CODEC);

    private CostRegistry DATA;

    public CostDatapack(RegistryAccess registryAccess) {
        super(CostData.CODEC, DEFINITION, registryAccess);
    }

    @Override
    public CostRegistry getData() {
        return DATA;
    }

    @Override
    protected void apply(Map<Identifier, CostData> identifierEnchantmentDataMap, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
        DATA = new CostRegistry();

        int count = 0;
        for(Map.Entry<Identifier, CostData> entry : identifierEnchantmentDataMap.entrySet()) {
            ResourceId id = remapIdentifierPath(entry.getKey());
            CostData data = entry.getValue();

            DATA.register(id, data);
            count++;
        }

        ImmersiveEnchanting.LOGGER.debug("Populated temp cost registry with {} entries, ready to pull.", count);
    }

    /**
     * Converts the datapack file path identifier (e.g. {@code minecraft/sharpness})
     * into the proper enchantment identifier form (e.g. {@code minecraft:sharpness})
     * by replacing the first path separator with a colon.
     *
     * Also preserves any additional sub-paths, allowing for hierarchical organization of enchantment cost files (e.g. {@code minecraft/weapon/sharpness} -> {@code minecraft:weapon/sharpness}).
     */
    private static ResourceId remapIdentifierPath(Identifier originalId) {
        String path = originalId.getPath();

        int firstSlash = path.indexOf('/');
        if (firstSlash == -1) {
            throw new IllegalArgumentException("Invalid enchantment cost path: " + originalId);
        }

        String namespace = path.substring(0, firstSlash);
        String subPath = path.substring(firstSlash + 1);

        return new ResourceId(namespace, subPath);
    }

    public static void resolveServerRegistry(ServerDatapackUpdatedEvent event) {
        CostRegistry.server().resolveEnchantmentHolders(event.getServer().registryAccess());
        ImmersiveEnchanting.LOGGER.debug("Resolved enchantment holders for server");
        CostRegistry.server().printRegistry();
    }

    public static void resolveClientRegistry(ClientDatapackUpdatedEvent event) {
        CostRegistry.client().resolveEnchantmentHolders(event.getPlayer().registryAccess());
        ImmersiveEnchanting.LOGGER.debug("Resolved enchantment holders for client");
        CostRegistry.client().printRegistry();

        sendWarningMessages(event.getPlayer());
    }

    private static void sendWarningMessages(Player player) {
        List<Holder<Enchantment>> costRegistryEnchantments = CostRegistry.client().getAllEnchantmentHolders();
        List<Holder<Enchantment>> allEnchantments = EnchantmentUtil.getAllEnchantmentsInRegistry(player.registryAccess());

        Set<Holder<Enchantment>> costRegistrySet = new HashSet<>(costRegistryEnchantments);
        Set<Holder<Enchantment>> allEnchantmentsSet = new HashSet<>(allEnchantments);

        Set<Holder<Enchantment>> missingInCost = new HashSet<>(allEnchantmentsSet);
        missingInCost.removeAll(costRegistrySet);

        MutableComponent modIdComponent = Component.literal("[ImmersiveEnchanting] ");
        if(!missingInCost.isEmpty()) {

            Set<Identifier> missingIds = getEnchantmentIds(missingInCost);
            player.sendSystemMessage(
                    modIdComponent.append(Component.translatable("immersiveenchanting.warn.cost_files_missing")
                            .withStyle(ChatFormatting.RED))
            );

            ImmersiveEnchanting.LOGGER.error("There are missing enchantment cost files for the following enchantments: {}", missingIds);
        }

        //Check if any in costs are null
        List<String> brokenIds = new ArrayList<>();
        for (Holder<Enchantment> enchantmentHolder : CostRegistry.client().getAllEnchantmentHolders()) {
            CostData enchantmentCost = CostRegistry.client().get(enchantmentHolder);
            CostLevels levelCosts = enchantmentCost.levelCosts();

            for (int level = 0; level < levelCosts.maxLevel(); level++) {
                if (levelCosts.getLevel(level+1).costs().isEmpty()) {
                    brokenIds.add(enchantmentHolder.getRegisteredName());
                }
            }
        }

        if(!brokenIds.isEmpty()) {
            String result = String.join(", ", brokenIds);
            player.sendSystemMessage(
                    modIdComponent.append(Component.translatable("immersiveenchanting.warn.invalid_cost_file", result)
                            .withStyle(ChatFormatting.RED))
            );
        }


        //Check enchanting fuels
        if(!CostRegistry.client().isRegistered(CostRegistry.ENCHANTING_FUELS)) {
            ImmersiveEnchanting.LOGGER.error("enchantment_costs/immersiveenchanting/enchanting_fuels.json is missing. Please add this file to your datapack.");
             player.sendSystemMessage(
                    modIdComponent.append(Component.translatable("immersiveenchanting.warn.enchanting_fuels_missing")
                            .withStyle(ChatFormatting.RED))
            );
        }

        if(!CostRegistry.client().isRegistered(CostRegistry.TRANSMUTE)) {
             player.sendSystemMessage(
                    modIdComponent.append(Component.translatable("immersiveenchanting.warn.cost_files_missing")
                            .withStyle(ChatFormatting.RED))
            );
            ImmersiveEnchanting.LOGGER.error("enchantment_costs/immersiveenchanting/transmute.json is missing. Please add this file to your datapack.");
        }

        if(!CostRegistry.client().isRegistered(CostRegistry.REPLICATE)) {
            player.sendSystemMessage(
                    modIdComponent.append(Component.translatable("immersiveenchanting.warn.cost_files_missing")
                            .withStyle(ChatFormatting.RED))
            );
            ImmersiveEnchanting.LOGGER.error("enchantment_costs/immersiveenchanting/replicate.json is missing. Please add this file to your datapack.");
        }
    }

    private static Set<Identifier> getEnchantmentIds(Set<Holder<Enchantment>> enchantmentHolders) {
        return enchantmentHolders.stream()
                .map(holder -> holder.unwrapKey().orElseThrow().identifier())
                .collect(Collectors.toSet());
    }
}
