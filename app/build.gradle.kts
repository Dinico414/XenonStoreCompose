plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "1.9.24"
}

android {
    namespace = "com.xenonware.store"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.xenonware.store"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "2.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "XENON_COMMONS_VERSION", "\"${libs.versions.xenonCommons.get()}\"")
        buildConfigField("String", "XENON_UI_VERSION", "\"${libs.versions.xenonUi.get()}\"")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-d"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "XENON_COMMONS_VERSION", "\"${libs.versions.xenonCommons.get()}\"")
            buildConfigField("String", "XENON_UI_VERSION", "\"${libs.versions.xenonUi.get()}\"")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "XENON_COMMONS_VERSION", "\"${libs.versions.xenonCommons.get()}\"")
            buildConfigField("String", "XENON_UI_VERSION", "\"${libs.versions.xenonUi.get()}\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    applicationVariants.all {
        outputs.all {
            val outputFileName = if (buildType.name == "release") {
                "XenonStore.apk"
            } else if (buildType.name == "debug") {
                "XenonStore-debug.apk"
            } else {
                "${project.name}-${buildType.name}.apk"
            }
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                outputFileName
        }
    }
}

dependencies {

    implementation(libs.coil.compose)

    implementation(libs.xenon.commons)
    implementation(libs.androidx.material3.window.size.class1.android)
    implementation(libs.androidx.material3.adaptive)
    val composeBom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.haze)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.androidx.animation.graphics)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.haze.materials)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.ui)
    implementation(libs.androidx.appcompat)
    implementation(libs.mathparser.org.mxparser)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.api)
    implementation(libs.provider)
}