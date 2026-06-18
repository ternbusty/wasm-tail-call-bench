package bench

import kotlinx.benchmark.*

/**
 * Static-dispatched mutual recursion. Top-level functions, no virtual dispatch.
 * The recursive structure is purely tail position so every call is eligible for `return_call`.
 *
 * On Node 24 / V8, without tail calls these would `RangeError` somewhere around depth 10k.
 * With tail calls, depth is unbounded so we measure throughput at a sweep of depths.
 */
@State(Scope.Benchmark)
class StaticMutualRecursion {
    @Param("100", "1000", "10000")
    var depth: Int = 0

    @Benchmark
    fun mutualEvenOdd(): Boolean = even(depth)

    private fun even(n: Int): Boolean = if (n == 0) true else odd(n - 1)
    private fun odd(n: Int): Boolean = if (n == 0) false else even(n - 1)
}
