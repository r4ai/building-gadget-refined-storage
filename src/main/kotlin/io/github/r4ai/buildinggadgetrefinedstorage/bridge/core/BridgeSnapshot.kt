package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import net.minecraft.world.item.ItemStack

data class ItemResourceSnapshot(
    val stableKey: String,
    val prototype: ItemStack,
    val amount: Long,
)

data class FluidResourceSnapshot(
    val stableKey: String,
    val descriptor: FluidDescriptor,
    val amount: Long,
)

data class BridgeSnapshot(
    val itemResources: List<ItemResourceSnapshot>,
    val fluidResources: List<FluidResourceSnapshot>,
) {
    companion object {
        val EMPTY: BridgeSnapshot = BridgeSnapshot(emptyList(), emptyList())
    }
}

