package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack

class InMemoryBridgeBackend(
    var active: Boolean = true,
    initialItems: List<ItemResourceSnapshot> = emptyList(),
    initialFluids: List<FluidResourceSnapshot> = emptyList(),
    private val itemStableKeyFactory: (ItemStack) -> String = { "item:${System.identityHashCode(it.item)}" },
    private val fluidStableKeyFactory: (FluidDescriptor) -> String = { "fluid:${it.fluidId}" },
) : BridgeBackend {
    private data class ItemState(
        val stableKey: String,
        val prototype: ItemStack,
        var amount: Long,
    )

    private data class FluidState(
        val stableKey: String,
        val descriptor: FluidDescriptor,
        var amount: Long,
    )

    private val items: MutableMap<String, ItemState> = linkedMapOf()
    private val fluids: MutableMap<String, FluidState> = linkedMapOf()
    private var revision: Long = 0L

    init {
        initialItems.forEach { items[it.stableKey] = ItemState(it.stableKey, it.prototype.copyWithCountSafe(1), it.amount) }
        initialFluids.forEach { fluids[it.stableKey] = FluidState(it.stableKey, it.descriptor, it.amount) }
    }

    override fun isActive(): Boolean = active

    override fun stateVersion(): Long = revision

    override fun snapshot(): BridgeSnapshot = BridgeSnapshot(
        itemResources = items.values.map { ItemResourceSnapshot(it.stableKey, it.prototype.copyWithCountSafe(1), it.amount) },
        fluidResources = fluids.values.map { FluidResourceSnapshot(it.stableKey, it.descriptor, it.amount) },
    )

    override fun extractItem(prototype: ItemStack, amount: Int, simulate: Boolean): ItemStack {
        if (!active || amount <= 0) {
            return ItemStack.EMPTY
        }
        val state = items.values.firstOrNull { ItemStack.isSameItemSameComponents(it.prototype, prototype) } ?: return ItemStack.EMPTY
        val extracted = state.amount.coerceAtMost(amount.toLong()).coerceAtMost(prototype.maxStackSize.toLong()).toInt()
        if (extracted <= 0) {
            return ItemStack.EMPTY
        }
        if (!simulate) {
            state.amount -= extracted.toLong()
            if (state.amount <= 0L) {
                items.remove(state.stableKey)
            }
            revision += 1
        }
        return state.prototype.copyWithCountSafe(extracted)
    }

    override fun insertItem(stack: ItemStack, simulate: Boolean): ItemStack {
        if (!active || stack.isEmpty) {
            return stack
        }
        val key = items.entries.firstOrNull { ItemStack.isSameItemSameComponents(it.value.prototype, stack) }?.key
            ?: itemStableKeyFactory(stack)
        if (!simulate) {
            val state = items.getOrPut(key) {
                ItemState(key, stack.copyWithCountSafe(1), 0)
            }
            state.amount += stack.count.toLong()
            revision += 1
        }
        return ItemStack.EMPTY
    }

    override fun extractFluid(descriptor: FluidDescriptor, amount: Int, simulate: Boolean): FluidStack {
        if (!active || amount <= 0) {
            return FluidStack.EMPTY
        }
        val state = fluids.values.firstOrNull { it.descriptor == descriptor } ?: return FluidStack.EMPTY
        val extracted = state.amount.coerceAtMost(amount.toLong()).toInt()
        if (extracted <= 0) {
            return FluidStack.EMPTY
        }
        if (!simulate) {
            state.amount -= extracted.toLong()
            if (state.amount <= 0L) {
                fluids.remove(state.stableKey)
            }
            revision += 1
        }
        return descriptor.toFluidStack(extracted)
    }

    override fun insertFluid(stack: FluidStack, simulate: Boolean): Int {
        if (!active || stack.isEmpty) {
            return 0
        }
        val descriptor = FluidDescriptor.of(stack) ?: return 0
        val key = fluids.entries.firstOrNull { it.value.descriptor == descriptor }?.key ?: fluidStableKeyFactory(descriptor)
        if (!simulate) {
            val state = fluids.getOrPut(key) { FluidState(key, descriptor, 0L) }
            state.amount += stack.amount.toLong()
            revision += 1
        }
        return stack.amount
    }

    fun bumpStateVersion() {
        revision += 1
    }
}
