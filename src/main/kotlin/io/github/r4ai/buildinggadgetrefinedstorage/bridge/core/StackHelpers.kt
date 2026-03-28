package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack

fun ItemStack.copyWithCountSafe(count: Int): ItemStack {
    val copy = copy()
    copy.count = count
    return copy
}

fun FluidStack.copyWithAmountSafe(amount: Int): FluidStack {
    if (isEmpty || amount <= 0) {
        return FluidStack.EMPTY
    }
    return FluidStack(BuiltInRegistries.FLUID.wrapAsHolder(fluid), amount, components.asPatch())
}

