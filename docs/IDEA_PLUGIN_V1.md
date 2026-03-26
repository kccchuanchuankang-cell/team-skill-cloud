# OpenSkills IDEA Plugin v1

## Purpose

This document defines the minimum viable IntelliJ IDEA plugin architecture for consuming OpenSkills project workflows inside IDEA.

Plugin v1 is a dependency-management and project-operations client, not an AI chat assistant. Its job is to:

- inspect project-visible skills
- visualize skill resolution priority
- run OpenSkills CLI operations from IDEA
- provide a stable place to configure the OpenSkills launcher
- make install, update, remove, and sync easier than memorizing terminal flags

## Product direction

The plugin should be treated as a graphical front end for the OpenSkills CLI.

That means:

- plugin actions should prefer calling the real OpenSkills commands
- plugin UI should explain scope, priority, and resolved path better than the terminal can
- plugin state should stay aligned with OpenSkills installation behavior instead of inventing a separate package manager model

The current OpenSkills command surface that shapes the plugin is:

- `openskills install <source> [options]`
- `openskills sync [-y] [-o <path>]`
- `openskills list`
- `openskills read <name>`
- `openskills update [name...]`
- `openskills remove <name>`
- `openskills manage`

The current OpenSkills priority order that shapes the plugin is:

1. `./.agent/skills/`
2. `~/.agent/skills/`
3. `./.claude/skills/`
4. `~/.claude/skills/`

## Scope

Plugin v1 should support:

- viewing installed and visible project skills
- reading and writing `openskills.json`
- reading and writing `openskills.lock.json`
- running `openskills list`
- running `openskills read`
- running `openskills install`
- running `openskills update`
- running `openskills remove`
- running `openskills sync`
- configuring the CLI launcher and related defaults
- visualizing where each visible skill resolves from

Plugin v1 should not support yet:

- publishing skills
- approval workflows
- cross-registry federation
- transitive dependency resolution
- interactive AI chat
- custom installation semantics that diverge from OpenSkills CLI

## Recommended Tech Stack

- Kotlin
- IntelliJ Platform Plugin SDK
- Gradle IntelliJ Plugin
- Java `HttpClient`
- Jackson for JSON
- background tasks for all external command execution

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
│   │   │       ├── cli/
│   │   │       ├── manifest/
│   │   │       ├── install/
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
├── cli
│   └── OpenSkillsCliAdapter.kt
├── manifest
│   ├── ManifestModels.kt
│   └── ProjectManifestService.kt
├── install
│   └── SkillInstaller.kt
├── settings
│   ├── PluginSettingsState.kt
│   ├── PluginSettingsService.kt
│   └── OpenSkillsConfigurable.kt
└── ui
    ├── install
    │   └── InstallPanel.kt
    ├── project
    │   └── ProjectPanel.kt
    ├── resolution
    │   └── ResolutionPanel.kt
    └── toolwindow
        ├── OpenSkillsToolWindowFactory.kt
        └── OpenSkillsToolWindowPanel.kt
```

## Tool Window Layout

The recommended tool window layout is:

1. `Installed`
2. `Install`
3. `Settings`
4. `Resolution`

The current starter now exposes `Installed`, `Install`, `Resolution`, and `Settings`, with CLI-backed workflows as the primary path.

### Installed tab

Responsibilities:

- show skills currently visible to the project
- show installed project skills from `openskills.lock.json`
- run `openskills list`
- allow `Read`, `Update`, `Remove`, `Refresh`, and `Sync`
- explain whether a skill is coming from project scope or global scope

Recommended columns:

- skill name
- resolved path
- scope: project or global
- mode: universal or claude
- priority rank
- status

### Install tab

Responsibilities:

- collect a skill source such as GitHub, local path, or private repo URL
- let the user choose install mode
- run `openskills install <source> [flags]`

Recommended install modes:

- `Project Universal`
- `Global Universal`

Suggested flag mapping:

- `Project Universal` -> `--universal`
- `Global Universal` -> `--global --universal`

### Settings tab

Responsibilities:

- configure CLI launcher
- configure default install mode
- configure sync output path
- enable or disable auto-sync
- allow or disallow prerelease versions for future use

On Windows, the plugin should support a launcher like:

- `C:\Program Files\nodejs\npx.cmd`

### Resolution tab

Responsibilities:

- visualize OpenSkills priority order
- show which directory won for each visible skill
- highlight when a lower-priority skill is shadowed by a higher-priority one

This is one of the main UX wins of the plugin because the terminal output is weaker at showing precedence clearly.

## Command mapping

Recommended mapping between UI actions and OpenSkills CLI:

- `Installed > Refresh` -> `openskills list`
- `Installed > Read` -> `openskills read <name>`
- `Installed > Update` -> `openskills update <name>`
- `Installed > Update All` -> `openskills update`
- `Installed > Remove` -> `openskills remove <name>`
- `Installed > Sync` -> `openskills sync`
- `Install > Install` -> `openskills install <source> --universal [--global] --yes`

`manage` can remain out of scope for v1 because it is interactive and less IDE-friendly than direct `remove <name>`.

## Current starter alignment

The current starter already aligns these actions with the OpenSkills CLI:

- `Installed > Refresh` -> `openskills list`
- `Installed > Read` -> `openskills read <name>`
- `Installed > Update` -> `openskills update <name>`
- `Installed > Remove` -> `openskills remove <name>`
- `Installed > Update All` -> `openskills update`
- `Installed > Sync` -> `openskills sync`

The starter also includes a configurable CLI launcher so sandboxed IDEA runs can target `npx.cmd` directly.

## Manifest and install services

The starter still keeps manifest and install helpers because they will be needed for future install, update, and remove flows:

- `ProjectManifestService` locates and updates `openskills.json` and `openskills.lock.json`
- `SkillInstaller` remains a prototype helper while direct `openskills install` UI wiring is finalized

## Minimal `plugin.xml`

The plugin needs:

- tool window registration
- persistent settings
- application configurable registration

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

intellijPlatform {
    instrumentCode = false
    buildSearchableOptions = false
}

kotlin {
    jvmToolchain(17)
}
```

## First development milestone

The first shippable milestone should do only this:

1. open the `OpenSkills` tool window
2. save a CLI launcher in settings
3. run `openskills list`
4. show installed project skills from the lock file
5. run `openskills sync`

That milestone is enough to validate:

- OpenSkills CLI integration inside IDEA
- background task execution
- settings persistence
- project-side manifest inspection

## Recommended next steps

1. Add an `Install` tab that maps directly to `openskills install <source> [flags]`.
2. Add a `Resolution` tab that visualizes the official OpenSkills priority order.
3. Add `Update All` to the installed-skills view.
4. Only bring registry browsing back if it becomes a thin enhancement over the official install workflow.

This repository also includes a copyable starter at [templates/openskills-idea-plugin](../templates/openskills-idea-plugin).



