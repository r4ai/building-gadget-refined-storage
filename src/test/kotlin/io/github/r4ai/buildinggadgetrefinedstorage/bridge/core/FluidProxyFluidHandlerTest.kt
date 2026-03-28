package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.neoforged.neoforge.fluids.FluidStack
import net.neoforged.neoforge.fluids.capability.IFluidHandler
import net.minecraft.world.level.material.Fluids

class FluidProxyFluidHandlerTest {
    private val levelKey: ResourceKey<Level> = ResourceKey.create(
        net.minecraft.core.registries.Registries.DIMENSION,
        ResourceLocation.withDefaultNamespace("overworld"),
    )
    private val position: GlobalPos = GlobalPos.of(levelKey, BlockPos(1, 64, 1))
    @Test
    fun `specific proxy drains only matching fluid`() {
        MinecraftTestBootstrap.ensure()
        val water = FluidDescriptor(ResourceLocation.withDefaultNamespace("water"))
        val backend = InMemoryBridgeBackend(
            initialFluids = listOf(FluidResourceSnapshot("water", water, 2_000)),
        )
        val handler = FluidProxyFluidHandler(
            container = ItemStack(Items.BUCKET),
            proxyRef = FluidProxyRef(position, FluidProxyMode.SPECIFIC, water),
            backendResolver = { backend },
        )

        val rejected = handler.drain(FluidStack(Fluids.LAVA, 1_000), IFluidHandler.FluidAction.SIMULATE)
        val drained = handler.drain(1_000, IFluidHandler.FluidAction.EXECUTE)

        assertTrue(rejected.isEmpty)
        assertEquals(1_000, drained.amount)
        assertEquals("minecraft:water", ResourceLocation.withDefaultNamespace("water").toString())
        assertEquals(1_000, backend.snapshot().fluidResources.single().amount)
    }

    @Test
    fun `specific proxy fills only matching fluid`() {
        MinecraftTestBootstrap.ensure()
        val water = FluidDescriptor(ResourceLocation.withDefaultNamespace("water"))
        val backend = InMemoryBridgeBackend()
        val handler = FluidProxyFluidHandler(
            container = ItemStack(Items.BUCKET),
            proxyRef = FluidProxyRef(position, FluidProxyMode.SPECIFIC, water),
            backendResolver = { backend },
        )

        val lavaInserted = handler.fill(FluidStack(Fluids.LAVA, 1_000), IFluidHandler.FluidAction.EXECUTE)
        val waterInserted = handler.fill(FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE)

        assertEquals(0, lavaInserted)
        assertEquals(1_000, waterInserted)
        assertEquals(1_000, backend.snapshot().fluidResources.single().amount)
    }

    @Test
    fun `wildcard proxy accepts arbitrary fluid input`() {
        MinecraftTestBootstrap.ensure()
        val backend = InMemoryBridgeBackend()
        val handler = FluidProxyFluidHandler(
            container = ItemStack(Items.BUCKET),
            proxyRef = FluidProxyRef(position, FluidProxyMode.WILDCARD_INPUT),
            backendResolver = { backend },
        )

        val inserted = handler.fill(FluidStack(Fluids.LAVA, 1_000), IFluidHandler.FluidAction.EXECUTE)

        assertEquals(1_000, inserted)
        assertEquals(1, backend.snapshot().fluidResources.size)
        assertEquals(1_000, backend.snapshot().fluidResources.single().amount)
    }

    @Test
    fun `missing or inactive backend fails closed`() {
        MinecraftTestBootstrap.ensure()
        val water = FluidDescriptor(ResourceLocation.withDefaultNamespace("water"))
        val inactive = InMemoryBridgeBackend(active = false)
        val handler = FluidProxyFluidHandler(
            container = ItemStack(Items.BUCKET),
            proxyRef = FluidProxyRef(position, FluidProxyMode.SPECIFIC, water),
            backendResolver = { inactive },
        )

        assertEquals(0, handler.fill(FluidStack(Fluids.WATER, 1_000), IFluidHandler.FluidAction.EXECUTE))
        assertTrue(handler.drain(1_000, IFluidHandler.FluidAction.EXECUTE).isEmpty)
    }
}
