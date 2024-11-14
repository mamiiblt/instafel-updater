plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "me.mamiiblt.instafel.updater"
    compileSdk = 34

    // disable include metadata in dep infos
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    defaultConfig {
        applicationId = "me.mamiiblt.instafel.updater"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0-beta"

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
    buildFeatures {
        aidl = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

var shizuku_version = "13.1.5"
var work_version = "2.9.1"
var okhttp_version = "4.9.0"

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation("dev.rikka.shizuku:api:$shizuku_version")
    implementation("dev.rikka.shizuku:provider:$shizuku_version")
    implementation("androidx.work:work-runtime:$work_version")
    implementation("com.squareup.okhttp3:okhttp:$okhttp_version")
    implementation(libs.navigation.ui)
    implementation(libs.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}