package bench

import kotlinx.benchmark.*
import kotlin.wasm.TailModCons

class Cell(val value: Int, var next: Cell?)

@State(Scope.Benchmark)
class TmcCtorShallow {
    @Param("100", "1000", "10000")
    var depth: Int = 0

    @Benchmark
    fun chainTmc(): Int = chainTmcImpl(depth)?.value ?: -1

    @Benchmark
    fun chainPlain(): Int = chainPlainImpl(depth)?.value ?: -1
}

@State(Scope.Benchmark)
class TmcCtorDeep {
    @Param("1000000")
    var depth: Int = 0

    @Benchmark
    fun chainTmc(): Int = chainTmcImpl(depth)?.value ?: -1
}

@TailModCons
private fun chainTmcImpl(n: Int): Cell? {
    if (n == 0) return null
    return Cell(n, chainTmcImpl(n - 1))
}

private fun chainPlainImpl(n: Int): Cell? {
    if (n == 0) return null
    return Cell(n, chainPlainImpl(n - 1))
}
