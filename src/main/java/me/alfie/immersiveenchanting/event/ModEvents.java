package me.alfie.immersiveenchanting.event;

import me.alfie.immersiveenchanting.ImmersiveEnchantingClient;
import me.alfie.immersiveenchanting.api.ApiPostEvents;
import me.alfie.immersiveenchanting.api.description.TooltipDescriptionExtensions;
import me.alfie.immersiveenchanting.command.ModCommands;
import me.alfie.immersiveenchanting.creativetab.ModCreativeTab;
import me.alfie.immersiveenchanting.datapack.enchantment_cost.CostDatapack;
import me.alfie.immersiveenchanting.datapack.mod_icons.ModIconsDatapack;
import me.alfie.immersiveenchanting.datapack.node_sounds.NodeSoundsDatapack;
import me.alfie.immersiveenchanting.gui.ModMenus;
import me.alfie.immersiveenchanting.networking.ModPackets;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

public class ModEvents {

    /**
     * Wires up all event listeners for the mod.
     * Registers the following on the NeoForge bus:
     * <ul>
     *   <li>Datapack reload listeners ({@link CostDatapack}, {@link NodeSoundsDatapack})</li>
     *   <li>Server lifecycle handlers (start, finished, reload, stop)</li>
     *   <li>Enchantment holder resolution on tag updates (client + server)</li>
     *   <li>Command registration ({@link ModCommands})</li>
     * </ul>
     * Registers the following on the mod event bus:
     * <ul>
     *   <li>Screen and packet registration ({@link me.alfie.immersiveenchanting.gui.ModMenus}, {@link me.alfie.immersiveenchanting.networking.ModPackets})</li>
     *   <li>Internal tooltip description extensions</li>
     *   <li>Creative tab population</li>
     * </ul>
     */
    public static void register(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(CostDatapack::resolveClientRegistry);
        NeoForge.EVENT_BUS.addListener(CostDatapack::resolveServerRegistry);

        NeoForge.EVENT_BUS.addListener(EnchantingTableBreakHandler::onBlockBreak);
        NeoForge.EVENT_BUS.addListener(ModCommands::register);

        modEventBus.addListener(ImmersiveEnchantingClient::registerBlockEntityRenderers);

        modEventBus.addListener(ModMenus::registerScreens);
        modEventBus.addListener(ModPackets::register);
        modEventBus.addListener(ModCreativeTab::build);

        registerPostEvents(modEventBus);
        registerInternalApiEvents(modEventBus);
    }

    private static void registerPostEvents(IEventBus modEventBus) {
        modEventBus.addListener(ApiPostEvents::postRegisterTooltipDescriptionsEvent);
    }

    private static void registerInternalApiEvents(IEventBus modEventBus) {
        modEventBus.addListener(TooltipDescriptionExtensions::registerInternalTooltipDescriptions);
    }
}
