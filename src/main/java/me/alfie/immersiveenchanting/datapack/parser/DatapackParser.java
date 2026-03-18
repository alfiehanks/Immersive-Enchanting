package me.alfie.immersiveenchanting.datapack.parser;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import me.alfie.immersiveenchanting.datapack.cost.*;
import me.alfie.immersiveenchanting.util.CostHelper;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DatapackParser {

    /**
     * Parse an enchantment cost json, return an EnchantmentCost.
     * @param file
     * @return
     */
    public static EnchantmentCost parseJson(JsonElement file) {
        JsonObject root = file.getAsJsonObject();

        //Check file has "levels"
        if(!root.has(JsonProperty.LEVELS.getKey()) || !root.get(JsonProperty.LEVELS.getKey()).isJsonObject()) {
            throw new EnchantmentCostParseException("Could not find 'levels' or 'levels' is not a JSON object in " + root);
        }

        //Optional check enabled - default true
        boolean enabled = true;
        if(root.has(JsonProperty.ENABLED.getKey())) {
            enabled = root.get(JsonProperty.ENABLED.getKey()).getAsBoolean();
        }

        //Search through each level key.
        JsonObject levels = root.getAsJsonObject(JsonProperty.LEVELS.getKey());
        Map<String, CostDefinition> levelCosts = new HashMap<>();
        for(Map.Entry<String, JsonElement> entry : levels.entrySet()) {
            String levelKey = getLevelKey(entry);
            JsonObject levelNode = entry.getValue().getAsJsonObject();
            CostDefinition costNode = parseCostNode(levelNode);

            levelCosts.put(levelKey, costNode);
        }

        return new EnchantmentCost(levelCosts, enabled);
    }

    /**
     * Recursively parse through a level key.
     * @param node
     * @return
     */
    public static CostDefinition parseCostNode(JsonObject node) {
        if(node.has(JsonProperty.ITEM.getKey()) || node.has(JsonProperty.AMOUNT.getKey()) || node.has(JsonProperty.XP_LEVELS.getKey())) {
            if(CostHelper.isItemTag(node.get(JsonProperty.ITEM.getKey()).getAsString())) {
                String itemTag = node.get(JsonProperty.ITEM.getKey()).getAsString();
                int amount = node.get(JsonProperty.AMOUNT.getKey()).getAsInt();
                int xpLevels = node.has(JsonProperty.XP_LEVELS.getKey())
                        ? node.get(JsonProperty.XP_LEVELS.getKey()).getAsInt()
                        : 0;


                ImmersiveEnchanting.LOGGER.info("Parsing item tag at {}", node);
                return new CostGroup(new ArrayList<>(), GroupType.ANY_OF,
                        new CostItemTag(itemTag, amount, xpLevels)); //todo XP Levels?
            }
            return parseCostEntry(node);
        }

        else if (node.has(JsonProperty.ANY_OF.getKey())) {
            JsonArray anyOfArray = node.getAsJsonArray(JsonProperty.ANY_OF.getKey());
            List<CostDefinition> children = new ArrayList<>();
            for(JsonElement element : anyOfArray) {
                children.add(parseCostNode(element.getAsJsonObject())); //Recursive!
            }
            return new CostGroup(children, GroupType.ANY_OF);
        } else if (node.has(JsonProperty.ALL_OF.getKey())) {
            JsonArray allOfArray = node.getAsJsonArray((JsonProperty.ALL_OF.getKey()));
            List<CostDefinition> children = new ArrayList<>();
            for(JsonElement element : allOfArray) {
                children.add(parseCostNode(element.getAsJsonObject())); //Recursive!
            }
            ImmersiveEnchanting.LOGGER.warn("'all_of' is accepted in enchantment cost files but not currently supported!");
            return new CostGroup(children, GroupType.ALL_OF);
        }

        else {
            throw new EnchantmentCostParseException("Invalid cost node.");
        }
    }

    public static CostEntry parseCostEntry(JsonObject costElement) {
        //Must contain "item"
        if(!costElement.has(JsonProperty.ITEM.getKey())) {
            throw new EnchantmentCostParseException("Cost is missing 'item'");
        }
        String item = costElement.get(JsonProperty.ITEM.getKey()).getAsString();

        //Optional contain "nbt"
        String nbt = "";
        if(costElement.has(JsonProperty.NBT.getKey())) {
            nbt = costElement.get(JsonProperty.NBT.getKey()).getAsString();
        }

        //Must contain "amount"
        if(!costElement.has(JsonProperty.AMOUNT.getKey())) {
            throw new EnchantmentCostParseException("Cost is missing 'amount'");
        }
        int amount = costElement.get(JsonProperty.AMOUNT.getKey()).getAsInt();

        //Optional contain "xp_levels"
        int xpLevels = 0;
        if(costElement.has(JsonProperty.XP_LEVELS.getKey())) {
            xpLevels = costElement.get(JsonProperty.XP_LEVELS.getKey()).getAsInt();
        }

        return new CostEntry(item, nbt, amount, xpLevels);
    }


    /**
     * Level key must be a positive integer as a string, i.e "1", "2", etc.
     * This method throws an exception if the string is not a positive integer.
     * Otherwise, returns it.
     * @param entry
     * @return
     */
    private static @NotNull String getLevelKey(Map.Entry<String, JsonElement> entry) {
        String levelKey = entry.getKey();

        //Ensure levelKey is a positive integer.
        try {
            int level = Integer.parseInt(levelKey); //Throws if not an int
            if(level <= 0) {
                throw new EnchantmentCostParseException("Level must be a positive integer, got: " + levelKey);
            }
        } catch(NumberFormatException e) {
            throw new EnchantmentCostParseException("Level key must be an integer string, got: " + levelKey);
        }
        return levelKey;
    }

    /**
     * Convert an EnchantmentCost into a JsonObject.
     * @param cost
     * @return
     */
    public static JsonObject toJson(EnchantmentCost cost) {
        JsonObject root = new JsonObject();

        //Enabled flag
        root.addProperty(JsonProperty.ENABLED.getKey(), cost.enabled);

        //Levels object
        JsonObject levelsObject = new JsonObject();

        for(Map.Entry<String, CostDefinition> entry : cost.levels.entrySet()) {
            String level = entry.getKey();
            CostDefinition node = entry.getValue();

            levelsObject.add(level, serializeNode(node));
        }

        root.add("levels", levelsObject);
        return root;
    }

    private static JsonObject serializeNode(CostDefinition costDefinition) {
        JsonObject object = new JsonObject();

        if (costDefinition instanceof CostEntry costEntry) {
            object.addProperty(JsonProperty.ITEM.getKey(), costEntry.item());

            if (!costEntry.nbt().isEmpty()) {
                object.addProperty(JsonProperty.NBT.getKey(), costEntry.nbt());
            }

            object.addProperty(JsonProperty.AMOUNT.getKey(), costEntry.amount());

            if (costEntry.xpLevels() > 0) {
                object.addProperty(JsonProperty.XP_LEVELS.getKey(), costEntry.xpLevels());
            }

        } else if (costDefinition instanceof CostGroup composite) {

            //If this group has a CostItemTag, serialize as a tag object
            if (composite.getCostItemTag().isPresent()) {
                CostItemTag tag = composite.getCostItemTag().get();

                object.addProperty(JsonProperty.ITEM.getKey(),
                        tag.itemTag()); // or tag.itemTag().location() if TagKey

                object.addProperty(JsonProperty.AMOUNT.getKey(), tag.amount());

                if (tag.xpLevels() > 0) {
                    object.addProperty(JsonProperty.XP_LEVELS.getKey(), tag.xpLevels());
                }

            } else {
                // Otherwise serialize children as any_of / all_of
                JsonArray childrenArray = new JsonArray();
                for (CostDefinition child : composite.children()) {
                    childrenArray.add(serializeNode(child));
                }

                if (composite.type() == GroupType.ANY_OF) {
                    object.add(JsonProperty.ANY_OF.getKey(), childrenArray);
                } else {
                    object.add(JsonProperty.ALL_OF.getKey(), childrenArray);
                }
            }
        }

        return object;
    }
}
