plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.aipulse"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.aipulse"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            var minifyEnabled = false
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

    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")


    // ✅ TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:2.12.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.3")
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.3")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // ✅ Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // ✅ AndroidX + UI
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ✅ Tests
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
