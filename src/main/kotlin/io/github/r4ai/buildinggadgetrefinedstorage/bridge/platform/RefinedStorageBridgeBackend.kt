package io.github.r4ai.buildinggadgetrefinedstorage.bridge.platform

import com.refinedmods.refinedstorage.api.core.Action
import com.refinedmods.refinedstorage.api.resource.ResourceAmount
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList
import com.refinedmods.refinedstorage.api.storage.root.RootStorageListener
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

class RefinedStorageBridgeBackend internal constructor(
    private val activeProvider: () -> Boolean,
    private val storageBindingProvider: () -> StorageBinding?,
    private val itemResourceKeyProvider: (ItemStack) -> RsResourceKey?,
) : BridgeBackend {
    constructor(
        blockEntity: RefinedStorageBridgeBlockEntity,
    ) : this(
        activeProvider = { blockEntity.isOperational },
        storageBindingProvider = {
            blockEntity.network
                ?.getComponent(StorageNetworkComponent::class.java)
                ?.let { storage -> StorageBinding(storage, StorageNetworkComponentAccess(storage)) }
        },
        itemResourceKeyProvider = { stack ->
            RefinedStorageApi.INSTANCE
                .getItemResourceFactory()
                .create(stack.copyWithCountSafe(1))
                .orElse(null)
                ?.resource
        },
    )

    private var stateVersion: Long = 0L
    private var attachedStorageIdentity: Any? = null
    private var attachedStorage: BridgeStorageAccess? = null
    private val storageListener = object : RootStorageListener {
        override fun changed(result: MutableResourceList.OperationResult) {
            stateVersion += 1
        }
    }

    override fun isActive(): Boolean = activeProvider()

    override fun stateVersion(): Long {
        syncStorageBinding()
        return stateVersion
    }

    override fun snapshot(): BridgeSnapshot {
        if (!isActive()) {
            return BridgeSnapshot.EMPTY
        }
        val storage = storage() ?: return BridgeSnapshot.EMPTY
        val itemSnapshots = mutableListOf<io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.ItemResourceSnapshot>()
        val fluidSnapshots = mutableListOf<io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.FluidResourceSnapshot>()
        storage.all.forEach { resourceAmount ->
            RefinedStorageReflectionAdapter.toItemSnapshot(resourceAmount.resource(), resourceAmount.amount())?.let(itemSnapshots::add)
            RefinedStorageReflectionAdapter.toFluidSnapshot(resourceAmount.resource(), resourceAmount.amount())?.let(fluidSnapshots::add)
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

    private fun storage(): BridgeStorageAccess? {
        syncStorageBinding()
        return attachedStorage
    }

    private fun syncStorageBinding() {
        val binding = storageBindingProvider()
        if (binding?.identity === attachedStorageIdentity) {
            return
        }
        attachedStorage?.removeListener(storageListener)
        attachedStorageIdentity = binding?.identity
        attachedStorage = binding?.storage
        attachedStorage?.addListener(storageListener)
        stateVersion += 1
    }

    private fun itemResourceKey(stack: ItemStack): RsResourceKey? =
        itemResourceKeyProvider(stack)

    private fun action(simulate: Boolean): Action = if (simulate) Action.SIMULATE else Action.EXECUTE

    internal data class StorageBinding(
        val identity: Any,
        val storage: BridgeStorageAccess,
    )

    internal interface BridgeStorageAccess {
        val all: Collection<ResourceAmount>

        fun extract(resource: RsResourceKey, amount: Long, action: Action, actor: Actor): Long

        fun insert(resource: RsResourceKey, amount: Long, action: Action, actor: Actor): Long

        fun addListener(listener: RootStorageListener)

        fun removeListener(listener: RootStorageListener)
    }

    private class StorageNetworkComponentAccess(
        private val delegate: StorageNetworkComponent,
    ) : BridgeStorageAccess {
        override val all: Collection<ResourceAmount>
            get() = delegate.all

        override fun extract(resource: RsResourceKey, amount: Long, action: Action, actor: Actor): Long =
            delegate.extract(resource, amount, action, actor)

        override fun insert(resource: RsResourceKey, amount: Long, action: Action, actor: Actor): Long =
            delegate.insert(resource, amount, action, actor)

        override fun addListener(listener: RootStorageListener) {
            delegate.addListener(listener)
        }

        override fun removeListener(listener: RootStorageListener) {
            delegate.removeListener(listener)
        }
    }
 
    companion object {
        private val BRIDGE_ACTOR: Actor = Actor { "Building Gadget Refined Storage Bridge" }
    }
}
