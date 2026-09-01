import java.util.Properties

// ── Load local.properties ─────────────────────────────────────────────────────
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.collagemanagmentsystem"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.collagemanagmentsystem"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ── Inject secrets from local.properties into BuildConfig ─────────────
        buildConfigField("String", "SMTP_SENDER_EMAIL",
            "\"${localProperties.getProperty("SMTP_SENDER_EMAIL", "")}\"")
        buildConfigField("String", "SMTP_SENDER_PASSWORD",
            "\"${localProperties.getProperty("SMTP_SENDER_PASSWORD", "")}\"")
        buildConfigField("String", "RAZORPAY_KEY_ID",
            "\"${localProperties.getProperty("RAZORPAY_KEY_ID", "")}\"")
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

    // ── Enable BuildConfig generation ─────────────────────────────────────────
    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.md",
                "META-INF/NOTICE.txt",
                "META-INF/ASL2.0",
                "META-INF/LGPL2.1"
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("de.hdodenhof:circleimageview:3.1.0")

    implementation("com.sun.mail:android-mail:1.6.8")
    implementation("com.sun.mail:android-activation:1.6.8")

    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation("com.razorpay:checkout:1.6.33")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")



}