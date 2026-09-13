plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.vsmelov.liveclock"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.vsmelov.liveclock"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        // Release подписывается тем же debug-ключом. Для личного приложения,
        // которое ставится по USB, отдельный keystore — лишняя сущность:
        // его нельзя положить в репозиторий, а потеряв его, нельзя обновить
        // уже установленную сборку. Свой ключ пропиши здесь, если дойдёт
        // до публикации.
        create("releaseLocal") {
            val debugStore = File(System.getProperty("user.home"), ".android/debug.keystore")
            storeFile = debugStore
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            signingConfig = signingConfigs.getByName("releaseLocal")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        // Критерий приёмки: сборка без варнингов. Проверяет билд, а не глаз.
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)

    // Виджет
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Конфигурационная Activity
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
