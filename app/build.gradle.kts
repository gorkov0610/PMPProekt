plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "uklo.fikt.pmp.pmpproekt"
    compileSdk = 36

    defaultConfig {
        applicationId = "uklo.fikt.pmp.pmpproekt"
        minSdk = 26
        targetSdk = 36
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

    }
    buildFeatures {
        compose = true
    }
    sourceSets {
        getByName("main") {
            java.srcDirs("build/generated/ksp/main/java", "build/generated/ksp/main/kotlin")
        }
        getByName("debug") {
            java.srcDirs("build/generated/ksp/debug/java", "build/generated/ksp/debug/kotlin")
        }
    }
}
kotlin {
    jvmToolchain(17)
    sourceSets.all {
        languageSettings.optIn("androidx.media3.common.util.UnstableApi")
    }
}
dependencies {
    //room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.media3.common.ktx)
    ksp(libs.androidx.room.compiler)

    //Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth) // Authentication
    implementation(libs.firebase.firestore) // Firestore
    implementation(libs.firebase.messaging) // Messaging
    implementation(libs.firebase.analytics) // Analytics

    //Google Play and Facebook
    implementation(libs.play.services.auth)
    implementation(libs.facebook.android.sdk)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.material)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.material3)
    implementation(libs.coil.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}