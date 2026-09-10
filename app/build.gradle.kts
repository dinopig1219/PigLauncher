plugins {
    id("com.android.application")
}

val ciVersionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull() ?: 1
val commitSha = System.getenv("APP_COMMIT_SHA")?.take(7)
val releaseTag = System.getenv("APP_RELEASE_TAG")?.takeIf { it.isNotBlank() }

android {
    namespace = "io.github.piglauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.piglauncher"
        minSdk = 28
        targetSdk = 35

        versionCode = ciVersionCode
        versionName = releaseTag
            ?: commitSha?.let { "1.0-$it" }
            ?: "1.0"
    }

    signingConfigs {
        create("release") {
            val keystore = file("release.keystore")

            if (keystore.exists()) {
                storeFile = keystore
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            // GitHub Actions decodes app/release.keystore before Gradle runs.
            // Without the file, local release builds remain unsigned.
            if (file("release.keystore").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Compile-only Xposed stubs; LSPosed supplies the real API at runtime.
    compileOnly(project(":xposed-api-stubs"))
}
