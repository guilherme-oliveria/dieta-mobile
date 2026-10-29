import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.firebase.messaging)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "org.br.dietamais"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.br.dietamais"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        val conviteHost = providers.gradleProperty("conviteHost").orElse("nao-configurado.invalid").get()
        require(conviteHost.matches(Regex("[a-zA-Z0-9.-]+"))) { "conviteHost deve conter apenas um domínio, sem esquema ou caminho" }
        manifestPlaceholders["conviteHost"] = conviteHost
        resValue("string", "convite_host", conviteHost)
        // URL da API só para desenvolvimento (ex.: -PurlApi=http://192.168.0.10:8080 em aparelho físico); vazio = padrão do app (10.0.2.2 no emulador).
        resValue("string", "url_api_dev", providers.gradleProperty("urlApi").orElse("").get())
        // Firebase sem google-services.json: as credenciais do projeto vêm de propriedades do Gradle (secrets do CI), nunca do Git.
        // Sem elas o push fica desligado e o app funciona normalmente.
        mapOf("fcm_app_id" to "fcmAppId", "fcm_api_key" to "fcmApiKey", "fcm_project_id" to "fcmProjectId", "fcm_sender_id" to "fcmSenderId").forEach { (recurso, propriedade) ->
            resValue("string", recurso, providers.gradleProperty(propriedade).orElse("").get())
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        resValues = true
    }
}
