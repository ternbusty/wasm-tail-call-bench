# Wasm Tail Call Benchmarks

Standalone `kotlinx-benchmark` project for measuring the impact of native Wasm
tail call emission in the Kotlin/Wasm backend.

## Requirements

The benchmarks compile with the in-development Kotlin compiler from the local
checkout at `../kotlin`. Before running, install that compiler to the local
Maven repository:

```bash
cd ../kotlin
git switch feature/wasm-tail-calls/02-static-emit
./gradlew install -x test
```

That publishes `org.jetbrains.kotlin:*:2.4.255-SNAPSHOT` under `~/.m2`.

## Running

```bash
./gradlew wasmJsBenchmark
```

Reports land under `build/reports/benchmarks/main/<timestamp>/`.

## Benchmarks

| File | What it measures |
| --- | --- |
| `StaticMutualRecursion.kt` | Top level `even` and `odd` bouncing. Static dispatch tail call (`return_call`). |
| `VirtualMutualRecursion.kt` | Open class siblings with virtual `test` method. Vtable dispatch tail call (`return_call_ref`). |
| `InterfaceMutualRecursion.kt` | Two implementations of an interface bouncing through itable. (`return_call_ref`) |
| `TailrecVsNative.kt` | Same arithmetic body with `tailrec` (loop lowering) vs unmarked recursive (native tail call). |

Each benchmark sweeps depth at 100, 1000, 10000.

## Comparing with feature off

The `02-static-emit` branch has `WASM_ENABLE_TAIL_CALLS` defaulting on. To measure
the baseline, edit `compiler/ir/backend.wasm/.../BodyGenerator.kt` to force
`isEligibleForTailCall` to return `false`, re-run `./gradlew install`, then re-run
this benchmark project.
