# Tail Call Benchmark Comparison

Compiler: locally built `2.4.255-SNAPSHOT` from `feature/wasm-tail-calls/02-static-emit`. Engine: Node.js shipped with Kotlin/Wasm gradle plugin (V8). `kotlinx-benchmark` 0.4.17 with 3 warmups and 5 iterations per data point. Throughput in operations per second, higher is better.

`OFF` was produced by patching `BodyGenerator.isEligibleForTailCall` to return `false` and reinstalling the compiler. `ON` is the default behavior of this branch.

## Static dispatch mutual recursion

| depth | OFF (ops/sec) | ON (ops/sec) | ON / OFF |
| ----: | ------------: | -----------: | -------: |
|   100 |    18,984,962 |   20,726,452 |   1.09 x |
|  1000 |     1,017,548 |    2,459,353 |   2.42 x |
| 10000 |        93,350 |      251,273 |   2.69 x |

Static mutual recursion is the headline case for PR 2. Throughput grows roughly linearly with how deep we go, peaking around 2.7 x at depth 10000. The shallow-depth gap (1.09 x) is small because V8 already handles short call chains well.

## Recursive function that is not marked `tailrec`

`TailrecVsNative.nativeSum`. A self-recursive function that the existing compiler emits as `call`. PR 2 detects the tail position and switches to `return_call`.

| depth | OFF (ops/sec) | ON (ops/sec) | ON / OFF |
| ----: | ------------: | -----------: | -------: |
|   100 |    12,652,513 |   21,338,094 |   1.69 x |
|  1000 |       530,233 |    2,254,782 |   4.25 x |
| 10000 |        52,658 |      223,615 |   4.25 x |

This is the largest single-pattern win. A function that would have been written `tailrec` (but for some reason was not) now runs roughly 4 x faster at moderate depths.

## `tailrec` lowered to a loop

`TailrecVsNative.tailrecSum`. `tailrec` runs through `TailrecLowering` and becomes a `do while` loop before BodyGenerator sees it. The patch in this PR does not touch this path.

| depth | OFF (ops/sec) | ON (ops/sec) | ON / OFF |
| ----: | ------------: | -----------: | -------: |
|   100 |    30,602,047 |   29,753,696 |   0.97 x |
|  1000 |     2,673,312 |    2,576,966 |   0.96 x |
| 10000 |       269,675 |      268,931 |   1.00 x |

Within noise. Confirms the design choice that `tailrec` stays as a loop.

## Virtual dispatch tail call

`VirtualMutualRecursion`. Two siblings of an open class bouncing through a virtual method. PR 2 does not yet wire the vtable emission path (that is PR 3), so this case should be unchanged.

| depth | OFF (ops/sec) | ON (ops/sec) | ON / OFF |
| ----: | ------------: | -----------: | -------: |
|   100 |    11,761,834 |   11,647,648 |   0.99 x |
|  1000 |       665,357 |      659,705 |   0.99 x |
| 10000 |        60,946 |       60,433 |   0.99 x |

Confirmed unchanged. PR 3 will introduce `return_call_ref` here.

## Interface dispatch tail call

`InterfaceMutualRecursion`. Same as above but through an interface. Also untouched by PR 2.

| depth | OFF (ops/sec) | ON (ops/sec) | ON / OFF |
| ----: | ------------: | -----------: | -------: |
|   100 |     1,876,429 |    1,838,794 |   0.98 x |
|  1000 |       190,315 |      182,166 |   0.96 x |
| 10000 |        15,986 |       17,141 |   1.07 x |

Within noise. PR 3 territory.

## Headline numbers

Where PR 2 actually changes the emitted code (`return_call` for static dispatch and for non `tailrec` self recursion), throughput goes up substantially. The biggest single win is the non `tailrec` self recursive case at depth 1000 and 10000, both at roughly 4.25 x. Mutual recursion at depth 10000 is 2.69 x.

`tailrec` stays at parity. Virtual and interface dispatch are unchanged because PR 2 does not touch those paths. PR 3 will introduce `return_call_ref` for vtable and itable dispatch.

## Notes

The benchmark project lives in a sibling directory outside the Kotlin source tree. It depends on a locally installed Kotlin compiler (`./gradlew install -x test` in the Kotlin checkout). `run-comparison.sh` toggles the feature by editing `BodyGenerator.isEligibleForTailCall` and reinstalling.

A `BinaryenConfig` patch is required to unblock the post compile `wasm-opt` step. Without `--enable-tail-call` in `binaryenCommonArgs`, any module containing `return_call` is rejected by binaryen with `unexpected false: return_call* requires tail calls`. This patch is included on top of the branch I tested.
