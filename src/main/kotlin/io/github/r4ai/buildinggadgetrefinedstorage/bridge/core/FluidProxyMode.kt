package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import com.mojang.serialization.Codec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec

enum class FluidProxyMode(private val wireName: String) {
    SPECIFIC("specific"),
    WILDCARD_INPUT("wildcard_input"),
    ;

    fun serializedName(): String = wireName

    companion object {
        val CODEC: Codec<FluidProxyMode> = Codec.STRING.xmap(::fromSerializedName, FluidProxyMode::serializedName)

        val STREAM_CODEC: StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, FluidProxyMode> =
            StreamCodec.of(
                { buffer, value -> ByteBufCodecs.STRING_UTF8.encode(buffer, value.serializedName()) },
                { buffer -> fromSerializedName(ByteBufCodecs.STRING_UTF8.decode(buffer)) },
            )

        fun fromSerializedName(name: String): FluidProxyMode = entries.firstOrNull { it.wireName == name }
            ?: throw IllegalArgumentException("Unknown fluid proxy mode: $name")
    }
}
