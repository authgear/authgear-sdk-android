import java.util.Properties

plugins {
    id("com.android.application")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { fis ->
        localProperties.load(fis)
    }
}

android {
    namespace = "com.oursky.authgeartest"
    compileSdk = 34
    buildToolsVersion = "34.0.0"

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        multiDexEnabled = true
        applicationId = "com.authgear.sdk.exampleapp.android"
        // minSdk is set to 23 so that we do not need version check to use biometric and app2app.
        minSdk = 23
        targetSdk = 34
        if (project.hasProperty("VERSION_CODE")) {
            versionCode = Integer.parseInt(project.findProperty("VERSION_CODE") as String)
        } else {
            versionCode = 1
        }
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "LOCAL_AUTHGEAR_CLIENT_ID", "\"${localProperties.getProperty("local.authgear.clientId")}\"")
        buildConfigField("String", "LOCAL_AUTHGEAR_ENDPOINT", "\"${localProperties.getProperty("local.authgear.endpoint")}\"")
    }

    if (project.hasProperty("STORE_FILE")) {
        signingConfigs {
            create("release") {
                storeFile = File(project.findProperty("STORE_FILE") as String)
                storePassword = project.findProperty("STORE_PASSWORD") as String
                keyAlias = project.findProperty("KEY_ALIAS") as String
                keyPassword = project.findProperty("KEY_PASSWORD") as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (project.hasProperty("STORE_FILE")) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
    implementation(project(mapOf("path" to ":sdk")))
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.2.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.2.0")
    implementation("androidx.activity:activity:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.1")
    implementation("com.google.android.material:material:1.2.1")
    implementation("com.tencent.mm.opensdk:wechat-sdk-android:6.8.34")
    implementation("androidx.biometric:biometric:1.2.0-alpha03")
    testImplementation("junit:junit:4.12")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}
