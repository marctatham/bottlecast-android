import java.util.Properties

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

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)

    alias(libs.plugins.hilt.gradle)
    alias(libs.plugins.ksp)

    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)

    alias(libs.plugins.roborazzi)
}

// the upload keystore & its credentials are kept out of version control (see .gitignore & local.properties)
val uploadKeystore: File = file("bottlecast-upload-keystore.jks")
val localProperties = Properties().apply {
    providers.fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText.orNull?.let { load(it.reader()) }
}

android {
    namespace = "com.bottlecast.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.bottlecast.app"
        minSdk = 33
        targetSdk = 37
        versionCode = 4
        versionName = "1.1.1"

        testInstrumentationRunner = "com.bottlecast.app.HiltTestRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        // only machines holding the upload keystore can sign releases, they're left unsigned otherwise
        if (uploadKeystore.length() > 0 && localProperties.containsKey("keyStorePassword")) {
            create("release") {
                storeFile = uploadKeystore
                storePassword = localProperties.getProperty("keyStorePassword")
                keyAlias = localProperties.getProperty("keyStoreAlias")
                keyPassword = localProperties.getProperty("keyStoreAliasPassword")
            }
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

            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Robolectric (screenshot tests) needs the merged resources
        unitTests.isIncludeAndroidResources = true
    }
}

// screenshot test goldens are committed alongside the tests, see CLAUDE.md
roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

// ByteBuddy's agent, preloaded into unit-test JVMs for mockk (see the Test tasks below)
val mockkAgent: Configuration by configurations.creating { isTransitive = false }

tasks.withType<Test>().configureEach {
    maxHeapSize = "2g"
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)
    // Robolectric reaches into JDK internals, which Java 17+ only allows when opened (robolectric.org/getting-started)
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.net=ALL-UNNAMED",
        "--add-opens=java.base/java.security=ALL-UNNAMED",
        "--add-opens=java.base/java.text=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    )
    // mockk attaches ByteBuddy's agent to the running JVM, which fails once a Robolectric NATIVE
    // test has run in the same worker; loading the agent at startup means it never has to attach
    val agent: FileCollection = mockkAgent
    inputs.files(agent).withPropertyName("mockkAgent").withNormalizer(ClasspathNormalizer::class.java)
    jvmArgumentProviders.add(CommandLineArgumentProvider { listOf("-javaagent:${agent.singleFile.absolutePath}") })
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
    ksp(libs.hilt.compiler)
    // Hilt and instrumented tests.
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)
    // Hilt and Robolectric tests.
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.android.compiler)

    // Arch Components
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core) // type-safe navigation routes
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3) // pinned ahead of the BOM in the version catalog

    // logging
    implementation(libs.timber)

    // the onboarding flag and the Compose draft
    implementation(libs.androidx.datastore.preferences)

    // Custom Tabs, for the Terms, Community guidelines and Privacy Policy
    implementation(libs.androidx.browser)

    // on-device language identification, stored on each cast (spec §3.4b)
    implementation(libs.mlkit.language.id)

    // firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics)

    // sign in with google, via credential manager
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Instrumented tests
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Local tests: jUnit, coroutines, Android runner
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    mockkAgent(libs.byte.buddy.agent)
    testImplementation(libs.kotlinx.test)

    // Local tests: screenshot tests (Roborazzi on Robolectric)
    testImplementation(composeBom)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.espresso.core)

    // Instrumented tests: jUnit rules and runners, and Espresso for pressing back
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
