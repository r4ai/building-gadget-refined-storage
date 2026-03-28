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
}
