package bench

import kotlinx.benchmark.*

/**
 * Stress tests at depth 1,000,000. Each pattern, with tail calls disabled, would crash on V8
 * with `RangeError: Maximum call stack size exceeded` well below this depth. These benchmarks
 * exist to demonstrate end-to-end correctness at a depth that is impossible without the feature,
 * and to give a rough per-call cost number at production-sized recursion depth.
 */
@State(Scope.Benchmark)
class DepthStress {

    @Benchmark
    fun staticMutualAt1M(): Boolean = staticEven(1_000_000)

    @Benchmark
    fun selfRecursionAt1M(): Long = selfSum(1_000_000, 0L)

    private val virtualEven = VirtualEvenImpl()
    private val virtualOdd = VirtualOddImpl()
    init {
        virtualEven.partner = virtualOdd
        virtualOdd.partner = virtualEven
    }

    @Benchmark
    fun virtualMutualAt1M(): Boolean = virtualEven.isMyKind(1_000_000)

    private val ifEven = InterfaceEvenImpl()
    private val ifOdd = InterfaceOddImpl()
    init {
        ifEven.partner = ifOdd
        ifOdd.partner = ifEven
    }

    @Benchmark
    fun interfaceMutualAt1M(): Boolean = ifEven.isMyKind(1_000_000)
}

private fun staticEven(n: Int): Boolean = if (n == 0) true else staticOdd(n - 1)
private fun staticOdd(n: Int): Boolean = if (n == 0) false else staticEven(n - 1)

private fun selfSum(n: Int, acc: Long): Long = if (n == 0) acc else selfSum(n - 1, acc + n)

abstract class VirtualParityStress {
    var partner: VirtualParityStress? = null
    abstract fun isMyKind(n: Int): Boolean
}

class VirtualEvenImpl : VirtualParityStress() {
    override fun isMyKind(n: Int): Boolean = if (n == 0) true else partner!!.isMyKind(n - 1)
}

class VirtualOddImpl : VirtualParityStress() {
    override fun isMyKind(n: Int): Boolean = if (n == 0) false else partner!!.isMyKind(n - 1)
}

interface InterfaceParityStress {
    var partner: InterfaceParityStress?
    fun isMyKind(n: Int): Boolean
}

class InterfaceEvenImpl(override var partner: InterfaceParityStress? = null) : InterfaceParityStress {
    override fun isMyKind(n: Int): Boolean = if (n == 0) true else partner!!.isMyKind(n - 1)
}

class InterfaceOddImpl(override var partner: InterfaceParityStress? = null) : InterfaceParityStress {
    override fun isMyKind(n: Int): Boolean = if (n == 0) false else partner!!.isMyKind(n - 1)
}
