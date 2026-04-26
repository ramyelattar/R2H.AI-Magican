import java.io.File
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

val modelAssetPathProvider = providers.gradleProperty("ai.runtime.model.asset").orElse("")
val modelSha256Provider = providers.gradleProperty("ai.runtime.model.sha256").orElse("")
val modelVersionProvider = providers.gradleProperty("ai.runtime.model.version").orElse("unknown")

android {
    namespace = "com.r2h.magican.ai.runtime"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        buildConfigField("boolean", "AI_RUNTIME_REQUIRED", "false")
        buildConfigField("String", "DEFAULT_MODEL_ASSET_PATH", "\"${modelAssetPathProvider.get()}\"")
        buildConfigField("String", "DEFAULT_MODEL_SHA256", "\"${modelSha256Provider.get()}\"")
        buildConfigField("String", "DEFAULT_MODEL_VERSION", "\"${modelVersionProvider.get()}\"")

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-fexceptions", "-frtti")
                arguments += listOf("-DANDROID_STL=c++_shared", "-DENABLE_STUB_RUNTIME=1")
            }
        }

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("boolean", "AI_RUNTIME_REQUIRED", "false")
            externalNativeBuild {
                cmake {
                    arguments += listOf("-DENABLE_STUB_RUNTIME=1")
                }
            }
        }

        getByName("release") {
            buildConfigField("boolean", "AI_RUNTIME_REQUIRED", "true")
            externalNativeBuild {
                cmake {
                    arguments += listOf("-DENABLE_STUB_RUNTIME=0")
                }
            }
        }
    }

    packaging {
        jniLibs {
            pickFirsts += setOf("**/libc++_shared.so")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.register("verifyReleaseAiRuntimeConfig") {
    group = "verification"
    description = "Fails release build when mandatory AI runtime config is missing."
    doLast {
        val modelAssetPath = modelAssetPathProvider.get().trim()
        val modelSha256 = modelSha256Provider.get().trim().lowercase()

        if (modelAssetPath.isBlank()) {
            throw org.gradle.api.GradleException("Release requires ai.runtime.model.asset gradle property")
        }
        if (modelSha256.isBlank()) {
            throw org.gradle.api.GradleException("Release requires ai.runtime.model.sha256 gradle property")
        }
        if (!modelSha256.matches(Regex("[0-9a-f]{64}"))) {
            throw org.gradle.api.GradleException("ai.runtime.model.sha256 must be 64 lowercase hex chars")
        }

        val assetFile = file("src/main/assets/$modelAssetPath")
        if (!assetFile.exists()) {
            throw org.gradle.api.GradleException("Configured model asset not found: $assetFile")
        }

        val llamaCppCmake = file("src/main/cpp/third_party/llama.cpp/CMakeLists.txt")
        if (!llamaCppCmake.exists()) {
            throw org.gradle.api.GradleException(
                "Release requires vendored llama.cpp at ${llamaCppCmake.path} (stub runtime is forbidden)."
            )
        }

        val actualSha256 = sha256(assetFile)
        if (actualSha256 != modelSha256) {
            throw org.gradle.api.GradleException(
                "Configured model SHA-256 does not match asset. expected=$modelSha256 actual=$actualSha256"
            )
        }

        if (assetFile.length() < 1_048_576L) {
            throw org.gradle.api.GradleException(
                "Model asset is unexpectedly small (${assetFile.length()} bytes). Placeholder assets are not allowed."
            )
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn("verifyReleaseAiRuntimeConfig")
}

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.hilt.android)
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.android)
    kapt(libs.hilt.compiler)
    testImplementation(libs.junit)
}
