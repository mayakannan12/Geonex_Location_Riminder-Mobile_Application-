plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.geonex"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.geonex"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_17  // Updated for newer AGP
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }



    configurations.all {
        resolutionStrategy {
            force("androidx.work:work-runtime:2.9.0")
        }
    }
}

dependencies {

    implementation("com.google.android.material:material:1.11.0")
    // AndroidX Core Libraries - Use versions compatible with AGP 8.2.0
    implementation("androidx.core:core:1.12.0")              // Changed from 1.17.0
    implementation("androidx.appcompat:appcompat:1.6.1")     // Stable version
    implementation("com.google.android.material:material:1.11.0")  // Stable version
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Lifecycle Components
    implementation("androidx.lifecycle:lifecycle-runtime:2.7.0")        // Compatible version
    implementation("androidx.lifecycle:lifecycle-common:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")

    // Navigation
    implementation("androidx.navigation:navigation-fragment:2.7.6")
    implementation("androidx.navigation:navigation-ui:2.7.6")

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.work:work-runtime:2.11.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // Google Play Services
    implementation("com.google.android.gms:play-services-maps:18.2.0")   // Older stable version
    implementation("com.google.android.gms:play-services-location:21.0.1") // Older stable version

    // RecyclerView and ViewPager2 - Use compatible versions
    implementation("androidx.recyclerview:recyclerview:1.3.2")   // Compatible with compileSdk 34
    implementation("androidx.viewpager2:viewpager2:1.0.0")       // Stable version

    // Startup Runtime
    implementation("androidx.startup:startup-runtime:1.1.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // WorkManager for background tasks
    implementation("androidx.work:work-runtime:2.9.0")

    // Biometric authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Security Crypto (for encryption)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Biometric authentication
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Security Crypto (for encryption)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Splash screen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Core Android - MUST have appcompat
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Biometric
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation ("com.google.android.material:material:1.9.0")
    // or latest version
    implementation ("com.google.android.material:material:1.12.0")


}