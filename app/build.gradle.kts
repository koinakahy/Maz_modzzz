plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// true only when ./bcore (BlackBox core module) exists in the project root
val hasEngine = rootProject.file("bcore").exists()

android {
    namespace = "com.mazmodzzz.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mazmodzzz.launcher"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    if (hasEngine) {
        sourceSets.getByName("main").java.srcDir("src/engine/kotlin")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    if (hasEngine) {
        implementation(project(":bcore"))
    }
}
