import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.WriteProperties

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val versaoDistribuicao = "1.0.0"
val diretorioVersao = layout.buildDirectory.dir("generated/versaoApp")
val gerarVersaoApp by tasks.registering(WriteProperties::class) {
    destinationFile = diretorioVersao.get().file("dietamais-versao.properties").asFile
    property("versao", versaoDistribuicao)
}
sourceSets.named("main") { resources.srcDir(diretorioVersao) }
tasks.processResources { dependsOn(gerarVersaoApp) }

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "org.br.dietamais.MainKt"
        // URL da API embutida no instalador: ./gradlew :desktopApp:packageMsi -PurlApi=https://api.exemplo.com.br (vazio = localhost)
        providers.gradleProperty("urlApi").orNull?.takeIf { it.isNotBlank() }?.let { jvmArgs("-Ddieta.url.api=$it") }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "DietaMais"
            packageVersion = versaoDistribuicao
        }
    }
}
