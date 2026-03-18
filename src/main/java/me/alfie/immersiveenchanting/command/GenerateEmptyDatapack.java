package me.alfie.immersiveenchanting.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import me.alfie.immersiveenchanting.util.EnchantmentUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public enum GenerateEmptyDatapack implements ImmersiveEnchantingCommand {
    COMMAND;

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("immersiveenchanting")
                        .then(Commands.literal("generateEmptyDatapack")
                                .requires(source -> source.hasPermission(4))
                                .executes(context -> {
                                    Level level = context.getSource().getLevel();
                                    File worldFolder = level.getServer().getWorldPath(LevelResource.GENERATED_DIR).toFile();
                                    File rootFolder = new File(worldFolder, "immersiveenchanting-generateddatapack");

                                    if(rootFolder.exists()) {
                                        try {
                                            FileUtils.deleteDirectory(rootFolder);
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                    rootFolder.mkdirs();

                                    File dataFolder = new File(rootFolder, "data");
                                    dataFolder.mkdirs();
                                    File immersiveEnchantingFolder = new File(dataFolder, "immersiveenchanting");
                                    immersiveEnchantingFolder.mkdirs();

                                    File enchantmentCostFolder = new File(immersiveEnchantingFolder, "enchantment_costs");
                                    enchantmentCostFolder.mkdirs();

                                    List<Holder.Reference<Enchantment>> allEnchantments = EnchantmentUtil.getAllEnchantments(level, false);

                                    for(Holder<Enchantment> enchantmentHolder : allEnchantments) {
                                        File jsonFile = getFile(enchantmentHolder, enchantmentCostFolder);

                                        int maxDefaultLevel = enchantmentHolder.value().getMaxLevel();
                                        JsonObject root = buildCostJson(maxDefaultLevel);

                                        writeJsonFile(jsonFile, root);
                                    }

                                    //Generate transmute/replicate
                                    File ieFolder = new File(enchantmentCostFolder, "immersiveenchanting");
                                    ieFolder.mkdirs();
                                    File transmuteFile = new File(ieFolder, "transmute.json");
                                    JsonObject transmuteJson = buildCostJson(1);
                                    File replicateFile = new File(ieFolder, "replicate.json");
                                    JsonObject replicateJson = buildCostJson(1);
                                    File enchantingFuelsFile = new File(ieFolder, "enchanting_fuels.json");
                                    JsonObject enchantingFuelsJson = buildCostJson(5);
                                    writeJsonFile(transmuteFile, transmuteJson);
                                    writeJsonFile(replicateFile, replicateJson);
                                    writeJsonFile(enchantingFuelsFile, enchantingFuelsJson);


                                    //Generate pack.mcmeta
                                    final int PACK_FORMAT = 48; //1.21.1 format.
                                    JsonObject pack = new JsonObject();
                                    pack.addProperty("description", "This is the description of your data pack");
                                    pack.addProperty("pack_format", PACK_FORMAT);
                                    pack.addProperty("min_format", PACK_FORMAT);
                                    pack.addProperty("max_format", PACK_FORMAT);

                                    JsonObject root = new JsonObject();
                                    root.add("pack", pack);
                                    File packmcmeta = new File(rootFolder, "pack.mcmeta");
                                    writeJsonFile(packmcmeta, root);

                                    Component message = Component.literal("Success! Click to copy the generated folder path.")
                                            .withStyle(style -> {
                                                try {
                                                    return style
                                                            .withColor(ChatFormatting.GREEN)
                                                            .withUnderlined(true)
                                                            .withClickEvent(new ClickEvent(
                                                                    ClickEvent.Action.COPY_TO_CLIPBOARD,
                                                                    rootFolder.getCanonicalPath()));
                                                } catch (IOException e) {
                                                    return style
                                                            .withColor(ChatFormatting.GREEN)
                                                            .withUnderlined(true)
                                                            .withClickEvent(new ClickEvent(
                                                                    ClickEvent.Action.COPY_TO_CLIPBOARD,
                                                                    rootFolder.getAbsolutePath()));
                                                }
                                            });

                                    context.getSource().sendSuccess(() -> message, false);

                                    return 1;
                                })
                        )
        );
    }

    private static @NotNull File getFile(Holder<Enchantment> enchantmentHolder, File enchantmentCostFolder) {
        String id = enchantmentHolder.getRegisteredName();

        String[] parts = id.split(":", 2);

        String namespace = parts[0];
        String enchantName = parts[1];

        //Try to make namespace folder if doesn't exist
        File namespaceFolder = new File(enchantmentCostFolder, namespace);
        if(!namespaceFolder.exists()) {
            namespaceFolder.mkdirs();
        }

        File jsonFile = new File(namespaceFolder, enchantName + ".json");
        return jsonFile;
    }

    private static JsonObject buildCostJson(int totalLevels) {
        JsonObject levels = new JsonObject();
        for (int i = 1; i <= totalLevels; i++) {
            JsonObject costDefinition = new JsonObject();
            costDefinition.addProperty("item", "minecraft:air");
            costDefinition.addProperty("amount", 0);
            costDefinition.addProperty("xp_levels", 0);
            levels.add(String.valueOf(i), costDefinition);
        }

        JsonObject root = new JsonObject();
        root.addProperty("enabled", true);
        root.add("levels", levels);
        return root;
    }

    private static void writeJsonFile(File file, JsonObject json) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try(FileWriter writer = new FileWriter(file)) {
            gson.toJson(json, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
