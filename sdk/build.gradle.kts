plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
    // NOTE(maven-publish): Step 1: install the plugins
    id("maven-publish")
    id("signing")
}

android {
    namespace = "com.oursky.authgear"
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

    // NOTE(maven-publish): Step 2: configure a Maven Software component.
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    // https://developer.android.com/studio/write/java8-support#library-desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("androidx.core:core-ktx:1.8.0")
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

// NOTE(maven-publish): Step 3: configure signing
// https://docs.gradle.org/current/userguide/signing_plugin.html#using_in_memory_ascii_armored_openpgp_subkeys
// The following setup assumes the following environment variables are set
// - ORG_GRADLE_PROJECT_signingKeyId: The last 8 letters of the GPG key ID. You can get it with `gpg -k`.
// - ORG_GRADLE_PROJECT_signingKey: The ASCII-armor GPG private key. You can get it with `gpg --export-secret-key --armor`.
// - ORG_GRADLE_PROJECT_signingPassword: The password of the GPG key. You can get it from 1Password.
//
// Note that the public key has been uploaded to ubuntu GPG keyserver.
// So Maven Central knows how to verify the signature.
signing {
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications)
}

// NOTE(maven-publish): Step 4: configure the publication.
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.authgear"
            artifactId = "android-sdk"
            // You have to replace this to publish a new version.
            // Gradle will complain anyway if you forget to do so.
            version = ""

            // All of these are requirements by Maven Central.
            // See https://central.sonatype.org/publish/requirements/
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

            afterEvaluate {
                from(components["release"])
            }
        }
    }

    // NOTE(maven-publish): Step 5: build
    // To build files you can upload to Maven Central manually, you have to run
    //
    //   ./gradlew :sdk:publish
    //
    // It will not work if you run publishToMavenLocal, as it does not generate the necessary checksum files.
    // See https://github.com/gradle/gradle/issues/22482
    //
    // NOTE(maven-publish): Step 6: upload the files manually.
    // On https://central.sonatype.com/publishing/namespaces
    // you are allowed to upload a single file.
    // Thus you need to go to ./build/maven-repository
    // and zip the `com` directory.
    // And upload the zip.
    // The contents of the zip should look like
    //
    // com/
    //   authgear/
    //     android-sdk/
    //       [version]/
    //         android-sdk-[version]-javadoc.jar
    //         android-sdk-[version]-javadoc.jar.asc
    //         android-sdk-[version]-javadoc.jar.md5
    //         ...
    repositories {
        // Tell maven publishing plugin that we want to publish to this local directory.
        maven {
            url = uri(layout.buildDirectory.dir("maven-repository"))
        }
    }
}
