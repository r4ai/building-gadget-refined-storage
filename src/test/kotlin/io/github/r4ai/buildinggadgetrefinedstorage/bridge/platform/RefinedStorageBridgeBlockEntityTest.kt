package io.github.r4ai.buildinggadgetrefinedstorage.bridge.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefinedStorageBridgeBlockEntityTest {
    @Test
    fun `active state reflection only runs for live bridge block entity`() {
        assertTrue(
            RefinedStorageBridgeBlockEntity.shouldReflectActiveState(
                isRemoved = false,
                hasSameBlockEntity = true,
                hasBridgeBlock = true,
            ),
        )

        assertFalse(
            RefinedStorageBridgeBlockEntity.shouldReflectActiveState(
                isRemoved = true,
                hasSameBlockEntity = true,
                hasBridgeBlock = true,
            ),
        )

        assertFalse(
            RefinedStorageBridgeBlockEntity.shouldReflectActiveState(
                isRemoved = false,
                hasSameBlockEntity = false,
                hasBridgeBlock = true,
            ),
        )

        assertFalse(
            RefinedStorageBridgeBlockEntity.shouldReflectActiveState(
                isRemoved = false,
                hasSameBlockEntity = true,
                hasBridgeBlock = false,
            ),
        )
    }

    @Test
    fun `active state update runs when persisted lamp state differs from operational state`() {
        assertTrue(
            RefinedStorageBridgeBlockEntity.shouldUpdateActiveState(
                cachedOperational = false,
                blockActive = true,
                newOperational = false,
            ),
        )

        assertTrue(
            RefinedStorageBridgeBlockEntity.shouldUpdateActiveState(
                cachedOperational = false,
                blockActive = false,
                newOperational = true,
            ),
        )

        assertFalse(
            RefinedStorageBridgeBlockEntity.shouldUpdateActiveState(
                cachedOperational = false,
                blockActive = false,
                newOperational = false,
            ),
        )
    }
}
