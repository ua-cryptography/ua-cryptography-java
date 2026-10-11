import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

/** The ABIs of the release of ua-cryptography-rust, all of them packaged. */
val nativeAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

val nativeRelease = Properties().apply {
    load(providers.fileContents(layout.projectDirectory.file("native-release.properties")).asText.get().reader())
}
val pinnedNativeVersion: String = nativeRelease.getProperty("version")
val nativeSha256ByTarget: Map<String, String> = nativeRelease.stringPropertyNames()
    .filter { it.startsWith("sha256.") }
    .associate { it.removePrefix("sha256.") to nativeRelease.getProperty(it) }
val nativeLibrariesDirectory = layout.buildDirectory.dir("native-libraries")

val supplyNativeLibraries = tasks.register<SupplyNativeLibrariesTask>("supplyNativeLibraries") {
    description = "Downloads the native library of every ABI from the pinned release, or copies it from a local directory."
    nativeLibraryVersion = pinnedNativeVersion
    sha256ByTarget = nativeSha256ByTarget
    // -PuaCryptography.localLibraryDir=../ua-cryptography-rust/target/android-jni-libs, relative to the repository.
    localLibraryDirectory = providers.gradleProperty("uaCryptography.localLibraryDir").map { layout.settingsDirectory.dir(it) }
    outputDirectory = nativeLibrariesDirectory
}

android {
    namespace = "io.github.marketkernel.uacryptography"
    compileSdk = 36
    // r28 and later align shared libraries to the 16 KB pages Google Play requires.
    ndkVersion = "28.2.13676358"

    defaultConfig {
        // java.time in the reports without core library desugaring in every app.
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "NATIVE_LIBRARY_VERSION", "\"$pinnedNativeVersion\"")
        ndk {
            abiFilters += nativeAbis
        }
        externalNativeBuild {
            cmake {
                arguments += "-DUA_CRYPTOGRAPHY_LIBRARY_DIR=${nativeLibrariesDirectory.get().asFile}"
            }
        }
    }

    buildFeatures {
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        warningsAsErrors = true
        // The stdlib and coroutines are the floor apps compile against (gradle/libs.versions.toml), and compileSdk
        // stays below the newest so that no app is made to raise its own.
        disable += listOf("GradleDependency", "NewerVersionAvailable")
        // Reads abiFilters as lacking x86_64, which nativeAbis has.
        disable += "ChromeOsAbiSupport"
    }
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        // Readable by the compilers of apps on Kotlin 2.0 and later (gradle/libs.versions.toml).
        languageVersion = KotlinVersion.KOTLIN_2_1
        apiVersion = KotlinVersion.KOTLIN_2_1
    }
}

// The release libraries go into the AAR beside the JNI library; AGP packages a CMake import in apps only.
androidComponents {
    onVariants { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(supplyNativeLibraries, SupplyNativeLibrariesTask::outputDirectory)
    }
}

// The JNI library links the release library of its ABI, so CMake runs after the libraries are supplied.
tasks.configureEach {
    if (name.startsWith("configureCMake") || name.startsWith("buildCMake")) {
        dependsOn(supplyNativeLibraries)
    }
}

dependencies {
    // The public API is made of Kotlin types.
    api(libs.kotlin.stdlib)
    // sign, verify and createKeyStore are suspending: they leave the caller's thread for the IO dispatcher.
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    // The JVM tests read reports with org.json, which android.jar only stubs.
    testImplementation(libs.json)

    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
}

mavenPublishing {
    // Maven Central requires a javadoc jar; the API documentation is the KDoc in the sources jar.
    configure(AndroidSingleVariantLibrary(JavadocJar.Empty(), SourcesJar.Sources(), variant = "release"))
    coordinates(artifactId = "ua-cryptography-android")
    // The deployment waits in the Central Portal for the author's "Publish".
    publishToMavenCentral(automaticRelease = false)
    // The release workflow passes the key (ORG_GRADLE_PROJECT_signingInMemoryKey); publishToMavenLocal needs none, and
    // Maven Central refuses an unsigned deployment.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
}
