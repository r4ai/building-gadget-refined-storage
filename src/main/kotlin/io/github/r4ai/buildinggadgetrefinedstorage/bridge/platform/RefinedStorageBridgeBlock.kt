package io.github.r4ai.buildinggadgetrefinedstorage.bridge.platform

import io.github.r4ai.buildinggadgetrefinedstorage.content.ModContent
import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult

class RefinedStorageBridgeBlock(properties: Properties) : Block(properties), EntityBlock {
    init {
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState>) {
        builder.add(ACTIVE)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity =
        RefinedStorageBridgeBlockEntity(pos, state)

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult,
    ): InteractionResult {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS
        }
        val blockEntity = level.getBlockEntity(pos) as? RefinedStorageBridgeBlockEntity ?: return InteractionResult.PASS
        player.displayClientMessage(blockEntity.status().message(), true)
        return InteractionResult.SUCCESS
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        neighborBlock: net.minecraft.world.level.block.Block,
        neighborPos: BlockPos,
        movedByPiston: Boolean,
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston)
        if (!level.isClientSide) {
            (level.getBlockEntity(pos) as? RefinedStorageBridgeBlockEntity)?.updateConnections()
        }
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>,
    ): BlockEntityTicker<T>? {
        val expectedType = ModContent.REFINED_STORAGE_BRIDGE_BLOCK_ENTITY.get()
        if (blockEntityType != expectedType) {
            return null
        }
        return BlockEntityTicker { tickLevel, tickPos, tickState, blockEntity ->
            @Suppress("UNCHECKED_CAST")
            RefinedStorageBridgeBlockEntity.tickServer(
                tickLevel,
                tickPos,
                tickState,
                blockEntity as RefinedStorageBridgeBlockEntity,
            )
        }
    }

    companion object {
        val ACTIVE: BooleanProperty = BlockStateProperties.LIT
    }
}
