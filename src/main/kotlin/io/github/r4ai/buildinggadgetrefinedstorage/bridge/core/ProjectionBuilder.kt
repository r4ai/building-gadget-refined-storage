package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import net.minecraft.core.GlobalPos

object ProjectionBuilder {
    fun build(snapshot: BridgeSnapshot, position: GlobalPos): SlotLayout {
        val projectedItems = snapshot.itemResources
            .sortedBy(ItemResourceSnapshot::stableKey)
            .map { item ->
                ItemResourceProjectedSlot(
                    stableKey = item.stableKey,
                    prototype = item.prototype.copyWithCountSafe(1),
                    displayStack = item.prototype.copyWithCountSafe(item.displayCount()),
                    amount = item.amount,
                )
            }

        val projectedFluids = snapshot.fluidResources
            .sortedBy(FluidResourceSnapshot::stableKey)
            .map { fluid ->
                FluidResourceProjectedSlot(
                    stableKey = fluid.stableKey,
                    descriptor = fluid.descriptor,
                    proxyRef = FluidProxyRef(position, FluidProxyMode.SPECIFIC, fluid.descriptor),
                    amount = fluid.amount,
                )
            }

        return SlotLayout(
            projectedItems +
                projectedFluids +
                ItemInputProjectedSlot +
                FluidInputProjectedSlot(FluidProxyRef(position, FluidProxyMode.WILDCARD_INPUT)),
        )
    }

    private fun ItemResourceSnapshot.displayCount(): Int =
        amount.coerceAtMost(prototype.maxStackSize.toLong()).toInt().coerceAtLeast(1)
}

