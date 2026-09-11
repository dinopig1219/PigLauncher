plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val ciVersionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull() ?: 1
val commitSha = System.getenv("APP_COMMIT_SHA")?.take(7)
val releaseTag = System.getenv("APP_RELEASE_TAG")?.takeIf { it.isNotBlank() }

android {
    namespace = "com.dinopig.piglauncher"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dinopig.piglauncher"
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

            if (file("release.keystore").exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }

        debug {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly(project(":xposed-api-stubs"))

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation:1.11.4")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.3")
    implementation("io.github.libxposed:service:102.0.0")
}
