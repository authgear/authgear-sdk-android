// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Android Gradle Plugin 8.1 always use SDK Build Tools 33.0.1
    // https://developer.android.com/build/releases/gradle-plugin#8-1-0
    // If you ever update Android Gradle Plugin, please also update
    // BUILD_TOOLS_VERSION in ./.github/workflows/ci.yaml
    id("com.android.library") version "8.1.2" apply false
    id("com.android.application") version "8.1.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    kotlin("plugin.serialization") version "1.9.20" apply false
    id("org.jetbrains.dokka") version "1.9.10"
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")
}