plugins {
    id("com.android.application")
}

android {
    // Define el namespace de la aplicación para que AGP lo use en lugar de la declaración en AndroidManifest.xml
    namespace = "com.ugb.cuadrasmart"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ugb.cuadrasmart"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

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

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // SQLite Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // RecyclerView y LiveData
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    // MPAndroidChart para reportes
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // PDF Generation
    implementation("com.itextpdf:itext7-core:7.1.16")

    // Notificaciones Push
    implementation("com.google.firebase:firebase-messaging-ktx:23.3.1")

    // Glide para imágenes en chat
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
}
