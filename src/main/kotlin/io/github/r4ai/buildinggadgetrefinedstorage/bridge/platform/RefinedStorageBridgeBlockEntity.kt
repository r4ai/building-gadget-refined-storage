package io.github.r4ai.buildinggadgetrefinedstorage.bridge.platform

import com.refinedmods.refinedstorage.api.network.energy.EnergyNetworkComponent
import com.refinedmods.refinedstorage.common.api.support.network.AbstractNetworkNodeContainerBlockEntity
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.BridgeItemHandler
import io.github.r4ai.buildinggadgetrefinedstorage.content.ModContent
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class RefinedStorageBridgeBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : AbstractNetworkNodeContainerBlockEntity<RefinedStorageBridgeNetworkNode>(
    ModContent.REFINED_STORAGE_BRIDGE_BLOCK_ENTITY.get(),
    pos,
    state,
    RefinedStorageBridgeNetworkNode(),
) {
    val backend = RefinedStorageBridgeBackend(this)
    val itemHandler = BridgeItemHandler(
        backend = backend,
        bridgePositionProvider = {
            val currentLevel = level ?: return@BridgeItemHandler null
            GlobalPos.of(currentLevel.dimension(), blockPos)
        },
        fluidProxyStackFactory = { ref ->
            ModContent.FLUID_PROXY_ITEM.get().createStack(ref)
        },
    )

    var isOperational: Boolean = false
        private set

    val network get() = mainNetworkNode.network

    fun status(): RefinedStorageBridgeStatus = when {
        mainNetworkNode.network == null -> RefinedStorageBridgeStatus.DISCONNECTED
        isOperational -> RefinedStorageBridgeStatus.CONNECTED
        else -> RefinedStorageBridgeStatus.INACTIVE
    }

    fun updateConnections() {
        containerProvider.update(level)
    }

    private fun serverTick(state: BlockState) {
        val network = mainNetworkNode.network
        val newOperational = if (network == null) {
            false
        } else {
            val energy = network.getComponent(EnergyNetworkComponent::class.java)
            if (energy.stored >= ENERGY_USAGE_PER_TICK) {
                energy.extract(ENERGY_USAGE_PER_TICK)
                true
            } else {
                false
            }
        }

        mainNetworkNode.setActive(newOperational)
        if (newOperational != isOperational) {
            isOperational = newOperational
            if (level != null && state.getValue(RefinedStorageBridgeBlock.ACTIVE) != newOperational) {
                level?.setBlock(blockPos, state.setValue(RefinedStorageBridgeBlock.ACTIVE, newOperational), Block.UPDATE_ALL)
            }
        }
    }

    companion object {
        private const val ENERGY_USAGE_PER_TICK: Long = 1L

        fun tickServer(level: Level, pos: BlockPos, state: BlockState, blockEntity: RefinedStorageBridgeBlockEntity) {
            if (level.isClientSide) {
                return
            }
            blockEntity.serverTick(state)
        }
    }
}
