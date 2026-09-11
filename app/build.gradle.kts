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
    compileSdk = 37

    defaultConfig {
        applicationId = "com.dinopig.piglauncher"
        minSdk = 33
        targetSdk = 37

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
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

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

    packaging {
        resources {
            merges += "META-INF/xposed/*"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    compileOnly("io.github.libxposed:api:102.0.0")

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation:1.11.4")
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.3")
    implementation("io.github.libxposed:service:102.0.0")
}
