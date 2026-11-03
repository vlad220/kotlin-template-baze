import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Релизная подпись: подключается, если рядом есть keystore.properties (в .gitignore,
// секрет). Без него release-сборка остаётся неподписанной и не ломает сборку.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties()
if (keystorePropsFile.exists()) {
    FileInputStream(keystorePropsFile).use { keystoreProps.load(it) }
}

android {
    // В новом проекте поменяйте на свой applicationId (например com.vasha.firma)
    namespace = "com.template"
    compileSdk = 34
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.template"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "API_BASE_URL", "\"https://vsem72.ru/api/v1/\"")
        buildConfigField("String", "APP_NAME", "\"template\"")
        // Ключ Яндекс.Карт для JS API внутри WebView. Серверного ключа нет, поэтому
        // геокодер работает системным android.location.Geocoder, а не Яндексом.
        buildConfigField("String", "YANDEX_MAPS_API_KEY", "\"509f2bc0-3b75-4fe1-a8e0-ccb30902d6f4\"")
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
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
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug { 
            isDebuggable = true 
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
        buildConfig = true
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.coroutines.android)
    implementation(libs.play.services.auth.api.phone)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    debugImplementation(libs.compose.ui.tooling)
}