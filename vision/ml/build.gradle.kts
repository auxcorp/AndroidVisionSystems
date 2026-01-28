plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.industrialvision.vision.ml"
    compileSdk = rootProject.extra["compileSdk"] as Int

    defaultConfig {
        minSdk = rootProject.extra["minSdk"] as Int
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        mlModelBinding = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:common"))

    // Hilt
    implementation("com.google.dagger:hilt-android:${rootProject.extra["hiltVersion"]}")
    ksp("com.google.dagger:hilt-android-compiler:${rootProject.extra["hiltVersion"]}")

    // ML Kit
    implementation("com.google.mlkit:barcode-scanning:${rootProject.extra["mlkitVersion"]}")
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.mlkit:object-detection:17.0.0")
    implementation("com.google.mlkit:image-labeling:17.0.7")

    // TensorFlow Lite
    implementation("org.tensorflow:tensorflow-lite:${rootProject.extra["tensorflowVersion"]}")
    implementation("org.tensorflow:tensorflow-lite-gpu:${rootProject.extra["tensorflowVersion"]}")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
