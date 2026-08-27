package me.alfie.immersiveenchanting.compat.ench_desc.mixin;

import net.darkhax.enchdesc.common.impl.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.darkhax.enchdesc.common.impl.EnchdescMod")
public interface EnchdescModAccessor {

    @Accessor("config")
    Config getConfig();
}
