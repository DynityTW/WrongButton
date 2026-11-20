import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/**
 * 上傳金鑰（upload key）。金鑰檔與密碼絕對不能進 repo：
 * 從專案根目錄的 keystore.properties（已列入 .gitignore）或環境變數讀取，兩者都沒有時正式版不簽名。
 * 步驟見 docs/RELEASE.md。
 */
val signingProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? = signingProps.getProperty(key) ?: System.getenv(env)
val uploadStore = signingValue("storeFile", "WB_UPLOAD_STORE_FILE")

android {
    namespace = "tw.dynity.wrongbutton"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.wrongbutton.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 5
        versionName = "0.5.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (uploadStore != null) {
            create("upload") {
                storeFile = rootProject.file(uploadStore)
                storePassword = signingValue("storePassword", "WB_UPLOAD_STORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "WB_UPLOAD_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "WB_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (uploadStore != null) signingConfig = signingConfigs.getByName("upload")
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.maxHeapSize = "2g"
                // Robolectric 自己下載 android-all 執行環境；走與 Gradle 相同的 Maven Central 鏡像。
                it.systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xcontext-parameters")
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.datastore)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.compose.ui.test.junit4)
}

// ---- 權限守門：合併後的 manifest 只能出現白名單裡的權限，其他一律讓建置失敗。 ----
abstract class VerifyPermissionsTask : DefaultTask() {
    @get:InputFile
    abstract val manifest: RegularFileProperty

    @get:Input
    abstract val allowed: SetProperty<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val text = manifest.get().asFile.readText()
        val tags = Regex("""<(uses-permission(-sdk-23)?|permission|permission-group|permission-tree)\b[^>]*>""").findAll(text).map { it.value }.toList()
        val names = tags.map { Regex("""android:name="([^"]+)"""").find(it)?.groupValues?.get(1) ?: it }
        val extra = names.filterNot { it in allowed.get() }
        check(extra.isEmpty()) { "合併後的 manifest 出現了白名單以外的權限：\n" + extra.joinToString("\n") }
        report.get().asFile.writeText("OK：只有白名單裡的權限。\n" + names.distinct().joinToString("\n"))
    }
}

/**
 * 權限白名單。
 * - DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION：androidx.core 自己定義、只給本 App 用的簽章權限。
 *   Android 8–12 上 ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED) 需要它，拿掉會當掉；它不是向使用者請求的權限。
 * - 之後接上真正的廣告與付費時，再加：INTERNET、ACCESS_NETWORK_STATE、com.google.android.gms.permission.AD_ID、com.android.vending.BILLING。
 */
fun allowedPermissions(applicationId: String) = setOf(
    "$applicationId.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
)

androidComponents {
    onVariants { variant ->
        val name = variant.name.replaceFirstChar { it.uppercase() }
        val verify = tasks.register<VerifyPermissionsTask>("verify${name}Permissions") {
            manifest.set(variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.MERGED_MANIFEST))
            allowed.set(variant.applicationId.map { allowedPermissions(it) })
            report.set(layout.buildDirectory.file("reports/permissions/${variant.name}.txt"))
        }
        tasks.matching { it.name == "assemble$name" || it.name == "bundle$name" }.configureEach { dependsOn(verify) }
    }
}
