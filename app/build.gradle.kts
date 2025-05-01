import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@Suppress("DSL_SCOPE_VIOLATION") // Remove when fixed https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.gradle)
    alias(libs.plugins.ksp)

    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.bottlecast.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bottlecast.app"
        minSdk = 33
        targetSdk = 34
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "com.bottlecast.app.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {

        // TODO: fix up signing config block
//        val keyStoreAlias = project.properties["keyStoreAlias"]  as String
//        val keyStoreAliasPassword = project.properties["keyStoreAliasPassword"]  as String
//        val keyStorePassword = project.properties["keyStorePassword"] as String
//
//        create("release") {
//            println("keyStoreAlias: $keyStoreAlias keyStoreAliasPassword: $keyStoreAliasPassword keyStorePassword: $keyStorePassword")
//            storeFile = file("bottlecast-upload-keystore.jks")
//            storePassword = keyStorePassword
//            keyAlias = keyStoreAlias
//            keyPassword = keyStoreAliasPassword
//        }

        create("release") {
//            val keyStoreAlias = gradleLocalProperties(rootDir).getProperty("keyStoreAlias")
//            val keyStoreAliasPassword = gradleLocalProperties(rootDir).getProperty("keyStoreAliasPassword")
//            val keyStorePassword = gradleLocalProperties(rootDir).getProperty("keyStorePassword")

            val debugSigningConfig: ApkSigningConfig = getByName("debug")
            storeFile = debugSigningConfig.storeFile
            storePassword = debugSigningConfig.storePassword
            keyAlias = debugSigningConfig.keyAlias
            keyPassword = debugSigningConfig.keyPassword
        }
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isDebuggable = false

            // Enables code shrinking, obfuscation, and optimization for only this build type.
            isMinifyEnabled = true

            // Enables resource shrinking, which is performed by the Android Gradle plugin.
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")
        }
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
        aidl = false
        buildConfig = false
        renderScript = false
        shaders = false
    }
}

dependencies {

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    // Hilt and instrumented tests.
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.android.compiler)
    // Hilt and Robolectric tests.
    testImplementation(libs.hilt.android.testing)
    kaptTest(libs.hilt.android.compiler)

    // Arch Components
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.navigation.fragment)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.compose.materialWindow)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.window)

    // moshi
    implementation(libs.squareup.moshi.kotlin)
    implementation(libs.squareup.moshi.adapters)
    ksp(libs.squareup.moshi.kotlin.codegen)

    // retrofit
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.converter.moshi)

    // logging
    implementation(libs.timber)

    // lottie
    implementation(libs.airbnb.lottie)
    implementation(libs.airbnb.lottie.compose)

    // firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.crashlytics.ktx)

    // play services
    implementation(libs.play.services.auth)

    // ui bits (worth revisiting & replacing with jetpack compose for the most part)
    implementation(libs.androidx.ui.text.google.fonts)

    // Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Instrumented tests
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Local tests: jUnit, coroutines, Android runner
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.test)

    // Instrumented tests: jUnit rules and runners
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
