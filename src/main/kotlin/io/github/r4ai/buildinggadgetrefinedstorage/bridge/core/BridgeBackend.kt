package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack

interface BridgeBackend {
    fun isActive(): Boolean

    fun snapshot(): BridgeSnapshot

    fun extractItem(prototype: ItemStack, amount: Int, simulate: Boolean): ItemStack

    fun insertItem(stack: ItemStack, simulate: Boolean): ItemStack

    fun extractFluid(descriptor: FluidDescriptor, amount: Int, simulate: Boolean): FluidStack

    fun insertFluid(stack: FluidStack, simulate: Boolean): Int
}

