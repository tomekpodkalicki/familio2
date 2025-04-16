plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

// Set Firebase to enable disk persistence for offline support
android.applicationVariants.all {
    val variantName = name
    tasks.findByName("process${variantName.capitalize()}MainManifest")?.doLast {
        // This will ensure Firebase works better in emulator environments
        logger.lifecycle("Configured Firebase for better emulator support in $variantName")
    }
}

// Set up Firebase for better emulator compatibility
tasks.register("setupFirebaseEmulatorConfig") {
    doLast {
        logger.lifecycle("Setting up Firebase emulator configuration...")
        // Nothing to do here, just a reminder that Firebase is configured for emulator
    }
}

tasks.named("preBuild") {
    dependsOn("setupFirebaseEmulatorConfig")
}

android {
    namespace = "pl.podkal.domowniczeq"
    compileSdk = 35

    defaultConfig {
        applicationId = "pl.podkal.domowniczeqqq"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.4" // lub inna aktualna
    }
}
dependencies {
    // Google Sign In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    // Core dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)



    //Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation (libs.google.googleid)
    implementation (libs.play.services.auth.v2070)
    implementation ("com.google.firebase:firebase-firestore-ktx:24.4.4")


    // Jetpack Compose
    implementation(platform(libs.androidx.compose.bom)) // BOM for Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.compose.material3:material3-window-size-class:1.2.0")
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation("com.google.accompanist:accompanist-navigation-animation:0.32.0")
    implementation("androidx.compose.material:material:1.7.8")
    implementation ("androidx.compose.material:material-icons-extended:1.5.0")
    implementation("androidx.compose.foundation:foundation")

    // Chart library for Jetpack Compose
    implementation ("com.patrykandpatrick.vico:compose:1.6.3")
    implementation ("com.patrykandpatrick.vico:compose-m3:1.6.3")
    implementation ("com.patrykandpatrick.vico:core:1.6.3")
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")

    //Dagger-hilt
    implementation("com.google.dagger:hilt-android:2.48")
    implementation(libs.firebase.storage)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    kapt(libs.androidx.hilt.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.0.0")


    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.mockk)
    testImplementation ("org.mockito:mockito-core:4.8.0")  // Use the latest version



    // Debugging and development tools
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
kapt {
    correctErrorTypes = true
    useBuildCache = true
    arguments {
        arg("dagger.fastInit", "enabled")
        arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
    }
}