import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "1.9.0"
}

kotlin {


    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting


        iosX64Main.dependencies {
        }

        iosArm64Main.dependencies {
        }

        iosSimulatorArm64Main.dependencies {
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            // Material 3
            implementation(compose.material3)


            // DateTime
            implementation(libs.kotlinx.datetime)

            // JSON Serialization
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

            // icon extended
            implementation("org.jetbrains.compose.material:material-icons-core:1.6.11")
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            // PDF generation for desktop
            implementation("com.itextpdf:itextpdf:5.5.13.3")
        }
    }
}



compose.desktop {
    application {
        mainClass = "gy.roach.json.medminder.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "gy.roach.json.medminder"
            packageVersion = "1.0.0"

            linux {
                iconFile.set(project.file("desktopAppIcons/medminder.png"))
            }

            macOS {
                iconFile.set(project.file("desktopAppIcons/medminder.icns"))
                bundleID = "gy.roach.json.medminder.desktopApp"
            }
        }
    }
}
