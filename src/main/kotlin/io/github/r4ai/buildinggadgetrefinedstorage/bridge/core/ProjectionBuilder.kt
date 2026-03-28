package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import net.minecraft.core.GlobalPos

object ProjectionBuilder {
    fun build(snapshot: BridgeSnapshot, position: GlobalPos): SlotLayout {
        val projectedItems = snapshot.itemResources
            .sortedBy(ItemResourceSnapshot::stableKey)
        val projectedFluids = snapshot.fluidResources
            .sortedBy(FluidResourceSnapshot::stableKey)
        val slots = ArrayList<ProjectedSlot>(projectedItems.size + projectedFluids.size + 2)

        projectedItems.forEach { item ->
            slots += ItemResourceProjectedSlot(
                stableKey = item.stableKey,
                prototype = item.prototype.copyWithCountSafe(1),
                displayStack = item.prototype.copyWithCountSafe(item.displayCount()),
                amount = item.amount,
            )
        }

        projectedFluids.forEach { fluid ->
            slots += FluidResourceProjectedSlot(
                stableKey = fluid.stableKey,
                descriptor = fluid.descriptor,
                proxyRef = FluidProxyRef(position, FluidProxyMode.SPECIFIC, fluid.descriptor),
                amount = fluid.amount,
            )
        }

        slots += ItemInputProjectedSlot
        slots += FluidInputProjectedSlot(FluidProxyRef(position, FluidProxyMode.WILDCARD_INPUT))

        return SlotLayout(slots)
    }

    private fun ItemResourceSnapshot.displayCount(): Int =
        amount.coerceAtMost(prototype.maxStackSize.toLong()).toInt().coerceAtLeast(1)
}
