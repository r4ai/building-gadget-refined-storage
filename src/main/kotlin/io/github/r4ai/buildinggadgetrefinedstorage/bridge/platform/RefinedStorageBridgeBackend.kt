package io.github.r4ai.buildinggadgetrefinedstorage.bridge.platform

import com.refinedmods.refinedstorage.api.core.Action
import com.refinedmods.refinedstorage.api.resource.ResourceKey as RsResourceKey
import com.refinedmods.refinedstorage.api.storage.Actor
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.BridgeBackend
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.BridgeSnapshot
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.FluidDescriptor
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.copyWithCountSafe
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.fluids.FluidStack

class RefinedStorageBridgeBackend(
    private val blockEntity: RefinedStorageBridgeBlockEntity,
) : BridgeBackend {
    override fun isActive(): Boolean = blockEntity.isOperational

    override fun snapshot(): BridgeSnapshot {
        if (!isActive()) {
            return BridgeSnapshot.EMPTY
        }
        val storage = storage() ?: return BridgeSnapshot.EMPTY
        val itemSnapshots = mutableListOf<io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.ItemResourceSnapshot>()
        val fluidSnapshots = mutableListOf<io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.FluidResourceSnapshot>()
        storage.all.forEach { resourceAmount ->
            RefinedStorageReflectionAdapter.toItemSnapshot(resourceAmount.resource, resourceAmount.amount)?.let(itemSnapshots::add)
            RefinedStorageReflectionAdapter.toFluidSnapshot(resourceAmount.resource, resourceAmount.amount)?.let(fluidSnapshots::add)
        }
        return BridgeSnapshot(itemSnapshots, fluidSnapshots)
    }

    override fun extractItem(prototype: ItemStack, amount: Int, simulate: Boolean): ItemStack {
        if (!isActive() || amount <= 0) {
            return ItemStack.EMPTY
        }
        val storage = storage() ?: return ItemStack.EMPTY
        val resource = itemResourceKey(prototype) ?: return ItemStack.EMPTY
        val extracted = storage.extract(resource, amount.toLong(), action(simulate), BRIDGE_ACTOR)
        if (extracted <= 0L) {
            return ItemStack.EMPTY
        }
        val count = extracted.coerceAtMost(amount.toLong()).coerceAtMost(prototype.maxStackSize.toLong()).toInt()
        return prototype.copyWithCountSafe(count)
    }

    override fun insertItem(stack: ItemStack, simulate: Boolean): ItemStack {
        if (!isActive() || stack.isEmpty) {
            return stack
        }
        val storage = storage() ?: return stack
        val resource = itemResourceKey(stack) ?: return stack
        val inserted = storage.insert(resource, stack.count.toLong(), action(simulate), BRIDGE_ACTOR)
        val remainder = (stack.count.toLong() - inserted).coerceAtLeast(0L).toInt()
        return if (remainder == 0) ItemStack.EMPTY else stack.copyWithCountSafe(remainder)
    }

    override fun extractFluid(descriptor: FluidDescriptor, amount: Int, simulate: Boolean): FluidStack {
        if (!isActive() || amount <= 0) {
            return FluidStack.EMPTY
        }
        val storage = storage() ?: return FluidStack.EMPTY
        val resource = RefinedStorageReflectionAdapter.toFluidResourceKey(descriptor) ?: return FluidStack.EMPTY
        val extracted = storage.extract(resource, amount.toLong(), action(simulate), BRIDGE_ACTOR)
        return descriptor.toFluidStack(extracted.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }

    override fun insertFluid(stack: FluidStack, simulate: Boolean): Int {
        if (!isActive() || stack.isEmpty) {
            return 0
        }
        val descriptor = FluidDescriptor.of(stack) ?: return 0
        val storage = storage() ?: return 0
        val resource = RefinedStorageReflectionAdapter.toFluidResourceKey(descriptor) ?: return 0
        return storage.insert(resource, stack.amount.toLong(), action(simulate), BRIDGE_ACTOR)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    private fun storage(): StorageNetworkComponent? = blockEntity.network
        ?.getComponent(StorageNetworkComponent::class.java)

    private fun itemResourceKey(stack: ItemStack): RsResourceKey? = RefinedStorageApi.INSTANCE
        .getItemResourceFactory()
        .create(stack.copyWithCountSafe(1))
        .orElse(null)
        ?.resource

    private fun action(simulate: Boolean): Action = if (simulate) Action.SIMULATE else Action.EXECUTE

    companion object {
        private val BRIDGE_ACTOR: Actor = Actor { "Building Gadget Refined Storage Bridge" }
    }
}
