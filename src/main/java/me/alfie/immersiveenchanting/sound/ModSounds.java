package me.alfie.immersiveenchanting.sound;

import me.alfie.immersiveenchanting.ImmersiveEnchanting;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ImmersiveEnchanting.MODID);

    public static final RegistryObject<SoundEvent> BIBLIOCLASM = SOUND_EVENTS.register("biblioclasm",
            () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(ImmersiveEnchanting.MODID, "biblioclasm")));

    public static final RegistryObject<SoundEvent> ARCANE_MEMORIES = SOUND_EVENTS.register("arcane_memories",
            () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(ImmersiveEnchanting.MODID, "arcane_memories")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }

}
