plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// The app ID can be overridden at build time:  ./gradlew assembleDebug -PappId=some.package.name
// See README.md ("Why the app ID matters") before changing it.
val appId: String = (project.findProperty("appId") as String?) ?: "net.ourabridge"

android {
    namespace = "net.ourabridge"
    compileSdk = 35

    defaultConfig {
        applicationId = appId
        minSdk = 34          // Pokémon Sleep's Health Connect sync needs Android 14+
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug") // personal sideload build
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
