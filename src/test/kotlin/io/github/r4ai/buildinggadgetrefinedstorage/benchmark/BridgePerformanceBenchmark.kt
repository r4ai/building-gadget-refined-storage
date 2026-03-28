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
        ): Pair<InMemoryBridgeBackend, BridgeItemHandler> {
            val backend = InMemoryBridgeBackend(initialItems = items)
            return backend to BridgeItemHandler(
                backend = backend,
                bridgePositionProvider = { position },
                fluidProxyStackFactory = { ItemStack.EMPTY },
            )
        }

        private fun thresholdMs(): Long? =
            System.getProperty("benchmark.fail.threshold.ms")?.toLongOrNull()
    }

    /**
     * Scenario 1: 内容変更後の強制再構築コスト
     *
     * backend revision を毎回進めて、内容変更後の cold-path を測る。
     */
    @Test
    fun `Scenario1 forced rebuild after content change scales with item count`() {
        MinecraftTestBootstrap.ensure()

        println("\n=== Scenario 1: Forced Rebuild After Content Change ===")
        for (n in ITEM_SIZES) {
            val items = makeItems(n)
            val (backend, handler) = makeHandler(items)

            val result = BenchmarkStats.measure(
                label = "forced-rebuild getSlots (N=$n)",
                warmup = if (n >= 1_000) 100 else 200,
                iterations = if (n >= 5_000) 200 else 500,
            ) {
                backend.bumpStateVersion()
                handler.slots
            }

            result.print()
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
            val (_, handler) = makeHandler(items)

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
     * Scenario 3: steady-state のフルスキャン
     *
     * Building Gadgets が実際に行う操作に近いパターン:
     * キャッシュ済みレイアウトに対して全スロットへ getStackInSlot() を呼ぶ。
     */
    @Test
    fun `Scenario3 steady state full tick scan stays below target`() {
        MinecraftTestBootstrap.ensure()
        val threshold = thresholdMs()

        println("\n=== Scenario 3: Full Tick Simulation (steady-state scan) ===")
        for (n in ITEM_SIZES) {
            val items = makeItems(n)
            val (_, handler) = makeHandler(items)
            handler.slots

            val result = BenchmarkStats.measure(
                label = "steady-state scan (N=$n)",
                warmup = if (n >= 1_000) 50 else 100,
                iterations = if (n >= 5_000) 100 else 300,
            ) {
                val slotCount = handler.slots
                for (slot in 0 until slotCount) {
                    handler.getStackInSlot(slot)
                }
            }

            result.print()
            if (threshold != null && n == ITEM_SIZES.last()) result.assertP99Below(threshold)
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
        }
    }
}
