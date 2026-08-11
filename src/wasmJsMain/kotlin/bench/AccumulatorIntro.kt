package bench

import kotlinx.benchmark.*

@State(Scope.Benchmark)
class AccumulatorIntro {
    @Param("100", "1000", "10000")
    var depth: Int = 0

    @Benchmark
    fun accSum(): Int = accSum(depth)

    @Benchmark
    fun tailrecSum(): Int = tailrecSumImpl(depth, 0)

    @Benchmark
    fun accFactorial(): Int = accFactorial(depth)

    @Benchmark
    fun tailrecFactorial(): Int = tailrecFactorialImpl(depth, 1)

    @Benchmark
    fun accRepeatStr(): String = repeatStr("ab", depth)

    @Benchmark
    fun tailrecRepeatStr(): String = tailrecRepeatStrImpl("ab", depth, "")

    private fun accSum(n: Int): Int {
        if (n == 0) return 0
        return accSum(n - 1) + n
    }

    private tailrec fun tailrecSumImpl(n: Int, acc: Int): Int =
        if (n == 0) acc else tailrecSumImpl(n - 1, acc + n)

    private fun accFactorial(n: Int): Int {
        if (n <= 1) return 1
        return n * accFactorial(n - 1)
    }

    private tailrec fun tailrecFactorialImpl(n: Int, acc: Int): Int =
        if (n <= 1) acc else tailrecFactorialImpl(n - 1, acc * n)

    private fun repeatStr(s: String, n: Int): String {
        if (n == 0) return ""
        return s + repeatStr(s, n - 1)
    }

    private tailrec fun tailrecRepeatStrImpl(s: String, n: Int, acc: String): String =
        if (n == 0) acc else tailrecRepeatStrImpl(s, n - 1, acc + s)
}
