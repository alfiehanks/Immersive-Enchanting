package me.alfie.immersiveenchanting.datapack;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.datapack.cost.*;
import me.alfie.immersiveenchanting.datapack.parser.DatapackParser;
import me.alfie.immersiveenchanting.networking.packet.enchantmentcostregistrysync.EnchantmentCostRegistrySyncPacket;
import me.alfie.immersiveenchanting.util.CostHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class EnchantmentCostDatapack extends SimpleJsonResourceReloadListener {

    private static final String DIRECTORY = "enchantment_costs";
    public static final EnchantmentCostDatapack DATAPACK = new EnchantmentCostDatapack(
            new Gson(), EnchantmentCostDatapack.DIRECTORY);

    private MinecraftServer server;

    public EnchantmentCostDatapack(Gson gson, String directory) {
        super(gson, directory);
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    /**
     * Fires server-side.<br>
     * Reads datapack from directory "enchantment_costs" into the server's enchantment cost registry.<br>
     * Triggers on /reload.
     * @param object
     * @param resourceManager
     * @param profiler
     */
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> object, ResourceManager resourceManager, ProfilerFiller profiler) {
        //Lazily initialise the server enchantment cost registry.
        if(EnchantmentCostRegistry.getServerRegistry() == null) {
            EnchantmentCostRegistry.setServerRegistry(new EnchantmentCostRegistry());
        }
        EnchantmentCostRegistry.getServerRegistry().clear();

        int fileCount = 0;

        ImmersiveEnchanting.LOGGER.info("Parsing datapack files...");

        for(Map.Entry<ResourceLocation, JsonElement> entry : object.entrySet()) {
            ResourceLocation fileId = entry.getKey();   //e.g. immersiveenchanting:minecraft/efficiency
            JsonElement json = entry.getValue();

            //Convert file path to actual enchantment RL: "minecraft/efficiency" -> ResourceLocation("minecraft", "efficiency")
            String[] parts = fileId.getPath().split("/", 2);
            if (parts.length != 2) continue; //invalid file structure

            //Load transmute/replicate costs differently
            EnchantmentCost enchantmentCost = DatapackParser.parseJson(json);
            if(Objects.equals(parts[0], "immersiveenchanting")) {

                EnchantmentCostRegistry.InternalCosts key;
                if(parts[1].equals("transmute")) {
                    key = EnchantmentCostRegistry.InternalCosts.TRANSMUTE;
                    EnchantmentCostRegistry.getServerRegistry().getInternalRegistry().put(key, enchantmentCost);
                    ImmersiveEnchanting.LOGGER.info("Loaded costs for transmute.");
                } else if(parts[1].equals("replicate")) {
                    key = EnchantmentCostRegistry.InternalCosts.REPLICATE;
                    EnchantmentCostRegistry.getServerRegistry().getInternalRegistry().put(key, enchantmentCost);
                    ImmersiveEnchanting.LOGGER.info("Loaded costs for replicate.");
                } else if(parts[1].equals("enchanting_fuels")) {
                    key = EnchantmentCostRegistry.InternalCosts.ENCHANTING_FUELS;
                    EnchantmentCostRegistry.getServerRegistry().getInternalRegistry().put(key, enchantmentCost);
                    ImmersiveEnchanting.LOGGER.info("Loaded costs for enchanting fuels.");
                }


            //Normal enchantment costs
            } else {
                // Parse JSON into an EnchantmentCost
                ResourceLocation enchantmentResourceLocation = ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]);

                //Put into server registry
                EnchantmentCostRegistry.getServerRegistry().getCostRegistry().put(ResourceKey.create(Registries.ENCHANTMENT, enchantmentResourceLocation), enchantmentCost);
                fileCount++;
            }
        }
        ImmersiveEnchanting.LOGGER.info("Loaded " + fileCount + " enchantment costs.");
        //-----------------------///

        //Sync client with server
        if(server != null) {
            int count = 0;
            for(ServerPlayer player : server.getPlayerList().getPlayers()) {
                EnchantmentCostRegistrySyncPacket.syncClientWithServer(player);
                count++;
            }
            ImmersiveEnchanting.LOGGER.info("Synced server enchantment cost registry with " + count + " client(s).");
        }
    }

    /**
     * Use the neoforge tag #neoforge:enchanting_fuels.
     * @return
     */
    public static List<Item> getValidEnchantingFuels() {
        return CostHelper.getItemsInItemTag(CostHelper.getItemTag("neoforge:enchanting_fuels"));
    }

    /**
     * Expand all item tags in a registry.
     * @param registry
     */
    public static void expandTags(EnchantmentCostRegistry registry) {
        expandCosts(registry.getCostRegistry().values());
        expandCosts(registry.getInternalRegistry().values());

        ImmersiveEnchanting.LOGGER.info("Expanded tags for " + registry.getName());
    }

    private static void expandCosts(Collection<EnchantmentCost> costs) {
        for (EnchantmentCost cost : costs) {
            for (int i = 0; i < cost.getHighestLevel(); i++) {
                CostDefinition costDefinition = cost.getCostForLevel(i + 1);

                if (costDefinition instanceof CostGroup costGroup) {
                    expandCostGroupTagsRecursive(costGroup);
                }
            }
        }
    }

    /**
     * Recursively expand tags with a CostGroup
     * @param costGroup
     */
    private static void expandCostGroupTagsRecursive(CostGroup costGroup) {
        costGroup.getCostItemTag().ifPresent(tag -> {
            expandCostGroupTag(costGroup);
        });

        for(CostDefinition child : costGroup.children()) {
            if(child instanceof CostGroup childGroup) {
                expandCostGroupTagsRecursive(childGroup);
            }
        }
    }

    /**
     * Expand tags within a CostGroup
     * @param costGroup
     */
    private static void expandCostGroupTag(CostGroup costGroup) {
        if(costGroup.getCostItemTag().isPresent()) {
            CostItemTag costItemTag = costGroup.getCostItemTag().get();

            List<Item> itemsInTag = CostHelper.getItemsInItemTag(
                    CostHelper.getItemTag(costItemTag.itemTag()));

            for(Item item : itemsInTag) {
                CostEntry costEntry = new CostEntry(item.toString(), "", costItemTag.amount(), costItemTag.xpLevels(), costItemTag);
                costGroup.children().add(costEntry);
            }

            ImmersiveEnchanting.LOGGER.info("Expanded {}", costGroup);
        }
    }

}
