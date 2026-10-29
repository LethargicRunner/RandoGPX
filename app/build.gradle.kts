plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mathieu.randogpx"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mathieu.randogpx"
        minSdk = 24
        targetSdk = 34
        versionCode = 17
        versionName = "2.1"
    }

    // Signature release : n'active la signature que si un keystore est fourni
    // (fichier release-signing/randogpx.jks créé par le workflow, mots de passe
    // passés via les secrets GitHub KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD).
    val keystoreFile = rootProject.file("release-signing/randogpx.jks")
    signingConfigs {
        create("release") {
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "randogpx2026"
                keyAlias = System.getenv("KEY_ALIAS") ?: "randogpx"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "randogpx2026"
            }
        }
    }

    buildTypes {
        release {
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.osmdroid:osmdroid-android:6.1.18")
}
