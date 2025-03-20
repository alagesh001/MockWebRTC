plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.p2pcall.mockwebrtc"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.p2pcall.mockwebrtc"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.webrtc") {
                useVersion("1.0.4430") // Ensure it uses the correct version
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation ("com.infobip:google-webrtc:1.0.0035529")
    implementation ("com.google.firebase:firebase-database-ktx:20.0.5") // Signaling
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    implementation ("com.google.code.gson:gson:2.11.0")

    androidTestImplementation(libs.androidx.espresso.core)
}