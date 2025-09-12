plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
    // NOTE(maven-publish): Step 1: install the plugins
    //
    // This plugin is recommended by https://www.jetbrains.com/help/kotlin-multiplatform-dev/multiplatform-publish-libraries.html
    // This is the only plugin I have seen so far that can handle Android library out-of-the-box.
    id("com.vanniktech.maven.publish") version "0.34.0"
}

android {
    namespace = "com.oursky.authgear"
    compileSdk = 35

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
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.4.0")
    // NOTE(backup): Please search NOTE(backup) before you update security-crypto or tink-android.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.crypto.tink:tink-android:1.8.0")
    implementation("androidx.browser:browser:1.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.2.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.2.0")
    implementation("androidx.biometric:biometric:1.2.0-alpha03")
    implementation("androidx.biometric:biometric-ktx:1.2.0-alpha03")
    testImplementation("junit:junit:4.12")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}

// NOTE(maven-publish): Step 4: configure the publishing plugin
// The required environment variables are documented at https://vanniktech.github.io/gradle-maven-publish-plugin/central/#secrets
//
// ORG_GRADLE_PROJECT_mavenCentralUsername       - The User Token username
// ORG_GRADLE_PROJECT_mavenCentralPassword       - The User Token password
// ORG_GRADLE_PROJECT_signingInMemoryKeyId       - The 8-digit GPG key ID
// ORG_GRADLE_PROJECT_signingInMemoryKeyPassword - The password of the GPG key
// ORG_GRADLE_PROJECT_signingInMemoryKey         - The contents of the ASCII armor GPG key
mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(
        "com.authgear",
        "android-sdk",
        System.getenv("GITHUB_REF_NAME") ?: "0.0.0-SNAPSHOT"
    )

    pom {
        name = "com.authgear:android-sdk"
        description = "Authgear SDK for Android"
        url = "https://github.com/authgear/authgear-sdk-android"

        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
            }
        }

        developers {
            developer {
                name = "Louis Chan"
                email = "louischan@oursky.com"
                organization = "Oursky"
                organizationUrl = "https://oursky.com"
            }
        }

        scm {
            connection = "scm:git:git://github.com/authgear/authgear-sdk-android.git"
            developerConnection = "scm:git:ssh://github.com/authgear/authgear-sdk-android.git"
            url = "https://github.com/authgear/authgear-sdk-android"
        }
    }
}
