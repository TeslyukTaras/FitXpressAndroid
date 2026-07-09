import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.kotlinx.serialization)
}

val localProps = Properties()
val localPropsFile: File = rootProject.file("local.properties")
if (localPropsFile.exists()) localProps.load(localPropsFile.inputStream())

fun terraDevId(key: String): String =
    localProps.getProperty(key)
        ?: providers.environmentVariable(key.replace('.', '_').uppercase()).orNull
        ?: ""

val buildsProdFlavor = gradle.startParameter.taskNames.any { it.contains("prod", ignoreCase = true) }

val prodTerraDevId: String = terraDevId("terra.prod.dev.id").also {
    if (buildsProdFlavor && it.isBlank()) {
        error(
            "terra.prod.dev.id is not set. Add it to local.properties (or TERRA_PROD_DEV_ID in the " +
                "environment). It must equal the PROD_TERRA_DEV_ID Firebase secret."
        )
    }
}

android {
    namespace = "com.hexis.bi"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hexis.bi"
        // Terra Android SDK requires minSdk 28 (Samsung Health / Health Connect).
        minSdk = 28
        targetSdk = 37
        versionCode = 11
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            // x86_64 excluded to eliminate the 16KB-page-size (4KB-aligned .so) warning.
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86"))
        }
    }

    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            //applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            resValue("string", "app_name", "Hexis-BI Dev")
            buildConfigField("String", "ENVIRONMENT", "\"dev\"")
            buildConfigField("String", "API_BASE_URL", "\"https://api.dev.hexis.bi/\"")
            buildConfigField("String", "TERRA_FUNCTION_PREFIX", "\"terraDev\"")
            buildConfigField("String", "TERRA_DEV_ID", "\"${terraDevId("terra.dev.id")}\"")
            buildConfigField("boolean", "TERRA_INCLUDE_DUMMY_PROVIDER", "true")
        }
        create("stage") {
            dimension = "env"
            //applicationIdSuffix = ".stage"
            versionNameSuffix = "-stage"
            resValue("string", "app_name", "Hexis-BI Stage")
            buildConfigField("String", "ENVIRONMENT", "\"stage\"")
            buildConfigField("String", "API_BASE_URL", "\"https://api.stage.hexis.bi/\"")
            buildConfigField("String", "TERRA_FUNCTION_PREFIX", "\"terraDev\"")
            buildConfigField("String", "TERRA_DEV_ID", "\"${terraDevId("terra.dev.id")}\"")
            buildConfigField("boolean", "TERRA_INCLUDE_DUMMY_PROVIDER", "false")
        }
        create("prod") {
            dimension = "env"
            resValue("string", "app_name", "Hexis-BI")
            buildConfigField("String", "ENVIRONMENT", "\"prod\"")
            buildConfigField("String", "API_BASE_URL", "\"https://api.hexis.bi/\"")
            buildConfigField("String", "TERRA_FUNCTION_PREFIX", "\"terraProd\"")
            buildConfigField("String", "TERRA_DEV_ID", "\"${prodTerraDevId}\"")
            buildConfigField("boolean", "TERRA_INCLUDE_DUMMY_PROVIDER", "false")
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.look.camera.sdk)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.timber)
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.terra.android)
    implementation(libs.androidx.browser)
    implementation(libs.libphonenumber)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
