plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.kotlin.serialization)

}

android {
    namespace = "com.emerjbl.ultra8"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.emerjbl.ultra8"
        minSdk = 24
        targetSdk = 36
        versionCode = 10
        versionName = "0.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            optIn.addAll(
                "androidx.compose.material3.ExperimentalMaterial3Api",
                "androidx.compose.ui.ExperimentalComposeUiApi",
                "kotlin.ExperimentalStdlibApi",
                "kotlin.ExperimentalUnsignedTypes",
                "kotlinx.coroutines.FlowPreview",
            )
        }
    }


    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Core & Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose.android)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.graphics.shapes.android)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Unit Tests
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.strikt.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":testutil"))

    // Android Instrumentation Tests
    androidTestImplementation(project(":testutil"))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.strikt.core)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
