import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.capstone"
    compileSdk = 35 // Using preview SDK, ensure your tools are up-to-date

    defaultConfig {
        applicationId = "com.example.capstone"
        // Note: minSdk 34 is very high and will significantly limit the number of devices
        // your app can run on. Consider minSdk 24 or 26 unless you specifically
        // require Android 14+ features for the app's core functionality.
        minSdk = 34
        targetSdk = 34 // Target latest stable or match compileSdk if using preview
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        // dataBinding = true // Only enable if you are actually using Data Binding
        viewBinding = true // Keep this enabled as you are likely using it
    }
} // End of android block

dependencies {

    // CameraX core library
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // TensorFlow Lite Task Library (Vision) - Exclude the conflicting Edge runtime
    implementation(libs.tensorflow.lite.task.vision) {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
    }

    // Optional: GPU Delegate Plugin - Also exclude here just in case
    implementation(libs.tensorflow.lite.gpu.delegate.plugin) {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
    }
    // Base GPU dependency (needed for CompatibilityList) - Also exclude here to be safe
    // Ensure libs.tensorflow.lite.gpu points to a compatible version (like 2.12.0) in libs.versions.toml
    implementation(libs.tensorflow.lite.gpu) {
        exclude(mapOf("group" to "com.google.ai.edge.litert", "module" to "litert-api")) // <-- CORRECTED SYNTAX using mapOf
    }


    // Standard libraries
    implementation(libs.androidx.core.ktx.v1120) // Check if libs.androidx.core.ktx is sufficient
    implementation(libs.androidx.appcompat.v161) // Check if libs.androidx.appcompat is sufficient
    implementation(libs.material.v1110)       // Check if libs.material is sufficient
    implementation(libs.androidx.constraintlayout.v214) // Check if libs.androidx.constraintlayout is sufficient
    implementation(libs.androidx.activity)
    // REMOVED: implementation(libs.litert.gpu) <-- This was incorrect

    // Test libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}