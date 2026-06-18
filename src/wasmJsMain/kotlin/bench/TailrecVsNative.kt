package bench

import kotlinx.benchmark.*

/**
 * Direct comparison between `tailrec` (lowered to a loop) and a self-recursive non-tailrec
 * function that gets the native `return_call` treatment. Same arithmetic body in both.
 *
 * Expected: the loop transformation is faster on most engines because it avoids the
 * frame swap entirely. This benchmark measures the gap.
 */
@State(Scope.Benchmark)
class TailrecVsNative {
    @Param("100", "1000", "10000")
    var depth: Int = 0

    @Benchmark
    fun tailrecSum(): Int = tailrecSum(depth, 0)

    @Benchmark
    fun nativeSum(): Int = nativeSum(depth, 0)

    private tailrec fun tailrecSum(n: Int, acc: Int): Int =
        if (n == 0) acc else tailrecSum(n - 1, acc + n)

    // Not marked `tailrec`, so TailrecLowering will NOT rewrite this as a loop.
    // BodyGenerator will instead emit `return_call` for the recursive site.
    private fun nativeSum(n: Int, acc: Int): Int =
        if (n == 0) acc else nativeSum(n - 1, acc + n)
}
