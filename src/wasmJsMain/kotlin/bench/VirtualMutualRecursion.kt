package bench

import kotlinx.benchmark.*

/**
 * Open class virtual dispatch tail call. The vtable dispatch path emits `return_call_ref`.
 * Two siblings of the same base class call each other through the base.
 */
@State(Scope.Benchmark)
class VirtualMutualRecursion {
    @Param("100", "1000", "10000")
    var depth: Int = 0

    private val even = EvenImpl()
    private val odd = OddImpl()

    init {
        even.partner = odd
        odd.partner = even
    }

    @Benchmark
    fun mutualEvenOdd(): Boolean = even.test(depth)

    abstract class Base {
        var partner: Base? = null
        abstract fun test(n: Int): Boolean
    }

    class EvenImpl : Base() {
        override fun test(n: Int): Boolean = if (n == 0) true else partner!!.test(n - 1)
    }

    class OddImpl : Base() {
        override fun test(n: Int): Boolean = if (n == 0) false else partner!!.test(n - 1)
    }
}
