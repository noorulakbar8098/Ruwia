import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    // On the classpath only (not applied): the marker must resolve for the
    // conditional apply() below to find the plugin id.
    alias(libs.plugins.googleServices) apply false
}

// Outside-the-app push (FCM) activates only once google-services.json is in
// place (Firebase console → Android app → download here). Until then the app
// builds and runs normally — just without system push. In-app notifications
// work regardless.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.play.services.location)

    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.kotlinx.coroutines.android)

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

    // Signing secrets come from the environment in CI and from the gitignored
    // local.properties on a developer machine. Never commit a keystore or its
    // passwords. The keystore itself is encrypted by JKS; its passwords must
    // stay in a secret manager / environment, not in the repository.
    fun signingValue(envName: String, propertyName: String): String? =
        System.getenv(envName)?.takeIf { it.isNotBlank() }
            ?: localProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }

    signingConfigs {
        create("release") {
            val storeFileVal = signingValue("RUWIA_STORE_FILE", "signing.storeFile")
            val storePasswordVal = signingValue("RUWIA_STORE_PASSWORD", "signing.storePassword")
            val keyAliasVal = signingValue("RUWIA_KEY_ALIAS", "signing.keyAlias")
            val keyPasswordVal = signingValue("RUWIA_KEY_PASSWORD", "signing.keyPassword")

            if (storeFileVal != null && storePasswordVal != null && keyAliasVal != null && keyPasswordVal != null) {
                storeFile = file(storeFileVal)
                storePassword = storePasswordVal
                keyAlias = keyAliasVal
                keyPassword = keyPasswordVal
            }
        }
    }

    defaultConfig {
        applicationId = "com.example.ruwia"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 7
        versionName = "2.5"
        manifestPlaceholders["appName"] = "Ruwia"

        // No Supabase keys are embedded here. The publishable key and URL live
        // in the shared client (protected by RLS), and the service-role key is
        // server-only (Supabase Edge Function secrets).
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

    lint {
        checkReleaseBuilds = false
        abortOnError = false
        disable += "InvalidFragmentVersionForActivityResult"
    }
}