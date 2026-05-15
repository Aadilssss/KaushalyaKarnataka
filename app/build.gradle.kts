plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.kaushalyakarnataka.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kaushalyakarnataka.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        val lp = rootProject.layout.projectDirectory.file("local.properties").asFile
        val webClientId = if (lp.exists()) {
            lp.readLines().firstOrNull { it.startsWith("WEB_CLIENT_ID=") }
                ?.substringAfter("=")?.trim().orEmpty()
        } else ""

        buildConfigField("String", "FIREBASE_API_KEY", "\"AIzaSyBogdyFB3IwdWz71E1g43uKWaurJb6sj94\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"gen-lang-client-0995334222\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"1:533144400361:android:b8f649fe931cd0b682db78\"")
        buildConfigField("String", "FIREBASE_STORAGE_BUCKET", "\"gen-lang-client-0995334222.firebasestorage.app\"")
        
        // CRITICAL FIX: Reverting to the correct specific Database ID for this project
        buildConfigField("String", "FIRESTORE_DATABASE_ID", "\"ai-studio-90349937-bc5c-40b8-9b17-b7bec6372e66\"")

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$webClientId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    implementation("com.google.android.gms:play-services-auth:21.3.0")

    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
