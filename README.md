# Authgear SDK for Android

![License](https://img.shields.io/badge/license-Apache%202-blue)

## Usage

Authgear requires at minimum Android API 21+.

1. Add jitpack repository to gradle
```gradle
allprojects {
    repositories {
        // Other repository
        maven { url 'https://jitpack.io' }
    }
}
```
2. Add authgear in dependencies. Use `$branch-SNAPSHOT` (e.g. `main-SNAPSHOT`) for latest version in a branch, or a release tag/git commit hash of desired version.
```gradle
dependencies {
    // Other implementations
    implementation 'com.github.authgear:authgear-sdk-android:SNAPSHOT'
}
```

## Contributing

Fork this repository and make sure PRs pass format and lint check.

### Prerequisite

Android Studio 4.0+ (For Java 8 library desugaring)