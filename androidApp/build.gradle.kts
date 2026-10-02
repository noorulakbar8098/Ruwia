import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
    implementation(libs.kotlinx.datetime.v080)
}

android {
    namespace = "com.example.ruwia"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    val localProperties = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            FileInputStream(file).use { load(it) }
        }
    }

    signingConfigs {
        create("release") {
            val storeFileVal = localProperties.getProperty("signing.storeFile")
            val storePasswordVal = localProperties.getProperty("signing.storePassword")
            val keyAliasVal = localProperties.getProperty("signing.keyAlias")
            val keyPasswordVal = localProperties.getProperty("signing.keyPassword")

            if (storeFileVal != null && storePasswordVal != null && keyAliasVal != null && keyPasswordVal != null) {
                storeFile = file(storeFileVal)
                storePassword = storePasswordVal
                keyAlias = keyAliasVal
                keyPassword = keyPasswordVal
            }
        }
    }

    // Single production build: Supabase keys come from local.properties.
    val prodUrl = localProperties.getProperty("supabase.url.prod") ?: "https://srvertclbjcvlfdectsl.supabase.co"
    val prodAnonKey = localProperties.getProperty("supabase.anonkey.prod") ?: "sb_publishable_N3oENikhDxobWX5dcEJoMw_FMbth7CN"
    // Never commit a real service-role secret here: when local.properties is
    // absent the build gets an empty key and the app shows the setup message.
    val prodServiceKey = localProperties.getProperty("supabase.servicekey.prod") ?: ""

    defaultConfig {
        applicationId = "com.example.ruwia"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 4
        versionName = "2.2"
        manifestPlaceholders["appName"] = "Ruwia"

        // Single production build: keys come from local.properties
        // (supabase.url.prod / supabase.anonkey.prod / supabase.servicekey.prod).
        buildConfigField("String", "SUPABASE_URL", "\"$prodUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$prodAnonKey\"")
        buildConfigField("String", "SUPABASE_SERVICE_KEY", "\"$prodServiceKey\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }
}