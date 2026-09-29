plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.havenapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.havenapp"
        minSdk = 24
        targetSdk = 36
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

    // ============================
    // ANDROIDX
    // ============================

    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.constraintlayout)


    // ============================
    // MATERIAL DESIGN
    // ============================

    implementation(libs.material)


    // ============================
    // RETROFIT / API
    // ============================

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)


    // ============================
    // GLIDE - PROFILE IMAGES
    // ============================

    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)


    // ============================
    // GOOGLE PLAY SERVICES
    // LOCATION / GPS
    // ============================

    implementation("com.google.android.gms:play-services-location:21.3.0")


    // ============================
    // TESTING
    // ============================

    testImplementation(libs.junit)

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}