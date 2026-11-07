package net.patchedpixel.scenecraft.common.mixin.replay;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.PacketDecoder;
import net.patchedpixel.scenecraft.common.replay.LenientPacketDecoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Lets the replay survive packets with trailing bytes, see {@link LenientPacketDecoder}.
 * <p>
 * Only decoders on an {@link EmbeddedChannel} are affected, which is what the replay uses. Real connections (and the
 * recorder, which sits on those) keep vanilla's strict behavior.
 * <p>
 * {@code require = 0}: SceneCraft's config uses {@code defaultRequire = 1}, but a miss here must only disable this
 * safety net instead of crashing the game while loading {@code PacketDecoder}.
 */
@Mixin(PacketDecoder.class)
@SuppressWarnings("rawtypes")
public abstract class MixinPacketDecoder {
    @Inject(
            method = "decode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/codec/StreamCodec;decode(Ljava/lang/Object;)Ljava/lang/Object;",
                    shift = At.Shift.AFTER
            ),
            require = 0
    )
    private void sceneCraft$skipTrailingBytesInReplay(ChannelHandlerContext ctx, ByteBuf input, List<Object> out, CallbackInfo ci) {
        if(!(ctx.channel() instanceof EmbeddedChannel)) {
            return;
        }
        PacketDecoder self = (PacketDecoder) (Object) this;
        LenientPacketDecoder.skipTrailingBytes(self.protocolInfo.id(), input);
    }
}
