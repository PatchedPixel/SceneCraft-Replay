package net.patchedpixel.scenecraft.platform.mixin;

import io.netty.channel.ChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraftforge.network.filters.NetworkFilters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetworkFilters.class, remap = false)
public abstract class MixinNetworkFilters {
    @Inject(method = "injectIfNecessary", at = @At("HEAD"), remap = false)
    private static void removeExistingFilters(Connection manager, CallbackInfo ci) {
        ChannelPipeline pipeline = manager.channel().pipeline();
        for(String name : new String[]{"forge:vanilla_filter", "forge:forge_fixes"}) {
            if(pipeline.get(name) != null) {
                pipeline.remove(name);
            }
        }
    }
}
