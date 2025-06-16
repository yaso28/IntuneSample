plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.microsoft.intune.mam")
}

android {
    namespace = "com.yaso28.sample202506"
    compileSdk = 35

    signingConfigs {
        getByName("debug") {
            storeFile = file("/Users/yaso28/Documents/apps/android/sample202506.jks")
            storePassword = "y28#Develop"
            keyAlias = "Sample202506"
            keyPassword = "y28#Develop"
        }
    }

    defaultConfig {
        applicationId = "com.yaso28.sample202506"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "0.0.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.msal)
    implementation(files("libs/Microsoft.Intune.MAM.SDK.aar"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}