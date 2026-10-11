plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.marketkernel.uacryptography.example"
    // The newest Compose needs it; the library itself compiles against 36.
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.marketkernel.uacryptography.example"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // R8 as an app shipping the library would run it: the library's consumer rules are what keeps it working.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            // An example, never published: the debug key lets `installRelease` run on any device.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        warningsAsErrors = true
        // Versions are bumped deliberately, not on lint's notice.
        disable += listOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}

dependencies {
    // From Maven Central, as an app depends on it; -PuaCryptography.exampleFromSource=true builds it from this repository.
    implementation(libs.ua.cryptography.android)
    implementation(libs.kotlinx.coroutines.core)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    debugImplementation(libs.compose.ui.test.manifest)
}

// The library of this repository in place of the published one: CI builds the example both ways, so a change of the
// API that breaks the example shows before it is released.
if (providers.gradleProperty("uaCryptography.exampleFromSource").orNull.toBoolean()) {
    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("io.github.marketkernel:ua-cryptography-android")).using(project(":ua-cryptography"))
        }
    }
}
