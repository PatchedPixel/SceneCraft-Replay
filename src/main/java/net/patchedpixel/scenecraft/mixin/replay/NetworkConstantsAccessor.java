package net.patchedpixel.scenecraft.mixin.replay;

import io.netty.util.AttributeKey;
import net.minecraftforge.network.NetworkConstants;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

// AT doesn't work with Forge class
@Mixin(value = NetworkConstants.class, remap = false)
public interface NetworkConstantsAccessor {
    @Accessor(value = "FML_NETVERSION", remap = false)
    static AttributeKey<String> sceneCraft$getFmlNetVersion() {
        throw new AssertionError();
    }
}
