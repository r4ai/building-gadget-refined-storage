package io.github.r4ai.buildinggadgetrefinedstorage.bridge.core

import net.minecraft.SharedConstants
import net.minecraft.server.Bootstrap

object MinecraftTestBootstrap {
    private var bootstrapped: Boolean = false

    fun ensure() {
        if (!bootstrapped) {
            initFmlLoadingModList()
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
            bootstrapped = true
        }
    }

    /**
     * NeoForge の FeatureFlags 静的初期化子が LoadingModList.get() を呼び出す。
     * テスト環境では FML が起動していないため null になり ExceptionInInitializerError が発生する。
     * ここで空の LoadingModList を作成して INSTANCE にセットすることで Bootstrap.bootStrap() が通るようにする。
     */
    private fun initFmlLoadingModList() {
        runCatching {
            val clazz = Class.forName("net.neoforged.fml.loading.LoadingModList")
            val instanceField = clazz.getDeclaredField("INSTANCE")
            instanceField.isAccessible = true
            if (instanceField.get(null) != null) return@runCatching

            val ofMethod = clazz.getDeclaredMethod(
                "of",
                List::class.java,
                List::class.java,
                List::class.java,
                List::class.java,
                Map::class.java,
            )
            ofMethod.invoke(null, emptyList<Any>(), emptyList<Any>(), emptyList<Any>(), emptyList<Any>(), emptyMap<Any, Any>())
        }
    }
}

