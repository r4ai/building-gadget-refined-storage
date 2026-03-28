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

class SlotLayout private constructor(
    private val entries: Array<ProjectedSlot>,
) {
    constructor(slots: List<ProjectedSlot>) : this(slots.toTypedArray())

    val slots: List<ProjectedSlot>
        get() = entries.asList()

    val size: Int
        get() = entries.size

    val itemInputIndex: Int
        get() = entries.indexOfFirst { it is ItemInputProjectedSlot }

    val fluidInputIndex: Int
        get() = entries.indexOfFirst { it is FluidInputProjectedSlot }

    operator fun get(index: Int): ProjectedSlot? = entries.getOrNull(index)

    companion object {
        val EMPTY: SlotLayout = SlotLayout(emptyArray())
    }
}
