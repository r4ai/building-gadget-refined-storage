package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.util.Optional
import net.minecraft.core.GlobalPos
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

data class FluidProxyRef(
    val position: GlobalPos,
    val mode: FluidProxyMode,
    val descriptor: FluidDescriptor? = null,
) {
    companion object {
        val CODEC: Codec<FluidProxyRef> = RecordCodecBuilder.create { instance ->
            instance.group(
                GlobalPos.CODEC.fieldOf("position").forGetter(FluidProxyRef::position),
                FluidProxyMode.CODEC.fieldOf("mode").forGetter(FluidProxyRef::mode),
                FluidDescriptor.CODEC.optionalFieldOf("descriptor")
                    .forGetter { ref -> Optional.ofNullable(ref.descriptor) },
            ).apply(instance) { position, mode, descriptor ->
                FluidProxyRef(position, mode, descriptor.orElse(null))
            }
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, FluidProxyRef> = StreamCodec.composite(
            GlobalPos.STREAM_CODEC,
            FluidProxyRef::position,
            FluidProxyMode.STREAM_CODEC,
            FluidProxyRef::mode,
            ByteBufCodecs.optional(FluidDescriptor.STREAM_CODEC),
            { ref -> Optional.ofNullable(ref.descriptor) },
            { position, mode, descriptor -> FluidProxyRef(position, mode, descriptor.orElse(null)) },
        )
    }
}
