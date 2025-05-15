import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.about.libraries)
    // for room
    alias(libs.plugins.ksp)
    // for compose navigation
    id("kotlin-parcelize")
}

android {
    namespace = "io.github.wiiznokes.gitnote"
    compileSdk = 35

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
        targetSdk = 35
        versionCode = 2
        versionName = "25.05.2"

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

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }
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

        debug {
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    ndkVersion = "27.2.12479018"
}

kotlin {
    jvmToolchain(21)
}

dependencies {

    // AndroidX Core
    implementation(libs.androidx.ktx)
    implementation(libs.runtime.ktx)
    implementation(libs.runtime.compose)
    implementation(libs.compose.activity)
    implementation(libs.datastore.preferences)
    //implementation(libs.work.runtime.ktx)
    //implementation(libs.splash.screen)


    val composeBom = platform(libs.compose.bom)

    // Compose
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.material)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    //implementation(libs.compose.constraintlayout)

    // Compose Debug
    implementation(libs.compose.ui.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // Accompanist
    //implementation(libs.accompanist.permissions)


    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    annotationProcessor(libs.room.compiler)
    ksp(libs.room.compiler)

    // Compose Navigation
    implementation(libs.reimagined.navigation)

    // Licenses
    implementation(libs.about.libraries)

    // Markdown to HTML
    //implementation(libs.markdown)

    // unit test
    testImplementation(libs.test.junit.ktx)

    // integration test
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.test.junit.ktx)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.runner)
}