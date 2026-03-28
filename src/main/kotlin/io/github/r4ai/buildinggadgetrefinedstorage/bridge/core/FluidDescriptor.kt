package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import java.util.Optional
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.material.Fluid
import net.neoforged.neoforge.fluids.FluidStack

data class FluidDescriptor(
    val fluidId: ResourceLocation,
    val components: DataComponentPatch = DataComponentPatch.EMPTY,
) {
    fun resolveFluid(): Fluid? = BuiltInRegistries.FLUID.getOptional(fluidId).orElse(null)

    fun matches(stack: FluidStack): Boolean {
        if (stack.isEmpty) {
            return false
        }
        val stackId = BuiltInRegistries.FLUID.getKey(stack.fluid)
        return fluidId == stackId && components == stack.components.asPatch()
    }

    fun toFluidStack(amount: Int): FluidStack {
        if (amount <= 0) {
            return FluidStack.EMPTY
        }
        val fluid = resolveFluid() ?: return FluidStack.EMPTY
        return FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(fluid), amount, components)
    }

    companion object {
        val CODEC: Codec<FluidDescriptor> = RecordCodecBuilder.create { instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("fluid").forGetter(FluidDescriptor::fluidId),
                DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                    .forGetter(FluidDescriptor::components),
            ).apply(instance, ::FluidDescriptor)
        }

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, FluidDescriptor> = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            FluidDescriptor::fluidId,
            DataComponentPatch.STREAM_CODEC,
            FluidDescriptor::components,
            ::FluidDescriptor,
        )

        fun of(stack: FluidStack): FluidDescriptor? {
            if (stack.isEmpty) {
                return null
            }
            return FluidDescriptor(BuiltInRegistries.FLUID.getKey(stack.fluid), stack.components.asPatch())
        }
    }
}

