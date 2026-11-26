import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
}

// Downloaded models need a bundled inference runtime; split APKs avoid duplicating every ABI.
val quickAddInferenceRuntime = providers.gradleProperty("quickAddInferenceRuntime")
    .map(String::toBooleanStrict).getOrElse(true)
val quickAddInferenceAbis = providers.gradleProperty("quickAddInferenceAbis").orNull
    ?.split(',')?.map(String::trim)?.filter(String::isNotEmpty)
    ?.also { abis ->
        require(abis.isNotEmpty() && abis.all { it in setOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64") }) {
            "quickAddInferenceAbis must contain valid Android ABIs"
        }
    }

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val deadlinerCorePath = providers.gradleProperty("deadlinerCoreDir")
    .orElse(providers.environmentVariable("DEADLINER_CORE_DIR"))
    .orNull ?: localProperties.getProperty("deadlinerCoreDir", "../DeadlinerCore")
val deadlinerCoreDirectory = rootProject.file(deadlinerCorePath)
val deadlinerCoreDeployScript = deadlinerCoreDirectory.resolve("runscript/buildAndDeploy.sh")
val deadlinerLifiMode = providers.gradleProperty("deadlinerLifiMode")
    .orElse(providers.environmentVariable("DEADLINER_LIFI_MODE")).orElse("private")
val deadlinerCoreSource = providers.gradleProperty("deadlinerCoreSource")
    .orElse(providers.environmentVariable("DEADLINER_CORE_SOURCE"))
    .getOrElse(localProperties.getProperty("deadlinerCoreSource", "package"))
val deadlinerCoreArtifacts = listOf("debug", "release").associateWith { configuration ->
    val suffix = configuration.replaceFirstChar { it.uppercase() }
    val aar = rootProject.file(".deadliner-core/kmp/$configuration/deadliner-core.aar")
    val syncCore = tasks.register<Exec>("syncDeadlinerCoreKmp$suffix") {
        group = "deadliner"
        description = "Install the $configuration Core AAR before Android compilation."
        outputs.file(aar)
        inputs.property("coreSource", deadlinerCoreSource)
        when (deadlinerCoreSource) {
            "package" -> {
                val installer = rootProject.file("scripts/core_package.py")
                val lock = rootProject.file("core-package.lock.json")
                inputs.files(installer, lock)
                val python = if (System.getProperty("os.name").startsWith("Windows")) "python" else "python3"
                commandLine(python, installer, "android", aar, "--lock", lock)
            }
            "local" -> {
                inputs.property("lifiMode", deadlinerLifiMode)
                inputs.files(fileTree(deadlinerCoreDirectory) {
                    include("shared/src/**", ".private/lifi/src/**", ".private/lifi/resources/**",
                        ".private/lifi/lifi-api-version", "lifi.revision", "shared/build.gradle.kts",
                        "build.gradle.kts", "settings.gradle.kts", "gradle.properties", "gradle/**", "runscript/**")
                    exclude("**/build/**", "**/__pycache__/**", "**/.DS_Store")
                })
                commandLine(deadlinerCoreDeployScript, "--configuration", configuration,
                    "--lifi-mode", deadlinerLifiMode.get(), "--skip-ios", "--skip-harmony",
                    "--android-repo", rootProject.projectDir)
            }
            else -> error("deadlinerCoreSource must be package or local")
        }
    }
    tasks.matching { it.name == "pre${suffix}Build" }.configureEach { dependsOn(syncCore) }
    files(aar).builtBy(syncCore)
}

// Keep the original manual entry points for release AAR consumers.
tasks.register("syncDeadlinerCoreKmp") { dependsOn("syncDeadlinerCoreKmpRelease") }
tasks.register("verifyDeadlinerCoreKmpAar") {
    dependsOn("syncDeadlinerCoreKmpRelease")
    doLast { check(deadlinerCoreArtifacts.getValue("release").singleFile.isFile) }
}

android {
    sourceSets.getByName("main").kotlin.srcDir(rootProject.file("third_party/miuix-glass/src/commonMain/kotlin"))

    namespace = "com.aritxonly.deadliner"
    compileSdk = 37

    defaultConfig {
        if (!quickAddInferenceRuntime) quickAddInferenceAbis?.let { ndk { abiFilters += it } }
        applicationId = "com.aritxonly.deadliner"
        minSdk = 33
        targetSdk = 36
        versionCode = 33
        versionName = "5.0.0-RC"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val timeStamp = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())
        buildConfigField("String", "BUILD_TIME", "\"$timeStamp\"")

        val secret: String =
            env.fetchOrNull("DEADLINER_APP_SECRET")              // .env
                ?: System.getenv("DEADLINER_APP_SECRET")           // CI 环境变量兜底
                ?: System.getProperty("DEADLINER_APP_SECRET")      // -D 注入兜底
                ?: ""
        buildConfigField("String", "DEADLINER_APP_SECRET", "\"$secret\"")
    }
    splits {
        abi {
            isEnable = quickAddInferenceRuntime
            reset()
            include(*(quickAddInferenceAbis ?: listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64")).toTypedArray())
            isUniversalApk = providers.gradleProperty("quickAddUniversalApk").map(String::toBooleanStrict).getOrElse(false)
        }
    }
    lint {
        disable += "NullSafeMutableLiveData"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        aidl = true
        viewBinding = true
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    packaging {
        jniLibs {
            // Keep direct-download APKs within the size budget; Android extracts these on install.
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    ndkVersion = "26.1.10909125"
}

dependencies {
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("com.github.topjohnwu.libsu:service:6.0.0")
    debugImplementation(deadlinerCoreArtifacts.getValue("debug"))
    releaseImplementation(deadlinerCoreArtifacts.getValue("release"))
    // File-based KMP AARs do not carry transitive runtime dependencies.
    if (quickAddInferenceRuntime) {
        implementation("com.microsoft.onnxruntime:onnxruntime-android:1.31.0")
    }
    implementation("app.cash.sqldelight:android-driver:2.2.1-0.3.0")
    // Local Core AARs have no transitive POM: focus statistics uses SQLDelight query Flows.
    implementation("app.cash.sqldelight:coroutines-extensions:2.2.1-0.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2-0.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1-0.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.1-0.3.0")
    implementation("io.ktor:ktor-client-okhttp:3.3.3-0.3.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.3.3-0.3.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.3-0.3.0")
    implementation("androidx.work:work-runtime-ktx:2.10.3")
    implementation("nl.dionsegijn:konfetti-xml:2.0.4")
    implementation("nl.dionsegijn:konfetti-compose:2.0.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("androidx.compose.material3:material3-window-size-class:1.4.0-beta02")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.4.0-beta02")
    implementation("io.github.ehsannarmani:compose-charts:0.1.7")
    implementation("androidx.navigation:navigation-compose:2.9.1")
    implementation("com.github.jeziellago:compose-markdown:0.5.7")
    implementation("androidx.window:window:1.4.0")
    implementation("androidx.startup:startup-runtime:1.1.1")
    implementation("com.airbnb.android:lottie:6.6.6")
    implementation("androidx.databinding:viewbinding:8.10.1")
    implementation("androidx.compose.runtime:runtime-livedata:1.9.1")
    implementation("androidx.datastore:datastore-preferences-android:1.1.7")
    implementation("io.github.rroohit:ImageCropView:3.1.1")
    implementation("com.materialkolor:material-kolor:4.1.1")
    implementation("top.yukonga.miuix.kmp:miuix-ui-android:0.9.4-rc01")
    implementation("top.yukonga.miuix.kmp:miuix-icons-android:0.9.4-rc01")
    implementation("top.yukonga.miuix.kmp:miuix-blur-android:0.9.4-rc01")
    implementation("top.yukonga.miuix.kmp:miuix-nav-android:0.9.4-rc01")
    implementation("top.yukonga.miuix.kmp:miuix-preference-android:0.9.4-rc01")
    implementation("io.github.kyant0:shapes:1.2.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.animation.tooling.internal)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.core.animation)
    implementation(libs.androidx.ui.viewbinding)
    implementation(libs.androidx.glance.appwidget)
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation(libs.androidx.ui.test.junit4)
    testImplementation(libs.androidx.espresso.core)
    testImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
