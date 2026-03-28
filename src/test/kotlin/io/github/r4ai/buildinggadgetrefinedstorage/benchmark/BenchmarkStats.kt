package io.github.r4ai.buildinggadgetrefinedstorage.benchmark

import kotlin.time.measureTime

object BenchmarkStats {
    data class Result(
        val label: String,
        val sampleCount: Int,
        val minNs: Long,
        val maxNs: Long,
        val meanNs: Double,
        val p50Ns: Long,
        val p95Ns: Long,
        val p99Ns: Long,
    ) {
        fun print() {
            println(
                "  %-55s  n=%-4d  min=%-8s  mean=%-8s  p50=%-8s  p95=%-8s  p99=%-8s  max=%s".format(
                    label,
                    sampleCount,
                    formatNs(minNs),
                    formatNs(meanNs.toLong()),
                    formatNs(p50Ns),
                    formatNs(p95Ns),
                    formatNs(p99Ns),
                    formatNs(maxNs),
                )
            )
        }

        fun assertP99Below(thresholdMs: Long) {
            val thresholdNs = thresholdMs * 1_000_000L
            check(p99Ns <= thresholdNs) {
                "[$label] p99 ${formatNs(p99Ns)} exceeded threshold ${thresholdMs}ms"
            }
        }

        private fun formatNs(ns: Long): String =
            when {
                ns >= 1_000_000L -> "%.2fms".format(ns / 1_000_000.0)
                ns >= 1_000L -> "%.2fus".format(ns / 1_000.0)
                else -> "${ns}ns"
            }
    }

    fun measure(
        label: String,
        warmup: Int = 200,
        iterations: Int = 500,
        block: () -> Unit,
    ): Result {
        repeat(warmup) { block() }

        val samples = LongArray(iterations)
        repeat(iterations) { i ->
            samples[i] = measureTime(block).inWholeNanoseconds
        }

        val sorted = samples.clone().also { it.sort() }
        val n = sorted.size
        return Result(
            label = label,
            sampleCount = n,
            minNs = sorted[0],
            maxNs = sorted[n - 1],
            meanNs = sorted.average(),
            p50Ns = sorted[n / 2],
            p95Ns = sorted[(n * 0.95).toInt().coerceAtMost(n - 1)],
            p99Ns = sorted[(n * 0.99).toInt().coerceAtMost(n - 1)],
        )
    }
}
