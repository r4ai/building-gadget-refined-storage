package io.github.r4ai.buildinggadgetrefinedstorage.bridge.platform

import io.github.r4ai.buildinggadgetrefinedstorage.BuildingGadgetRefinedStorageMod
import net.minecraft.network.chat.Component

enum class RefinedStorageBridgeStatus {
    CONNECTED,
    INACTIVE,
    DISCONNECTED,
    ;

    fun message(): Component = when (this) {
        CONNECTED -> Component.translatable("message.${BuildingGadgetRefinedStorageMod.MOD_ID}.connected")
        INACTIVE -> Component.translatable("message.${BuildingGadgetRefinedStorageMod.MOD_ID}.inactive")
        DISCONNECTED -> Component.translatable("message.${BuildingGadgetRefinedStorageMod.MOD_ID}.disconnected")
    }
}

