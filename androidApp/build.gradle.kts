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

    defaultConfig {
        applicationId = "com.example.ruwia"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["appName"] = "Ruwia"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }

    val localProperties = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            FileInputStream(file).use { load(it) }
        }
    }

    val devUrl = localProperties.getProperty("supabase.url.dev") ?: "https://srvertclbjcvlfdectsl.supabase.co"
    val devAnonKey = localProperties.getProperty("supabase.anonkey.dev") ?: "sb_publishable_N3oENikhDxobWX5dcEJoMw_FMbth7CN"
    val devServiceKey = localProperties.getProperty("supabase.servicekey.dev") ?: "sb_secret_F8KyUwYsXLvzcdM_5TQqRg_a7IHDjEr"

    val prodUrl = localProperties.getProperty("supabase.url.prod") ?: "https://srvertclbjcvlfdectsl.supabase.co"
    val prodAnonKey = localProperties.getProperty("supabase.anonkey.prod") ?: "sb_publishable_N3oENikhDxobWX5dcEJoMw_FMbth7CN"
    val prodServiceKey = localProperties.getProperty("supabase.servicekey.prod") ?: "sb_secret_F8KyUwYsXLvzcdM_5TQqRg_a7IHDjEr"

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            manifestPlaceholders["appName"] = "Ruwia Dev"
            buildConfigField("String", "SUPABASE_URL", "\"$devUrl\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"$devAnonKey\"")
            buildConfigField("String", "SUPABASE_SERVICE_KEY", "\"$devServiceKey\"")
        }
        create("prod") {
            dimension = "environment"
            manifestPlaceholders["appName"] = "Ruwia"
            buildConfigField("String", "SUPABASE_URL", "\"$prodUrl\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"$prodAnonKey\"")
            buildConfigField("String", "SUPABASE_SERVICE_KEY", "\"$prodServiceKey\"")
        }
    }
}