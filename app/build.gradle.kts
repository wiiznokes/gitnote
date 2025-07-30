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

    id("org.mozilla.rust-android-gradle.rust-android") version "0.9.6"
}

val isDebug = gradle.startParameter.taskNames.any { it.contains("Debug") }

cargo {
    module  = "./src/main/rust"
    libname = "git_wrapper"
    targets = listOf("arm64", "x86_64")
    prebuiltToolchains = true
    profile = if (isDebug) "debug" else "release"
}

tasks.whenTaskAdded {
    if (name == "mergeDebugJniLibFolders" || name == "mergeReleaseJniLibFolders") {
        outputs.upToDateWhen { false }
        //dependsOn("cargoBuild")
    }
}

android {

    ndkVersion = "27.2.12479018"

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
        versionCode = 8
        versionName = "25.07"

        buildConfigField(
            "String",
            "GIT_HASH",
            "\"${getGitHash()}\""
        )

        androidResources {
            localeFilters += (
                listOf(
                    "en",
                    "fr"
                )
            )
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
    implementation(libs.androidx.ktx)
    implementation(libs.runtime.ktx)
    implementation(libs.runtime.compose)
    implementation(libs.compose.activity)
    implementation(libs.datastore.preferences)

    val composeBom = platform(libs.compose.bom)

    // Compose
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Compose Debug
    implementation(libs.compose.ui.preview)
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
    androidTestImplementation(libs.androidx.runner)
}