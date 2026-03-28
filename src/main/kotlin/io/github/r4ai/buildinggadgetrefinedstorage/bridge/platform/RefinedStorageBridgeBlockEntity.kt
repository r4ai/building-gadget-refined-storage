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
        currentTickProvider = { level?.gameTime ?: 0L },
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

    private fun serverTick() {
        val level = level ?: return
        val currentState = level.getBlockState(blockPos)
        if (currentState.block !is RefinedStorageBridgeBlock) {
            return
        }

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
        if (shouldUpdateActiveState(
                cachedOperational = isOperational,
                blockActive = currentState.getValue(RefinedStorageBridgeBlock.ACTIVE),
                newOperational = newOperational,
            )
        ) {
            isOperational = newOperational
            updateActiveBlockState(level, currentState, newOperational)
        }
    }

    private fun updateActiveBlockState(level: Level, currentState: BlockState, active: Boolean) {
        if (!shouldReflectActiveState(isRemoved, currentState.block is RefinedStorageBridgeBlock)) {
            return
        }
        if (currentState.getValue(RefinedStorageBridgeBlock.ACTIVE) != active) {
            level.setBlock(blockPos, currentState.setValue(RefinedStorageBridgeBlock.ACTIVE, active), Block.UPDATE_ALL)
        }
    }

    companion object {
        private const val ENERGY_USAGE_PER_TICK: Long = 1L

        internal fun shouldReflectActiveState(
            isRemoved: Boolean,
            hasSameBlockEntity: Boolean,
            hasBridgeBlock: Boolean,
        ): Boolean = !isRemoved && hasSameBlockEntity && hasBridgeBlock

        internal fun shouldUpdateActiveState(
            cachedOperational: Boolean,
            blockActive: Boolean,
            newOperational: Boolean,
        ): Boolean = cachedOperational != newOperational || blockActive != newOperational

        private fun shouldReflectActiveState(
            isRemoved: Boolean,
            hasBridgeBlock: Boolean,
        ): Boolean = shouldReflectActiveState(
            isRemoved = isRemoved,
            hasSameBlockEntity = true,
            hasBridgeBlock = hasBridgeBlock,
        )

        fun tickServer(level: Level, pos: BlockPos, state: BlockState, blockEntity: RefinedStorageBridgeBlockEntity) {
            if (level.isClientSide || blockEntity.isRemoved) {
                return
            }
            if (level.getBlockEntity(pos) !== blockEntity) {
                return
            }
            if (level.getBlockState(pos).block !is RefinedStorageBridgeBlock) {
                return
            }
            blockEntity.serverTick()
        }
    }
}
