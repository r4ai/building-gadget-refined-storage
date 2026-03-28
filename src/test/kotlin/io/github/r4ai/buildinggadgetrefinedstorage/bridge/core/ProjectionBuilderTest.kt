package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level

class ProjectionBuilderTest {
    private val levelKey: ResourceKey<Level> = ResourceKey.create(
        net.minecraft.core.registries.Registries.DIMENSION,
        ResourceLocation.withDefaultNamespace("overworld"),
    )
    private val position: GlobalPos = GlobalPos.of(levelKey, BlockPos(10, 64, 10))

    @Test
    fun `items and fluids are projected in deterministic order`() {
        MinecraftTestBootstrap.ensure()
        val itemB = ItemStack(Items.STONE)
        val itemA = ItemStack(Items.DIRT)
        val layout = ProjectionBuilder.build(
            BridgeSnapshot(
                itemResources = listOf(
                    ItemResourceSnapshot("item-b", itemB, 2),
                    ItemResourceSnapshot("item-a", itemA, 1),
                ),
                fluidResources = listOf(
                    FluidResourceSnapshot("fluid-z", FluidDescriptor(ResourceLocation.withDefaultNamespace("lava")), 1000),
                    FluidResourceSnapshot("fluid-y", FluidDescriptor(ResourceLocation.withDefaultNamespace("water")), 2000),
                ),
            ),
            position,
        )

        assertEquals(6, layout.size)
        assertEquals(listOf("item-a", "item-b", "fluid-y", "fluid-z", "\u0000item_input", "\u0000fluid_input"), layout.slots.map(ProjectedSlot::stableKey))
        assertIs<ItemInputProjectedSlot>(layout[4])
        assertIs<FluidInputProjectedSlot>(layout[5])
    }

    @Test
    fun `item display count is clamped to max stack size`() {
        MinecraftTestBootstrap.ensure()
        val layout = ProjectionBuilder.build(
            BridgeSnapshot(
                itemResources = listOf(ItemResourceSnapshot("limited", ItemStack(Items.ENDER_PEARL), 48)),
                fluidResources = emptyList(),
            ),
            position,
        )

        val slot = assertIs<ItemResourceProjectedSlot>(layout[0])
        assertEquals(16, slot.displayStack.count)
        assertEquals(1, slot.prototype.count)
    }
}
