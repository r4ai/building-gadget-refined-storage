package io.github.r4ai.buildinggadgetrefinedstorage.bridge.platform

import com.refinedmods.refinedstorage.api.core.Action
import com.refinedmods.refinedstorage.api.resource.ResourceAmount
import com.refinedmods.refinedstorage.api.resource.ResourceKey
import com.refinedmods.refinedstorage.api.resource.list.MutableResourceList
import com.refinedmods.refinedstorage.api.storage.Actor
import com.refinedmods.refinedstorage.api.storage.root.RootStorageListener
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.BridgeSnapshot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RefinedStorageBridgeBackendTest {
    @Test
    fun `state version tracks listener changes and storage swaps`() {
        val storageA = FakeStorageAccess()
        val storageB = FakeStorageAccess()
        var active = true
        var binding: RefinedStorageBridgeBackend.StorageBinding? =
            RefinedStorageBridgeBackend.StorageBinding("A", storageA)

        val backend = RefinedStorageBridgeBackend(
            activeProvider = { active },
            storageBindingProvider = { binding },
            itemResourceKeyProvider = { null },
        )

        assertEquals(1L, backend.stateVersion())
        assertNotNull(storageA.listener)
        assertNull(storageB.listener)

        assertEquals(1L, backend.stateVersion())

        storageA.emitChange()
        assertEquals(2L, backend.stateVersion())

        binding = RefinedStorageBridgeBackend.StorageBinding("B", storageB)
        assertEquals(3L, backend.stateVersion())
        assertNull(storageA.listener)
        assertNotNull(storageB.listener)

        binding = null
        assertEquals(4L, backend.stateVersion())
        assertNull(storageB.listener)

        active = false
        assertEquals(BridgeSnapshot.EMPTY, backend.snapshot())
    }

    private class FakeStorageAccess : RefinedStorageBridgeBackend.BridgeStorageAccess {
        override val all: Collection<ResourceAmount> = emptyList()
        var listener: RootStorageListener? = null

        override fun extract(resource: ResourceKey, amount: Long, action: Action, actor: Actor): Long = 0

        override fun insert(resource: ResourceKey, amount: Long, action: Action, actor: Actor): Long = 0

        override fun addListener(listener: RootStorageListener) {
            this.listener = listener
        }

        override fun removeListener(listener: RootStorageListener) {
            if (this.listener === listener) {
                this.listener = null
            }
        }

        fun emitChange() {
            listener?.changed(MutableResourceList.OperationResult(FakeResourceKey, 1L, 1L, true))
        }
    }

    private data object FakeResourceKey : ResourceKey
}
