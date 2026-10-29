import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "dietamais"
        browser {
            commonWebpackConfig {
                outputFileName = "dietamais.js"
                // Desenvolvimento: o site em :8081 encaminha /api para o backend em :8080, ficando na mesma origem (cookie do refresh).
                // No backend: WEB_ORIGEM=http://localhost:8081 e CORS_ORIGENS=http://localhost:8081.
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    port = 8081
                    proxy = mutableListOf(KotlinWebpackConfig.DevServer.Proxy(
                        mutableListOf("/api", "/legal"), "http://localhost:8080"))
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
