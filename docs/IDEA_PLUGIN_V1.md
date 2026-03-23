# OpenSkills IDEA Plugin v1

## Purpose

This document defines the minimum viable IntelliJ IDEA plugin architecture for consuming an OpenSkills Registry v1 and managing project-installed skills.

Plugin v1 is a dependency-management client, not an AI chat assistant. Its job is to:

- browse registry skills
- install versioned skill packages
- manage project-installed skills
- update project manifest files
- trigger sync after install or update

## Scope

Plugin v1 should support:

- browsing a registry
- viewing skill details and versions
- installing a skill into the current project
- updating or removing installed skills
- reading and writing `openskills.json`
- reading and writing `openskills.lock.json`
- triggering a configurable sync command

Plugin v1 should not support:

- publishing skills
- approval workflows
- cross-registry federation
- transitive dependency resolution
- interactive AI chat

## Recommended Tech Stack

- Kotlin
- IntelliJ Platform Plugin SDK
- Gradle IntelliJ Plugin
- Java `HttpClient`
- `kotlinx.serialization` or Jackson for JSON
- Java ZIP support for archive extraction

## Project Layout

Recommended plugin repository:

```text
openskills-idea-plugin/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── com/yourorg/openskills/
│   │   │       ├── registry/
│   │   │       ├── manifest/
│   │   │       ├── install/
│   │   │       ├── project/
│   │   │       ├── sync/
│   │   │       ├── settings/
│   │   │       └── ui/
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── plugin.xml
│   │       └── icons/
│   └── test/
└── README.md
```

## Recommended Packages

```text
com.yourorg.openskills
├── registry
│   ├── RegistryClient.kt
│   ├── RegistryModels.kt
│   └── RegistryService.kt
├── manifest
│   ├── ManifestModels.kt
│   ├── ManifestReader.kt
│   ├── ManifestWriter.kt
│   └── ProjectManifestService.kt
├── install
│   ├── PackageDownloader.kt
│   ├── ChecksumVerifier.kt
│   ├── ZipExtractor.kt
│   └── SkillInstaller.kt
├── project
│   ├── ProjectSkillService.kt
│   └── ProjectSkillState.kt
├── sync
│   ├── SyncCommandRunner.kt
│   └── SyncService.kt
├── settings
│   ├── PluginSettingsState.kt
│   ├── PluginSettingsService.kt
│   └── OpenSkillsConfigurable.kt
└── ui
    ├── toolwindow
    │   ├── OpenSkillsToolWindowFactory.kt
    │   └── OpenSkillsToolWindowPanel.kt
    ├── catalog
    │   └── CatalogPanel.kt
    ├── project
    │   └── ProjectPanel.kt
    └── settings
        └── SettingsPanel.kt
```

## Tool Window Layout

The plugin should expose a single `OpenSkills` tool window with three tabs:

1. `Catalog`
2. `Project`
3. `Settings`

### Catalog tab

Responsibilities:

- load registry root index
- search by id, title, tags, and description
- show skill detail
- let the user choose a version
- start install flow

### Project tab

Responsibilities:

- show installed skills from `openskills.lock.json`
- compare installed versions with registry latest
- allow update, remove, refresh, and sync

### Settings tab

Responsibilities:

- configure registry URL
- configure install path
- configure sync command
- enable or disable auto-sync
- allow or disallow prerelease versions

## Core Models

The plugin should use the Registry v1 and Project Manifest v1 documents as the source contract:

- [REGISTRY_SPEC_V1.md](REGISTRY_SPEC_V1.md)
- [PROJECT_MANIFEST_V1.md](PROJECT_MANIFEST_V1.md)

Suggested Kotlin models:

```kotlin
data class RegistryIndex(
    val registryVersion: String,
    val generatedAt: String,
    val skills: List<SkillSummary>
)

data class SkillSummary(
    val id: String,
    val title: String,
    val description: String,
    val owner: String,
    val tags: List<String>,
    val latestVersion: String,
    val latestStableVersion: String,
    val indexUrl: String
)

data class SkillVersionIndex(
    val id: String,
    val title: String,
    val description: String,
    val owner: String,
    val versions: List<SkillVersion>
)

data class SkillVersion(
    val version: String,
    val publishedAt: String,
    val breaking: Boolean,
    val manifestVersion: String,
    val packageUrl: String,
    val checksumSha256: String,
    val notesUrl: String?
)
```

## Service Responsibilities

### `RegistryClient`

- fetch root index
- fetch per-skill index
- resolve relative URLs against the configured registry base URL

### `RegistryService`

- search and filter registry entries
- choose default install version
- compare installed and latest versions

### `ProjectManifestService`

- locate project manifest files
- create missing `openskills.json`
- read and write both manifest and lock files

### `SkillInstaller`

- download package zip
- verify SHA-256 checksum
- extract into `.agent/skills/<skill-id>`
- update `openskills.json`
- update `openskills.lock.json`

### `SyncService`

- run a configurable sync command
- surface output and failures to the user

## Install Flow

Recommended install sequence:

1. User selects a skill and version in `Catalog`.
2. Plugin downloads the package zip.
3. Plugin verifies checksum.
4. Plugin extracts the package into the project install path.
5. Plugin creates or updates `openskills.json`.
6. Plugin creates or updates `openskills.lock.json`.
7. Plugin runs sync if enabled.
8. Plugin refreshes `Project` tab state.

## Update Flow

Recommended update sequence:

1. Read current installed version from `openskills.lock.json`.
2. Read latest available version from the registry.
3. Confirm the target version with the user.
4. Download and replace the package contents.
5. Rewrite `openskills.lock.json`.
6. Re-run sync.

## Remove Flow

Recommended remove sequence:

1. Remove the skill from `openskills.json`.
2. Delete the local installed skill directory.
3. Rewrite `openskills.lock.json`.
4. Re-run sync.

## Minimal `plugin.xml`

The plugin needs:

- tool window registration
- persistent settings
- optional project service registrations

Example:

```xml
<idea-plugin>
  <id>com.yourorg.openskills</id>
  <name>OpenSkills</name>
  <vendor>Your Org</vendor>

  <extensions defaultExtensionNs="com.intellij">
    <toolWindow
        id="OpenSkills"
        anchor="right"
        factoryClass="com.yourorg.openskills.ui.toolwindow.OpenSkillsToolWindowFactory" />

    <applicationConfigurable
        id="openskills.settings"
        displayName="OpenSkills"
        instance="com.yourorg.openskills.settings.OpenSkillsConfigurable" />
  </extensions>
</idea-plugin>
```

## Minimal `build.gradle.kts`

Example skeleton:

```kotlin
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
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        bundledPlugin("com.intellij.java")
    }
}

kotlin {
    jvmToolchain(21)
}
```

## First Development Milestone

The first shippable milestone should do only this:

1. open the `OpenSkills` tool window
2. save a registry URL in settings
3. fetch `registry/index.json`
4. display the list of skills in `Catalog`
5. show per-skill detail from the per-skill index

That milestone is enough to validate:

- registry format
- HTTP client behavior
- UI layout
- state management approach

## Recommended Next Steps

1. Create a separate plugin repository using this structure.
2. Implement the first milestone only.
3. Test against the static registry generated by this repository.
4. Add install flow after registry browsing is stable.

This repository also includes a copyable starter at [templates/idea-plugin-starter](../templates/idea-plugin-starter).

