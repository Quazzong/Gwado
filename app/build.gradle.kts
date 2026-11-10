import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
}

val versiSqlite = "2.7.1"  // sqlite, sqlite-bundled, dan .so test harus satu versi

// Kunci rilis dibaca dari keystore.properties di root proyek (tidak masuk Git):
// storeFile, storePassword, keyAlias, keyPassword. Kalau file itu tidak ada, APK rilis
// dibuat tanpa tanda tangan.
val kunciRilis = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { berkas ->
    Properties().apply { berkas.inputStream().use { load(it) } }
}

android {
    namespace = "io.github.quazzong.gwado"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.quazzong.gwado"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (kunciRilis != null) {
            create("rilis") {
                storeFile = file(kunciRilis.getProperty("storeFile"))
                storePassword = kunciRilis.getProperty("storePassword")
                keyAlias = kunciRilis.getProperty("keyAlias")
                keyPassword = kunciRilis.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("rilis")
            optimization {
                // Seluruh aplikasi dioptimalkan R8. Optimasi sebagian (packageScope) memindahkan
                // sebagian kelas kotlin.collections keluar paketnya sehingga aplikasi crash saat dibuka.
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("androidx.sqlite:sqlite:$versiSqlite")
    implementation("androidx.sqlite:sqlite-bundled:$versiSqlite")
    implementation("com.google.android.gms:play-services-auth:21.5.1")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.profileinstaller)
    "baselineProfile"(project(":baselineprofile"))
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
// Test JVM di laptop butuh libsqliteJni.so versi Linux. sqlite-bundled versi
// Android hanya membawa .so untuk HP, jadi .so diambil dari artefak desktop
// (sqlite-bundled-jvm) ke folder yang dibaca AGP saat test. Tidak di-commit.
val sqliteNativeJvm = configurations.create("sqliteNativeJvm") { isTransitive = false }

dependencies {
    sqliteNativeJvm("androidx.sqlite:sqlite-bundled-jvm:$versiSqlite")
}

interface LayananBerkas {
    @get:javax.inject.Inject val arsip: ArchiveOperations
    @get:javax.inject.Inject val berkas: FileSystemOperations
}

val ekstrakSqliteNative = tasks.register("ekstrakSqliteNative") {
    val jar: FileCollection = sqliteNativeJvm
    val tujuan = layout.projectDirectory.dir("src/test/jniLibs")
    val layanan = objects.newInstance<LayananBerkas>()
    inputs.files(jar)
    outputs.dir(tujuan)
    doLast {
        layanan.berkas.sync {
            from(layanan.arsip.zipTree(jar.singleFile)) {
                include("natives/linux_x64/libsqliteJni.so")
                eachFile { path = name }
            }
            includeEmptyDirs = false
            into(tujuan)
        }
    }
}

tasks.withType<Test>().configureEach {
    dependsOn(ekstrakSqliteNative)
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
