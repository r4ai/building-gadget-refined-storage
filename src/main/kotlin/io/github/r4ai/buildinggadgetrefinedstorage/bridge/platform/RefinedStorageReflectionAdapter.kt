package io.github.r4ai.buildinggadgetrefinedstorage.bridge.platform

import com.refinedmods.refinedstorage.api.resource.ResourceKey as RsResourceKey
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.FluidDescriptor
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.FluidResourceSnapshot
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.ItemResourceSnapshot
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.copyWithCountSafe
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.NbtOps
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.material.Fluid

object RefinedStorageReflectionAdapter {
    private const val ITEM_RESOURCE_CLASS_NAME = "com.refinedmods.refinedstorage.common.support.resource.ItemResource"
    private const val FLUID_RESOURCE_CLASS_NAME = "com.refinedmods.refinedstorage.common.support.resource.FluidResource"

    private val itemResourceClass: Class<*> by lazy { Class.forName(ITEM_RESOURCE_CLASS_NAME) }
    private val fluidResourceClass: Class<*> by lazy { Class.forName(FLUID_RESOURCE_CLASS_NAME) }

    private val itemToItemStackMethod by lazy { itemResourceClass.getMethod("toItemStack", java.lang.Long.TYPE) }
    private val itemGetter by lazy { itemResourceClass.getMethod("item") }
    private val itemComponentsGetter by lazy { itemResourceClass.getMethod("components") }
    private val fluidGetter by lazy { fluidResourceClass.getMethod("fluid") }
    private val fluidComponentsGetter by lazy { fluidResourceClass.getMethod("components") }
    private val fluidConstructor by lazy { fluidResourceClass.getConstructor(Fluid::class.java, DataComponentPatch::class.java) }

    fun toItemSnapshot(resource: RsResourceKey, amount: Long): ItemResourceSnapshot? {
        if (!itemResourceClass.isInstance(resource)) {
            return null
        }
        val item = itemGetter.invoke(resource) as Item
        val components = itemComponentsGetter.invoke(resource) as DataComponentPatch
        val prototype = (itemToItemStackMethod.invoke(resource, 1L) as ItemStack).copyWithCountSafe(1)
        return ItemResourceSnapshot(
            stableKey = buildItemStableKey(item, components),
            prototype = prototype,
            amount = amount,
        )
    }

    fun toFluidSnapshot(resource: RsResourceKey, amount: Long): FluidResourceSnapshot? {
        if (!fluidResourceClass.isInstance(resource)) {
            return null
        }
        val fluid = fluidGetter.invoke(resource) as Fluid
        val components = fluidComponentsGetter.invoke(resource) as DataComponentPatch
        val descriptor = FluidDescriptor(BuiltInRegistries.FLUID.getKey(fluid), components)
        return FluidResourceSnapshot(
            stableKey = buildFluidStableKey(descriptor),
            descriptor = descriptor,
            amount = amount,
        )
    }

    fun toFluidResourceKey(descriptor: FluidDescriptor): RsResourceKey? {
        val fluid = descriptor.resolveFluid() ?: return null
        return fluidConstructor.newInstance(fluid, descriptor.components) as RsResourceKey
    }

    private fun buildItemStableKey(item: Item, components: DataComponentPatch): String =
        "${BuiltInRegistries.ITEM.getKey(item)}|${encodeComponents(components)}"

    private fun buildFluidStableKey(descriptor: FluidDescriptor): String =
        "${descriptor.fluidId}|${encodeComponents(descriptor.components)}"

    private fun encodeComponents(components: DataComponentPatch): String = DataComponentPatch.CODEC
        .encodeStart(NbtOps.INSTANCE, components)
        .result()
        .map(Any::toString)
        .orElse(components.toString())
}

