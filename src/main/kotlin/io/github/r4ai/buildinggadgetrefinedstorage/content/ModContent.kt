package io.github.r4ai.buildinggadgetrefinedstorage.content

import com.mojang.datafixers.DSL
import io.github.r4ai.buildinggadgetrefinedstorage.BuildingGadgetRefinedStorageMod
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.FluidProxyRef
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.platform.RefinedStorageBridgeBlock
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.platform.RefinedStorageBridgeBlockEntity
import java.util.function.Supplier
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.registries.DeferredRegister

object ModContent {
    val BLOCKS: DeferredRegister<Block> = DeferredRegister.create(Registries.BLOCK, BuildingGadgetRefinedStorageMod.MOD_ID)
    val ITEMS: DeferredRegister<Item> = DeferredRegister.create(Registries.ITEM, BuildingGadgetRefinedStorageMod.MOD_ID)
    val BLOCK_ENTITY_TYPES: DeferredRegister<BlockEntityType<*>> =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BuildingGadgetRefinedStorageMod.MOD_ID)
    val DATA_COMPONENTS: DeferredRegister<DataComponentType<*>> =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, BuildingGadgetRefinedStorageMod.MOD_ID)

    val REFINED_STORAGE_BRIDGE_BLOCK = BLOCKS.register("refined_storage_bridge", Supplier {
        RefinedStorageBridgeBlock(
            BlockBehaviour.Properties.of()
                .strength(3.5f)
                .requiresCorrectToolForDrops(),
        )
    })

    val REFINED_STORAGE_BRIDGE_ITEM = ITEMS.register("refined_storage_bridge", Supplier {
        BlockItem(REFINED_STORAGE_BRIDGE_BLOCK.get(), Item.Properties())
    })

    val FLUID_PROXY_ITEM = ITEMS.register("fluid_proxy", Supplier {
        FluidProxyItem(Item.Properties().stacksTo(1))
    })

    val REFINED_STORAGE_BRIDGE_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
        "refined_storage_bridge",
        Supplier {
            BlockEntityType.Builder.of(::RefinedStorageBridgeBlockEntity, REFINED_STORAGE_BRIDGE_BLOCK.get())
                .build(DSL.remainderType())
        },
    )

    val FLUID_PROXY_REF_COMPONENT = DATA_COMPONENTS.register("fluid_proxy_ref", Supplier {
        DataComponentType.builder<FluidProxyRef>()
            .persistent(FluidProxyRef.CODEC)
            .networkSynchronized(FluidProxyRef.STREAM_CODEC)
            .build()
    })

    fun register(eventBus: IEventBus) {
        BLOCKS.register(eventBus)
        ITEMS.register(eventBus)
        BLOCK_ENTITY_TYPES.register(eventBus)
        DATA_COMPONENTS.register(eventBus)
    }

    fun fluidProxyRef(stack: ItemStack): FluidProxyRef? = stack.get(FLUID_PROXY_REF_COMPONENT.get())
}
