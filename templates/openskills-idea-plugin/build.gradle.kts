plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "com.yourorg"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    instrumentCode = false
    buildSearchableOptions = false
}

tasks {
    register<Delete>("resetSandbox") {
        group = "intellij platform"
        description = "Deletes the IntelliJ sandbox so the next run starts clean."
        delete(layout.buildDirectory.dir("idea-sandbox"))
    }

    prepareSandbox {
        doLast {
            val disabledPluginsFile = layout.buildDirectory.file("idea-sandbox/config/disabled_plugins.txt").get().asFile
            disabledPluginsFile.parentFile.mkdirs()
            disabledPluginsFile.writeText(
                listOf(
                    "com.intellij.gradle",
                    "org.jetbrains.idea.maven"
                ).joinToString(System.lineSeparator()) + System.lineSeparator()
            )
        }
    }
}

kotlin {
    jvmToolchain(17)
}
