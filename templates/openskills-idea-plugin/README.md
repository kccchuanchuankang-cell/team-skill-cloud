# OpenSkills IDEA Plugin

Runnable starter for an OpenSkills-aware IntelliJ IDEA plugin.

## Runtime baseline

This starter is tuned for **JDK 17** and an IntelliJ platform target of **2024.3** so it is easier to try on machines that do not yet have JDK 21.

## Build defaults

For this starter, IntelliJ code instrumentation is disabled with the official Gradle plugin setting because the current prototype does not rely on instrumentation-dependent features, and this avoids JVM-vendor-specific issues during early local runs.

The starter also writes `com.intellij.gradle` and `org.jetbrains.idea.maven` into the sandbox disabled-plugins file so OpenSkills testing is not polluted by unrelated sandbox startup errors.

If those plugins still appear in the sandbox logs, run the Gradle task `resetSandbox` once and then launch `runIde` again so the sandbox starts clean.

## Current product direction

The plugin is being shaped as a graphical front end for the official OpenSkills CLI.

That means the best long-term UX is:

- `Installed` for visible and installed skills
- `Install` for `openskills install <source> [flags]`
- `Settings` for CLI launcher and defaults
- `Resolution` for priority-order visualization

## Current alignment with OpenSkills CLI

The starter currently supports these OpenSkills actions:

- `Installed > Refresh` -> `openskills list`
- `Installed > Read` -> `openskills read <name>`
- `Installed > Update` -> `openskills update <name>`
- `Installed > Remove` -> `openskills remove <name>`
- `Installed > Sync` -> `openskills sync -y`
- `Install > Install` -> choose an install location, then run `openskills install <source> --universal [--global] --yes`
  - success now auto-refreshes `Installed` and `Resolution`
  - optional automatic `openskills sync -y` now runs when enabled in Settings
  - success feedback now includes source, mode, and CLI output

The tool window now includes:

- `Installed`
- `Install`
- `Resolution`
- `Settings`

`Installed` now also includes `Update All` to run `openskills update` for every installed skill.

## Goal

Use this starter to bootstrap a separate `openskills-idea-plugin` repository.






