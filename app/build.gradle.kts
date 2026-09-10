plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.piglauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.piglauncher"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    compileOnly(project(":xposed-api-stubs"))
}
