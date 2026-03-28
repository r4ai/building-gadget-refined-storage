package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level

class BridgeItemHandlerTest {
    private val levelKey: ResourceKey<Level> = ResourceKey.create(
        net.minecraft.core.registries.Registries.DIMENSION,
        ResourceLocation.withDefaultNamespace("overworld"),
    )
    private val position: GlobalPos = GlobalPos.of(levelKey, BlockPos(0, 80, 0))
    @Test
    fun `matching item extraction succeeds`() {
        MinecraftTestBootstrap.ensure()
        val stone = ItemStack(Items.STONE)
        val backend = InMemoryBridgeBackend(
            initialItems = listOf(ItemResourceSnapshot("stone", stone, 5)),
        )
        val handler = handler(backend)

        val extracted = handler.extractItem(0, 3, false)

        assertEquals(3, extracted.count)
        assertEquals(2, backend.snapshot().itemResources.single().amount)
    }

    @Test
    fun `non matching insert into resource slot is rejected`() {
        MinecraftTestBootstrap.ensure()
        val stone = ItemStack(Items.STONE)
        val dirt = ItemStack(Items.DIRT)
        val backend = InMemoryBridgeBackend(
            initialItems = listOf(ItemResourceSnapshot("stone", stone, 5)),
        )
        val handler = handler(backend)

        val remainder = handler.insertItem(0, dirt, false)

        assertEquals(dirt, remainder)
        assertEquals(5, backend.snapshot().itemResources.single().amount)
    }

    @Test
    fun `item input slot accepts unknown items`() {
        MinecraftTestBootstrap.ensure()
        val stone = ItemStack(Items.STONE)
        val dirt = ItemStack(Items.DIRT)
        val backend = InMemoryBridgeBackend(
            initialItems = listOf(ItemResourceSnapshot("stone", stone, 5)),
        )
        val handler = handler(backend)

        val remainder = handler.insertItem(1, dirt, false)

        assertTrue(remainder.isEmpty)
        assertEquals(2, backend.snapshot().itemResources.size)
    }

    @Test
    fun `inactive backend fails closed`() {
        MinecraftTestBootstrap.ensure()
        val backend = InMemoryBridgeBackend(active = false)
        val handler = handler(backend)
        val stack = ItemStack(Items.STONE)

        assertEquals(0, handler.slots)
        assertEquals(stack, handler.insertItem(0, stack, false))
        assertTrue(handler.extractItem(0, 1, false).isEmpty)
        assertFalse(handler.isItemValid(0, stack))
    }

    @Test
    fun `layout stays stable within a tick and refreshes next tick`() {
        MinecraftTestBootstrap.ensure()
        val stone = ItemStack(Items.STONE)
        val dirt = ItemStack(Items.DIRT)
        val backend = InMemoryBridgeBackend(
            initialItems = listOf(ItemResourceSnapshot("stone", stone, 5)),
        )
        var gameTime = 0L
        val handler = BridgeItemHandler(
            backend = backend,
            gameTimeProvider = { gameTime },
            bridgePositionProvider = { position },
            fluidProxyStackFactory = { ItemStack(Items.BUCKET) },
        )

        assertEquals(3, handler.slots)
        backend.insertItem(dirt.copyWithCountSafe(1), simulate = false)

        assertEquals(3, handler.slots)

        gameTime += 1

        assertEquals(4, handler.slots)
    }

    private fun handler(backend: BridgeBackend): BridgeItemHandler = BridgeItemHandler(
        backend = backend,
        gameTimeProvider = { 0L },
        bridgePositionProvider = { position },
        fluidProxyStackFactory = { ItemStack(Items.BUCKET) },
    )
}
