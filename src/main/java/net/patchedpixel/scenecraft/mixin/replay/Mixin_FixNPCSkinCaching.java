package net.patchedpixel.scenecraft.mixin.replay;

import net.patchedpixel.scenecraft.core.versions.MCVer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(AbstractClientPlayer.class)
public abstract class Mixin_FixNPCSkinCaching {
    @Shadow @Nullable protected abstract PlayerInfo getPlayerInfo();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void forceCachePlayerListEntry(CallbackInfo ci) {
        // Server-side NPCs (without a client mod) rely on the player list entry being cached in this class to function
        // because the remove the entry shortly after spawning the player. However this method is only called when the
        // player is rendered, and if we are currently jumping around in a replay then it most likely will not be
        // rendered while the entry is still there. The result is that most NPCs get Steve/Alex skins instead of the
        // intended ones. To fix that, we make this caching-glitch which servers have come to rely on an actual feature
        // by just fetching the cache in the constructor which arguably is what MC should have done to begin with,
        // especially because the spawn packet handling code already requires the entry to be present).

        // To reduce the chance of incompatibility with custom player entities, we only do this for the vanilla MP one.
        //noinspection ConstantConditions
        if (!(((Object) this) instanceof RemotePlayer)) return;

        // To get the player list entry, we need to be connected (we usually are, but better be safe than sorry)
        if (MCVer.getMinecraft().getConnection() == null) return;

        // And we catch any exceptions, so if there is still something, it's hopefully not fatal
        try {
            this.getPlayerInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
