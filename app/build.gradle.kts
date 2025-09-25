import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    // for room
    alias(libs.plugins.ksp)
    // for compose navigation
    id("kotlin-parcelize")
}

android {
    // changing this version require to also change it in CI.
    // link: https://developer.android.com/ndk/downloads
    // Note that we should always take an lts version (end in d, ex: "r27d"), because the dl link
    // could be removed otherwise
    ndkVersion = "27.3.13750724"

    namespace = "io.github.wiiznokes.gitnote"
    compileSdk = 36
    
    dependenciesInfo {
        // Disables dependency metadata when building APKs (for IzzyOnDroid/F-Droid)
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles (for Google Play)
        includeInBundle = false
    }

    defaultConfig {

        fun getGitHash(): String {
            val command = arrayOf("git", "rev-parse", "HEAD")
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            return reader.readLine()
        }

        fun getVersion(): String {
            val currentDate = LocalDate.now()
            val formatter = DateTimeFormatter.ofPattern("yy.MM")
            return currentDate.format(formatter)
        }

        applicationId = "io.github.wiiznokes.gitnote"
        minSdk = 30
        targetSdk = 36
        versionCode = 10
        versionName = "25.09"

        buildConfigField(
            "String",
            "GIT_HASH",
            "\"${getGitHash()}\""
        )

        androidResources {
            generateLocaleConfig = true
        }

        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    signingConfigs {
        create("release") {
            // on powershell
            // $env:KEY_ALIAS = "var"
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
            storeFile = file("key.jks")
            storePassword = System.getenv("STORE_PASSWORD")
        }

        // need this because debug key is machine dependent
        create("nightly") {
            keyAlias = "key0"
            keyPassword = "123456"
            storeFile = file("nightly-signing-key.jks")
            storePassword = "123456"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")
        }


        create("nightly") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("nightly")
            applicationIdSuffix = ".nightly"
        }

        debug {
            applicationIdSuffix = ".debug"
        }
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    kotlin {
        compilerOptions {
            // set the target JVM bytecode
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

}

kotlin {
    // set what version of the jdk will be use to compile the code
    jvmToolchain(21)
}

dependencies {

    // AndroidX Core
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)
    implementation(libs.datastore.preferences)

    val composeBom = platform(libs.compose.bom)

    // Compose
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Compose Debug
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    annotationProcessor(libs.room.compiler)
    ksp(libs.room.compiler)

    // Compose Navigation
    implementation(libs.reimagined.navigation)

    // Markdown to HTML
    implementation(libs.richtext.commonmark)
    implementation(libs.richtext.ui.material3)

    // unit test
    testImplementation(kotlin("test"))

    // integration test
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.test.junit.ktx)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
}