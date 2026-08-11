plugins {
    kotlin("multiplatform") version "2.5.255-SNAPSHOT"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.17"
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    sourceSets {
        val wasmJsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.17")
            }
        }
    }
}

val wasmFlags = buildList {
    if (project.findProperty("enableTailCalls")?.toString() != "false") {
        add("-Xwasm-enable-tail-calls")
    }
    if (project.findProperty("enableStacklessRecursion")?.toString() != "false") {
        add("-Xwasm-enable-stackless-recursion")
    }
}

kotlin.targets.withType<org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget>().configureEach {
    compilations.configureEach {
        compileTaskProvider.configure {
            compilerOptions.freeCompilerArgs.addAll(wasmFlags)
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile>().configureEach {
    compilerOptions.freeCompilerArgs.addAll(wasmFlags)
}

benchmark {
    targets {
        register("wasmJs")
    }
    configurations {
        named("main") {
            warmups = 3
            iterations = 5
            iterationTime = 1
            iterationTimeUnit = "s"
            reportFormat = "text"
            exclude("staticMutual")
        }
        create("drfoverhead") {
            warmups = 5
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "s"
            reportFormat = "text"
            include("DrfOverhead")
        }
        create("lexer") {
            warmups = 5
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "s"
            reportFormat = "text"
            include("LexerDispatch")
        }
        create("blog") {
            warmups = 3
            iterations = 5
            iterationTime = 1
            iterationTimeUnit = "s"
            reportFormat = "text"
            include("StaticMutualRecursion|TailrecVsNative|VirtualMutualRecursion|InterfaceMutualRecursion")
        }
        create("accum") {
            warmups = 5
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "s"
            reportFormat = "text"
            include("AccumulatorIntro")
        }
        create("tmcpass") {
            warmups = 5
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "s"
            reportFormat = "text"
            include("TmcCtor|TmcAccum")
        }
        create("iterpass") {
            warmups = 5
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "s"
            reportFormat = "text"
            include("IterativeRec")
        }
        create("apollo") {
            warmups = 10
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "s"
            reportFormat = "text"
            include("ApolloParseType")
        }
        create("selfrec") {
            warmups = 5
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "s"
            reportFormat = "text"
            include("SelfRecForEach")
        }
        create("regex") {
            warmups = 5
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "s"
            reportFormat = "text"
            include("RegexTailCall")
        }
        create("virtualcps") {
            warmups = 5
            iterations = 10
            iterationTime = 1
            iterationTimeUnit = "s"
            reportFormat = "text"
            include("VirtualCpsOverhead|VirtualMutualRecursion")
        }
        create("depthprobe") {
            warmups = 1
            iterations = 3
            iterationTime = 1
            iterationTimeUnit = "s"
            reportFormat = "text"
            include("RegexDepthProbe")
        }
    }
}

// Kotlin 2.5 wraps wasmExports in a Proxy that throws on property access
// (except .memory). kotlinx-benchmark 0.4.17 reads wasmExports.jsPromisingStart,
// which hits this throw. Patch the generated .mjs to forward unknown props to
// the real wasm instance exports instead.
tasks.matching { it.name.contains("ProductionExecutable") && it.name.contains("WasmJs") }
    .configureEach {
        doLast {
            fileTree("build/wasm/packages") {
                include("**/*.mjs")
            }.files.forEach { f ->
                val text = f.readText()
                if (text.contains("throw new Error('Accessing exports via `wasmExports` is no longer supported")) {
                    val patched = text.replace(
                        "throw new Error('Accessing exports via `wasmExports` is no longer supported. Remove usages or update dependencies. Read more: https://kotl.in/vr3szr');",
                        "return exports[prop];"
                    )
                    f.writeText(patched)
                }
            }
        }
    }

// Optional extra V8 flags forwarded to node. Pass via `-PnodeV8Args="--flag1 --flag2"`.
// Used for experiments such as `-PnodeV8Args=--no-wasm-inlining` and
// `-PnodeV8Args=--trace-wasm-inlining`.
val nodeV8Args: String = (project.findProperty("nodeV8Args") as String?).orEmpty()
if (nodeV8Args.isNotBlank()) {
    tasks.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec>().configureEach {
        nodeArgs.addAll(nodeV8Args.split(Regex("\\s+")).filter { it.isNotBlank() })
    }
}

val overrideNodeExe: String? = (project.findProperty("nodeExe") as String?)
if (overrideNodeExe != null) {
    tasks.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsExec>().configureEach {
        executable = overrideNodeExe
    }
}

