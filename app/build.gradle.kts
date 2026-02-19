import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.firebase.appdistribution")

}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.joshua.chokepoint"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.joshua.chokepoint"
        minSdk = 29
        targetSdk = 36
        versionCode = System.getenv("BUILD_VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("BUILD_VERSION_NAME") ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        val webClientId = localProperties.getProperty("default_web_client_id") ?: System.getenv("DEFAULT_WEB_CLIENT_ID") ?: ""
        buildConfigField("String", "DEFAULT_WEB_CLIENT_ID", "\"$webClientId\"")

        val mqttBrokerUrl = localProperties.getProperty("mqtt_broker_url") ?: System.getenv("MQTT_BROKER_URL") ?: ""
        buildConfigField("String", "MQTT_BROKER_URL", "\"$mqttBrokerUrl\"")

        val mqttUsername = localProperties.getProperty("mqtt_username") ?: System.getenv("MQTT_USERNAME") ?: ""
        buildConfigField("String", "MQTT_USERNAME", "\"$mqttUsername\"")

        val mqttPassword = localProperties.getProperty("mqtt_password") ?: System.getenv("MQTT_PASSWORD") ?: ""
        buildConfigField("String", "MQTT_PASSWORD", "\"$mqttPassword\"")

        val razorpayKey = localProperties.getProperty("razorpay_key_id") ?: System.getenv("RAZORPAY_KEY_ID") ?: ""
        buildConfigField("String", "RAZORPAY_KEY_ID", "\"$razorpayKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            configure<com.google.firebase.appdistribution.gradle.AppDistributionExtension> {
                appId = "1:164679848850:android:f2f6f8d7e75333a8d6a74b"
                testers = "joshuadude2715@gmail.com"
                releaseNotes = "Automated Debug/CI Build"
            }
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
    implementation(libs.firebase.appdistribution)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.paho.mqtt)
    implementation(libs.paho.android.service)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.coil.compose)
    implementation("com.razorpay:checkout:1.6.40")
    implementation("com.google.android.gms:play-services-wallet:19.4.0")
}
