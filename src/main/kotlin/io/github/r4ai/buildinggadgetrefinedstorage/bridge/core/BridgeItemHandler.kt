package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import net.minecraft.core.GlobalPos
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.items.IItemHandler

class BridgeItemHandler(
    private val backend: BridgeBackend,
    private val bridgePositionProvider: () -> GlobalPos?,
    private val fluidProxyStackFactory: (FluidProxyRef) -> ItemStack,
) : IItemHandler {
    private var cachedVersion: Long = Long.MIN_VALUE
    private var cachedLayout: SlotLayout = SlotLayout.EMPTY

    override fun getSlots(): Int = currentLayout().size

    override fun getStackInSlot(slot: Int): ItemStack {
        val projected = currentLayout()[slot] ?: return ItemStack.EMPTY
        return when (projected) {
            is ItemResourceProjectedSlot -> projected.displayStack
            is FluidResourceProjectedSlot -> fluidProxyStackFactory(projected.proxyRef)
            is ItemInputProjectedSlot -> ItemStack.EMPTY
            is FluidInputProjectedSlot -> fluidProxyStackFactory(projected.proxyRef)
        }
    }

    override fun insertItem(slot: Int, stack: ItemStack, simulate: Boolean): ItemStack {
        if (stack.isEmpty || !backend.isActive()) {
            return stack
        }
        return when (val projected = currentLayout()[slot]) {
            is ItemResourceProjectedSlot -> {
                if (ItemStack.isSameItemSameComponents(projected.prototype, stack)) {
                    backend.insertItem(stack, simulate)
                } else {
                    stack
                }
            }

            is ItemInputProjectedSlot -> backend.insertItem(stack, simulate)
            is FluidResourceProjectedSlot, is FluidInputProjectedSlot -> stack
            null -> stack
        }
    }

    override fun extractItem(slot: Int, amount: Int, simulate: Boolean): ItemStack {
        if (amount <= 0 || !backend.isActive()) {
            return ItemStack.EMPTY
        }
        return when (val projected = currentLayout()[slot]) {
            is ItemResourceProjectedSlot -> backend.extractItem(projected.prototype, amount, simulate)
            else -> ItemStack.EMPTY
        }
    }

    override fun getSlotLimit(slot: Int): Int {
        val projected = currentLayout()[slot] ?: return 0
        return when (projected) {
            is ItemResourceProjectedSlot -> projected.prototype.maxStackSize
            is FluidResourceProjectedSlot, is FluidInputProjectedSlot -> 1
            is ItemInputProjectedSlot -> 64
        }
    }

    override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
        if (stack.isEmpty || !backend.isActive()) {
            return false
        }
        return when (val projected = currentLayout()[slot]) {
            is ItemResourceProjectedSlot -> ItemStack.isSameItemSameComponents(projected.prototype, stack)
            is ItemInputProjectedSlot -> true
            else -> false
        }
    }

    private fun currentLayout(): SlotLayout {
        if (!backend.isActive()) {
            cachedLayout = SlotLayout.EMPTY
            cachedVersion = Long.MIN_VALUE
            return SlotLayout.EMPTY
        }
        val currentVersion = backend.stateVersion()
        if (currentVersion != cachedVersion) {
            cachedVersion = currentVersion
            cachedLayout = bridgePositionProvider()?.let { position ->
                ProjectionBuilder.build(backend.snapshot(), position)
            } ?: SlotLayout.EMPTY
        }
        return cachedLayout
    }
}
