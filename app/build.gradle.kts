plugins {
    alias(libs.plugins.android.application)

    // Firebase Google Services plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.monali.careermateai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.monali.careermateai"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))

    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth")

    // Cloud Firestore
    implementation("com.google.firebase:firebase-firestore")

    // Firebase Storage - for CV PDF uploads later
    implementation("com.google.firebase:firebase-storage")

    // Firebase Cloud Functions - for AI Career Coach backend call
    implementation("com.google.firebase:firebase-functions")

    // PDF text extraction
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}