package io.github.r4ai.buildinggadgetrefinedstorage.benchmark

import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.BridgeItemHandler
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.BridgeSnapshot
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.InMemoryBridgeBackend
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.ItemResourceSnapshot
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.MinecraftTestBootstrap
import io.github.r4ai.buildinggadgetrefinedstorage.bridge.core.ProjectionBuilder
import kotlin.test.Test
import net.minecraft.core.BlockPos
import net.minecraft.core.GlobalPos
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

class BridgePerformanceBenchmark {
    companion object {
        private val ITEM_SIZES = listOf(10, 100, 1_000, 5_000)

        private val position: GlobalPos = GlobalPos.of(
            ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                ResourceLocation.withDefaultNamespace("overworld"),
            ),
            BlockPos(0, 64, 0),
        )

        private fun makeItems(n: Int): List<ItemResourceSnapshot> {
            val proto = Items.STONE.defaultInstance
            return List(n) { i -> ItemResourceSnapshot("item:%05d".format(i), proto, (i + 1).toLong()) }
        }

        private fun makeHandler(
            items: List<ItemResourceSnapshot>,
            gameTimeProvider: () -> Long,
        ): BridgeItemHandler {
            val backend = InMemoryBridgeBackend(initialItems = items)
            return BridgeItemHandler(
                backend = backend,
                gameTimeProvider = gameTimeProvider,
                bridgePositionProvider = { position },
                fluidProxyStackFactory = { ItemStack.EMPTY },
            )
        }

        private fun thresholdMs(): Long? =
            System.getProperty("benchmark.fail.threshold.ms")?.toLongOrNull()
    }

    /**
     * Scenario 1: キャッシュミス時のコスト
     *
     * ティックごとにレイアウトを再構築するケース。
     * `ProjectionBuilder.build()` の O(N log N) ソートが毎回実行される。
     */
    @Test
    fun `Scenario1 cache miss layout rebuild scales with item count`() {
        MinecraftTestBootstrap.ensure()
        val threshold = thresholdMs()

        println("\n=== Scenario 1: Cache Miss (layout rebuild per tick) ===")
        for (n in ITEM_SIZES) {
            val items = makeItems(n)
            var tick = 0L
            val handler = makeHandler(items) { tick }

            val result = BenchmarkStats.measure(
                label = "cache-miss getSlots (N=$n)",
                warmup = if (n >= 1_000) 100 else 200,
                iterations = if (n >= 5_000) 200 else 500,
            ) {
                tick++
                handler.slots
            }

            result.print()
            if (threshold != null) result.assertP99Below(threshold)
        }
    }

    /**
     * Scenario 2: キャッシュヒット時のコスト
     *
     * ティックが変わらない場合、キャッシュされたレイアウトを返すだけ。
     * アイテム数に依存しない定数時間になるはず。
     */
    @Test
    fun `Scenario2 cache hit is constant regardless of item count`() {
        MinecraftTestBootstrap.ensure()

        println("\n=== Scenario 2: Cache Hit (cached layout returned) ===")
        for (n in ITEM_SIZES) {
            val items = makeItems(n)
            val handler = makeHandler(items) { 42L }

            // キャッシュをウォームアップ
            handler.slots

            val result = BenchmarkStats.measure(
                label = "cache-hit  getSlots (N=$n)",
                warmup = 50,
                iterations = 500,
            ) {
                handler.slots
            }

            result.print()
        }
    }

    /**
     * Scenario 3: 1ティック分のフルスキャン
     *
     * Building Gadgets が実際に行う操作に近いパターン:
     * レイアウト再構築 + 全スロットに対して getStackInSlot() を呼ぶ。
     */
    @Test
    fun `Scenario3 full tick simulation rebuild plus all slot access`() {
        MinecraftTestBootstrap.ensure()
        val threshold = thresholdMs()

        println("\n=== Scenario 3: Full Tick Simulation (rebuild + scan all slots) ===")
        for (n in ITEM_SIZES) {
            val items = makeItems(n)
            var tick = 0L
            val handler = makeHandler(items) { tick }

            val result = BenchmarkStats.measure(
                label = "full-tick scan (N=$n)",
                warmup = if (n >= 1_000) 50 else 100,
                iterations = if (n >= 5_000) 100 else 300,
            ) {
                tick++
                val slotCount = handler.slots
                for (slot in 0 until slotCount) {
                    handler.getStackInSlot(slot)
                }
            }

            result.print()
            if (threshold != null) result.assertP99Below(threshold)
        }
    }

    /**
     * Scenario 4: ProjectionBuilder.build() 単体
     *
     * BridgeItemHandler のオーバーヘッドを除いた、
     * ビルド処理自体のコストを直接計測する。
     */
    @Test
    fun `Scenario4 ProjectionBuilder build in isolation`() {
        MinecraftTestBootstrap.ensure()
        val threshold = thresholdMs()

        println("\n=== Scenario 4: ProjectionBuilder.build() in isolation ===")
        for (n in ITEM_SIZES) {
            val items = makeItems(n)
            val backend = InMemoryBridgeBackend(initialItems = items)
            val snapshot = backend.snapshot()

            val result = BenchmarkStats.measure(
                label = "ProjectionBuilder.build (N=$n)",
                warmup = if (n >= 1_000) 100 else 200,
                iterations = if (n >= 5_000) 200 else 500,
            ) {
                ProjectionBuilder.build(snapshot, position)
            }

            result.print()
            if (threshold != null) result.assertP99Below(threshold)
        }
    }
}
