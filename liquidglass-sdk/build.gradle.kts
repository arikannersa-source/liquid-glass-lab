plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.liquidglass.sdk"
    compileSdk = 37

    defaultConfig {
        minSdk = 33
        consumerProguardFiles("consumer-rules.pro")
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

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    api(libs.compose.runtime)
    api(libs.compose.ui)
    api(libs.compose.ui.graphics)
    api(libs.compose.foundation)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    testImplementation(libs.skiko.linux.x64)
}
