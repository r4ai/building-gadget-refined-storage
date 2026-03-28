package io.github.r4ai.buildinggadgetrefinedstorage.content

import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.FluidProxyRef
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

class FluidProxyItem(properties: Properties) : Item(properties) {
    fun createStack(proxyRef: FluidProxyRef): ItemStack = ItemStack(this).apply {
        set(ModContent.FLUID_PROXY_REF_COMPONENT.get(), proxyRef)
    }
}

