import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec

plugins {
    id("com.android.application") version "9.4.1"
}

val gradleManifest = layout.buildDirectory.file("gradle-manifest/AndroidManifest.xml").get().asFile
gradleManifest.parentFile.mkdirs()
file("java/AndroidManifest.xml").readText()
    .replaceFirst(Regex("""\s+package="[^"]+""""), "")
    .replaceFirst(Regex("""(?s)\s*<uses-sdk[^>]*/>"""), "")
    .let { if (!gradleManifest.exists() || gradleManifest.readText() != it) gradleManifest.writeText(it) }

val keysDir = file(providers.gradleProperty("penguin.keysDir").getOrElse("../priv-keys"))
val keyName = providers.gradleProperty("penguin.keyName").getOrElse("shared")
val pk8 = keysDir.resolve("$keyName.pk8")
val x509 = keysDir.resolve("$keyName.x509.pem")
val signingStore = layout.buildDirectory.file("signing/$keyName.p12").get().asFile
val signingStorePassword = "penguin-build-local"

val hasReleaseKey = pk8.isFile && x509.isFile
if (hasReleaseKey) {
    val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(pk8.readBytes()))
    val cert = x509.inputStream().use { CertificateFactory.getInstance("X.509").generateCertificate(it) }
    val store = KeyStore.getInstance("PKCS12").apply { load(null, null) }
    store.setKeyEntry(keyName, key, signingStorePassword.toCharArray(), arrayOf(cert))
    signingStore.parentFile.mkdirs()
    signingStore.outputStream().use { store.store(it, signingStorePassword.toCharArray()) }
}

android {
    namespace = "com.android.inputmethod.latin"
    compileSdk = 36
    ndkVersion = "29.0.14206865"

    defaultConfig {
        applicationId = "com.penguin.keyboard"
        minSdk = 30
        targetSdk = 36
        versionCode = 260930
        versionName = "1.6.1"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("penguin") {
                storeFile = signingStore
                storePassword = signingStorePassword
                keyAlias = keyName
                keyPassword = signingStorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "java/proguard.flags")
            signingConfig = signingConfigs.findByName("penguin") ?: signingConfigs.getByName("debug")
        }
        debug {
            signingConfig = signingConfigs.findByName("penguin") ?: signingConfigs.getByName("debug")
        }
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile(gradleManifest)
            java.directories.run { clear(); add("common/src"); add("java/src") }
            res.directories.run { clear(); add("java/res") }
            assets.directories.run { clear(); add("java/assets") }
        }
    }

    externalNativeBuild {
        ndkBuild {
            path = file("native/jni/AndroidNdk.mk")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    androidResources {
        noCompress += "dict"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.BELLSOFT
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-unchecked", "-Xmaxerrs", "1000"))
}

dependencies {
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.dynamicanimation:dynamicanimation:1.1.0")
    implementation("com.google.android.material:material:1.14.0")
}
