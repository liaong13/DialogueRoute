import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 签名配置由环境变量显式指定；未配置时生成未签名的 release 包。
val releaseProps = System.getenv("DIALOGUE_ROUTE_KEYSTORE_PROPS")?.takeIf { it.isNotBlank() }?.let { path ->
    val propsFile = rootProject.file(path)
    require(propsFile.isFile) { "DIALOGUE_ROUTE_KEYSTORE_PROPS must point to an existing properties file" }
    Properties().apply {
        FileInputStream(propsFile).use { load(it) }
        for (key in listOf("storeFile", "storePassword", "keyAlias", "keyPassword")) {
            require(!getProperty(key).isNullOrBlank()) { "Missing release signing property: $key" }
        }
    }
}

android {
    namespace = "io.github.liaong13.dialogueroute"
    compileSdk = 37

    buildFeatures {
        compose = true
    }

    defaultConfig {
        applicationId = "io.github.liaong13.dialogueroute"
        minSdk = 30
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"

        // 当前发行配置仅包含 arm64-v8a，以控制离线 OCR 模型的包体大小。
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        if (releaseProps != null) {
            create("release") {
                storeFile = rootProject.file(releaseProps.getProperty("storeFile"))
                storePassword = releaseProps.getProperty("storePassword")
                keyAlias = releaseProps.getProperty("keyAlias")
                keyPassword = releaseProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    // Uncompressed, page-aligned .so files: required for the 16 KB page-size
    // devices Android 15+ ships, and it lets the loader mmap the ML Kit natives
    // instead of unpacking them at install time.
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // 由框架在宿主进程提供，应用正常启动时不加载这些类。
    compileOnly("io.github.libxposed:api:102.0.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("top.yukonga.miuix.kmp:miuix-ui:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-preference:0.9.3")
    implementation("top.yukonga.miuix.kmp:miuix-icons:0.9.3")
    // On-device OCR. The *bundled* Chinese model (not the play-services variant):
    // it works on phones with no Google Play services and needs no model download.
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")

    testImplementation("junit:junit:4.13.2")
}
