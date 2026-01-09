plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.lenovo.levoice.caption"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.lenovo.levoice.caption"
        minSdk = 29
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // RecyclerView, CardView, AppCompat for GalleryActivity
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

//    implementation("androidx.compose.remote:remote-core:1.0.0-alpha01")
//
//    // Use to create Remote Compose documents
//    implementation("androidx.compose.remote:remote-creation:1.0.0-alpha01")
//    implementation("androidx.compose.remote:remote-creation-core:1.0.0-alpha01")
//    implementation("androidx.compose.remote:remote-creation-android:1.0.0-alpha01")
//    implementation("androidx.compose.remote:remote-creation-compose:1.0.0-alpha01")
//
//    // Use to render a Remote Compose document
//    implementation("androidx.compose.remote:remote-player-core:1.0.0-alpha01")
//    implementation("androidx.compose.remote:remote-player-view:1.0.0-alpha01")
//
//    implementation("androidx.compose.remote:remote-tooling-preview:1.0.0-alpha01")

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}