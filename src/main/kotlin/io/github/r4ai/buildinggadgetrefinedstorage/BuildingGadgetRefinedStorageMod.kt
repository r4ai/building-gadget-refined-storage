package io.github.r4ai.buildinggadgetrefinedstorage

import io.github.r4ai.buildinggadgetrefinedstorage.content.ModContent
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.platform.RefinedStorageBridgeBlockEntity
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.platform.BridgeLookup
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.FluidProxyFluidHandler
import net.minecraft.world.item.CreativeModeTabs
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi

@Mod(BuildingGadgetRefinedStorageMod.MOD_ID)
class BuildingGadgetRefinedStorageMod {
    init {
        ModContent.register(MOD_BUS)
        MOD_BUS.addListener(::registerCapabilities)
        MOD_BUS.addListener(::buildCreativeTabContents)
    }

    companion object {
        const val MOD_ID: String = "buildinggadgetrefinedstorage"
    }

    private fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        val bridgeType = ModContent.REFINED_STORAGE_BRIDGE_BLOCK_ENTITY.get()
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            bridgeType,
            { blockEntity: RefinedStorageBridgeBlockEntity, _ -> blockEntity.itemHandler },
        )
        event.registerBlockEntity(
            RefinedStorageNeoForgeApi.INSTANCE.getNetworkNodeContainerProviderCapability(),
            bridgeType,
            { blockEntity: RefinedStorageBridgeBlockEntity, _ -> blockEntity.getContainerProvider() },
        )
        event.registerItem(
            Capabilities.FluidHandler.ITEM,
            { stack, _ ->
                val proxyRef = ModContent.fluidProxyRef(stack) ?: return@registerItem null
                FluidProxyFluidHandler(stack, proxyRef, BridgeLookup::resolveBackend)
            },
            ModContent.FLUID_PROXY_ITEM.get(),
        )
    }

    private fun buildCreativeTabContents(event: BuildCreativeModeTabContentsEvent) {
        when (event.tabKey) {
            CreativeModeTabs.FUNCTIONAL_BLOCKS -> event.accept(ModContent.REFINED_STORAGE_BRIDGE_ITEM.get())
            CreativeModeTabs.TOOLS_AND_UTILITIES -> event.accept(ModContent.FLUID_PROXY_ITEM.get())
        }
    }
}
