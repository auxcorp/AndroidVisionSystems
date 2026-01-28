// Industrial Android Vision Systems
// Professional Multi-Agent AI Vision Platform
// Powered by Deep Learning & LLM Technologies

buildscript {
    extra.apply {
        set("compileSdk", 34)
        set("minSdk", 26)
        set("targetSdk", 34)
        set("kotlinVersion", "1.9.22")
        set("composeVersion", "1.6.1")
        set("hiltVersion", "2.50")
        set("roomVersion", "2.6.1")
        set("cameraxVersion", "1.3.1")
        set("tensorflowVersion", "2.14.0")
        set("mlkitVersion", "17.0.1")
    }
}

plugins {
    id("com.android.application") version "8.2.2" apply false
    id("com.android.library") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.22" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
