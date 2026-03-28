package io.github.r4ai.buildinggadgetrefinedstorage.bridge.platform

import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.BridgeBackend
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.FluidProxyRef
import net.neoforged.neoforge.server.ServerLifecycleHooks

object BridgeLookup {
    fun resolveBackend(proxyRef: FluidProxyRef): BridgeBackend? {
        val server = ServerLifecycleHooks.getCurrentServer() ?: return null
        val level = server.getLevel(proxyRef.position.dimension) ?: return null
        val blockEntity = level.getBlockEntity(proxyRef.position.pos()) as? RefinedStorageBridgeBlockEntity ?: return null
        return blockEntity.backend
    }
}

