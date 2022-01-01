import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm") version "1.5.31"
    id("org.jetbrains.compose") version "1.0.0"
    id("org.openjfx.javafxplugin") version "0.0.10"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "se.tristanfarkas"
version = "0.1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("dev.stalla:stalla:1.1.0")
    implementation("io.ktor:ktor-client-core:1.6.7")
    implementation("io.ktor:ktor-client-cio:1.6.7")
    implementation("io.ktor:ktor-client-serialization:1.6.7")
    implementation("org.apache.commons:commons-text:1.9")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb)
            packageName = "lyssna"
            packageVersion = "0.1.0"
            version = "0.1.0"
            description = "A simple podcast application"
            copyright = "© 2022 Tristan Farkas"
            vendor = "Tristan Farkas"
            licenseFile.set(project.file("LICENSE.md"))
            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}

javafx {
    version = "17"
    modules = listOf("javafx.media")
}
