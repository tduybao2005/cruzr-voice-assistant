import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.baxailab.cadebot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.baxailab.cadebot"
        minSdk = 22
        targetSdk = 22
        versionCode = 19
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += "armeabi-v7a"
        }

        val localProps = Properties()
        rootProject.file("local.properties").takeIf { it.exists() }?.reader()?.use { localProps.load(it) }
        // Keep the production gateway as the safe fallback when a developer
        // builds without creating local.properties first.
        buildConfigField("String", "CADEBOT_API_URL", "\"${localProps.getProperty("cadebot.api.url", "https://cadebot.example.com")}\"")
        buildConfigField("String", "PAYMENT_API_URL", "\"${localProps.getProperty("payment.api.url", "https://cadebot.example.com")}\"")
    }

    // Prefer the handover keystore bundled by the project so another developer
    // can reproduce the same signature and install -r over the Cruzr build.
    // Fall back to the standard local debug keystore for development checkouts.
    val bundledKeystore = rootProject.file("signing/cadebot-cruzr-debug.keystore")
    val localKeystore = file(System.getProperty("user.home") + "/.android/debug.keystore")
    val cruzrKeystore = if (bundledKeystore.isFile) bundledKeystore else localKeystore
    signingConfigs {
        getByName("debug") {
            storeFile = cruzrKeystore
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("release") {
            storeFile = cruzrKeystore
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // targetSdk is deliberately pinned to 22 for Cruzr (Android 5.1.1) compatibility,
    // which trips AGP's Play Store "ExpiredTargetSdkVersion" lintVital gate. This
    // build is not distributed via Play Store, so that check does not apply here.
    lint {
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Cruzr SDK (cruzr-sdk-2.8.0.jar) is optional and not redistributable, so it is
    // not committed here. Drop it into app/libs/ and the Assistant wake-up button
    // gets hidden automatically — see app/libs/README.md.
    implementation(fileTree("libs") { include("*.jar", "*.aar") })

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    testImplementation("org.json:json:20231013")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
