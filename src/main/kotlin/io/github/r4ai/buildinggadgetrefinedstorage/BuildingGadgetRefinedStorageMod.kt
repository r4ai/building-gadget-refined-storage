package io.github.r4ai.buildinggadgetrefinedstorage

import io.github.r4ai.buildinggadgetrefinedstorage.content.ModContent
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi

@Mod(BuildingGadgetRefinedStorageMod.MOD_ID)
class BuildingGadgetRefinedStorageMod {
    init {
        ModContent.register(MOD_BUS)
        MOD_BUS.addListener(::registerCapabilities)
    }

    companion object {
        const val MOD_ID: String = "buildinggadgetrefinedstorage"
    }

    private fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        val bridgeType = ModContent.REFINED_STORAGE_BRIDGE_BLOCK_ENTITY.get()
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            bridgeType,
            NullSafeCapabilityProviders.itemHandlerProvider(),
        )
        event.registerBlockEntity(
            RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(),
            bridgeType,
            NullSafeCapabilityProviders.networkNodeContainerProvider(),
        )
        event.registerItem(
            Capabilities.FluidHandler.ITEM,
            NullSafeCapabilityProviders.fluidProxyItemProvider(),
            ModContent.FLUID_PROXY_ITEM.get(),
        )
    }


}
