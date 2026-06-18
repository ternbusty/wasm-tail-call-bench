#!/usr/bin/env bash
# Toggle the WasmEnableTailCalls feature and re-run benchmarks.
#
# Usage:
#   ./run-comparison.sh on    # tail calls enabled (default)
#   ./run-comparison.sh off   # tail calls disabled at the BodyGenerator level
#
# `off` mode edits compiler/ir/backend.wasm/.../BodyGenerator.kt in the
# sibling `../kotlin` checkout so isEligibleForTailCall returns false, runs
# `./gradlew install`, then runs the benchmark suite. After collecting numbers,
# run with `on` to restore the default and re-install.

set -euo pipefail

mode=${1:-on}
kotlin_root=$(cd "$(dirname "$0")/../kotlin" && pwd)
body_gen="$kotlin_root/compiler/ir/backend.wasm/src/org/jetbrains/kotlin/backend/wasm/ir2wasm/codegenGenerators/BodyGenerator.kt"
sentinel='if (true) return false // BENCH OFF'

case "$mode" in
  on)
    if grep -F "$sentinel" "$body_gen" > /dev/null; then
      sed -i '' "/$sentinel/d" "$body_gen"
      echo "Restored isEligibleForTailCall to default behavior."
    else
      echo "isEligibleForTailCall already in default state."
    fi
    ;;
  off)
    if grep -F "$sentinel" "$body_gen" > /dev/null; then
      echo "Sentinel already in place."
    else
      python3 - "$body_gen" "$sentinel" <<'PY'
import sys, pathlib
path = pathlib.Path(sys.argv[1])
sentinel = sys.argv[2]
src = path.read_text()
needle = "private fun isEligibleForTailCall(call: IrFunctionAccessExpression, callee: IrFunction): Boolean {\n"
idx = src.index(needle) + len(needle)
patched = src[:idx] + "        " + sentinel + "\n" + src[idx:]
path.write_text(patched)
PY
      echo "Inserted sentinel to disable tail call emission."
    fi
    ;;
  *)
    echo "Usage: $0 on|off"
    exit 1
    ;;
esac

echo "Re-installing Kotlin compiler with mode=$mode ..."
(cd "$kotlin_root" && ./gradlew install -q -x test)

echo "Running benchmarks ..."
./gradlew wasmJsBenchmark -q
