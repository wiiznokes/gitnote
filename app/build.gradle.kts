import java.io.BufferedReader
import java.io.InputStreamReader

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.about.libraries)
    // for room
    alias(libs.plugins.ksp)
    // for compose navigation
    id("kotlin-parcelize")
}

android {
    namespace = "io.github.wiiznokes.gitnote"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.wiiznokes.gitnote"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = File("VERSION").readText()

        fun getGitHash(): String {
            val command = "git rev-parse HEAD"
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            return reader.readLine()
        }

        buildConfigField(
            "String",
            "GIT_HASH",
            "\"${getGitHash()}\""
        )

        resourceConfigurations.addAll(
            listOf(
                "en",
                "fr"
            )
        )

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

    composeOptions.kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }


    ndkVersion = "26.1.10909125"
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

    // Compose
    implementation(platform(libs.compose.bom))
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

    // Fuzzy search
    implementation(libs.fuzzywuzzy)

    // unit test
    testImplementation(libs.test.junit.ktx)

    // integration test
    androidTestImplementation(libs.test.junit.ktx)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.runner)
}