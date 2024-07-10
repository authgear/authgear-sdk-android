plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
    id("maven-publish")
}

android {
    namespace = "com.oursky.authgear.okhttp"
    compileSdk = 34

    defaultConfig {
        multiDexEnabled = true
        aarMetadata {
            minCompileSdk = 21
        }

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
        // https://developer.android.com/studio/write/java8-support#library-desugaring
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        // -Xjvm-default=all is required to use Java 8 default methods in interface.
        // Using @JvmDefault annotation is an error.
        freeCompilerArgs += listOf("-Xjvm-default=all")
    }
}

dependencies {
    // https://developer.android.com/studio/write/java8-support#library-desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation(project(mapOf("path" to ":sdk")))
    testImplementation("junit:junit:4.12")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = findProperty("group") as String
            artifactId = "authgear-sdk-android-okhttp"
            version = findProperty("version") as String

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
