package bench

import kotlinx.benchmark.*

/**
 * Interface dispatch tail call. The itable dispatch path emits `return_call_ref`.
 */
@State(Scope.Benchmark)
class InterfaceMutualRecursion {
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

    interface Tester {
        var partner: Tester?
        fun test(n: Int): Boolean
    }

    class EvenImpl : Tester {
        override var partner: Tester? = null
        override fun test(n: Int): Boolean = if (n == 0) true else partner!!.test(n - 1)
    }

    class OddImpl : Tester {
        override var partner: Tester? = null
        override fun test(n: Int): Boolean = if (n == 0) false else partner!!.test(n - 1)
    }
}
