package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap

object MinecraftTestBootstrap {
    private var bootstrapped: Boolean = false

    fun ensure() {
        if (!bootstrapped) {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
            bootstrapped = true
        }
    }
}

