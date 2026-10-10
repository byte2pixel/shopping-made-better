import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Supabase connection config is read from local.properties (never committed),
// then from the environment, which is how the release workflow supplies the
// cloud project's values. The URL falls back to the standard emulator->host
// address; the key has no default and must be set per-teammate (see README
// "Setting Up Supabase Locally").
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun buildSetting(name: String): String? =
    localProperties.getProperty(name)?.takeIf { it.isNotBlank() }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }

// Release signing comes only from the environment (the release workflow decodes
// the keystore from a secret). Without it, assembleRelease produces an unsigned APK.
val releaseKeystorePath = System.getenv("RELEASE_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }

// The release workflow derives both from the release tag (v<major>.<minor>.<patch>);
// local and PR builds keep the defaults.
val versionNameFromEnv = System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() }
val versionCodeFromEnv = System.getenv("VERSION_CODE")?.toIntOrNull()

android {
    namespace = "com.fullsail.shoppingmadebetter"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.fullsail.shoppingmadebetter"
        minSdk = 30
        targetSdk = 37
        versionCode = versionCodeFromEnv ?: 1
        versionName = versionNameFromEnv ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${buildSetting("SUPABASE_URL") ?: "http://10.0.2.2:54321"}\"",
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${buildSetting("SUPABASE_ANON_KEY") ?: ""}\"",
        )
    }

    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                // PKCS12 keystores (keytool's default) share one password, so the
                // key password is optional.
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
                    ?: System.getenv("RELEASE_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            if (releaseKeystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        // Let JVM unit tests call android.util.* (e.g. Log) without throwing
        // "not mocked" — stubbed calls return default values instead.
        unitTests.isReturnDefaultValues = true
    }
    buildToolsVersion = "37.0.0"
}

kotlin {
    compilerOptions {
        // Opt in project-wide so date/time code doesn't need per-usage @OptIn.
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(platform(libs.supabase.bom))
    implementation(libs.androidx.ui)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.kotlinx.datetime)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.paging.compose)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.paging.common)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
