package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import net.minecraft.world.item.ItemStack

sealed interface ProjectedSlot {
    val stableKey: String
}

data class ItemResourceProjectedSlot(
    override val stableKey: String,
    val prototype: ItemStack,
    val displayStack: ItemStack,
    val amount: Long,
) : ProjectedSlot

data class FluidResourceProjectedSlot(
    override val stableKey: String,
    val descriptor: FluidDescriptor,
    val proxyRef: FluidProxyRef,
    val amount: Long,
) : ProjectedSlot

data object ItemInputProjectedSlot : ProjectedSlot {
    override val stableKey: String = "\u0000item_input"
}

data class FluidInputProjectedSlot(
    val proxyRef: FluidProxyRef,
) : ProjectedSlot {
    override val stableKey: String = "\u0000fluid_input"
}

data class SlotLayout(
    val slots: List<ProjectedSlot>,
) {
    val size: Int
        get() = slots.size

    val itemInputIndex: Int
        get() = slots.indexOfFirst { it is ItemInputProjectedSlot }

    val fluidInputIndex: Int
        get() = slots.indexOfFirst { it is FluidInputProjectedSlot }

    operator fun get(index: Int): ProjectedSlot? = slots.getOrNull(index)

    companion object {
        val EMPTY: SlotLayout = SlotLayout(emptyList())
    }
}

