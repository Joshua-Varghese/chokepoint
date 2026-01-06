import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.joshua.chokepoint"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.joshua.chokepoint"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        val webClientId = localProperties.getProperty("default_web_client_id") ?: ""
        buildConfigField("String", "DEFAULT_WEB_CLIENT_ID", "\"$webClientId\"")

        val mqttBrokerUrl = localProperties.getProperty("mqtt_broker_url") ?: ""
        buildConfigField("String", "MQTT_BROKER_URL", "\"$mqttBrokerUrl\"")

        val mqttUsername = localProperties.getProperty("mqtt_username") ?: ""
        buildConfigField("String", "MQTT_USERNAME", "\"$mqttUsername\"")

        val mqttPassword = localProperties.getProperty("mqtt_password") ?: ""
        buildConfigField("String", "MQTT_PASSWORD", "\"$mqttPassword\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.paho.mqtt)
    implementation(libs.paho.android.service)
    implementation(libs.androidx.localbroadcastmanager)
}
