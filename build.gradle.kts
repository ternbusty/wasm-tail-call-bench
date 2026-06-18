plugins {
    kotlin("multiplatform") version "2.4.255-SNAPSHOT"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.17"
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
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
        }
    }
}
