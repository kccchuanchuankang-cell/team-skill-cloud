# OpenSkills IDEA Plugin Template

Starter template for a registry-aware IntelliJ IDEA plugin skeleton.

## Goal

Use this template to bootstrap a separate `openskills-idea-plugin` repository.

It includes:

- Gradle IntelliJ setup
- minimal `plugin.xml`
- `OpenSkills` tool window skeleton
- registry client and settings stubs
- a working catalog panel prototype
- an install flow skeleton that downloads packages and updates `openskills.json` / `openskills.lock.json`
- a project panel that reads installed skills from the lock file

## Suggested next steps

1. Copy this folder into a new repository.
2. Rename package `com.yourorg.openskills`.
3. Update plugin id, vendor, and project name.
4. Replace the example registry URL with your actual registry.
5. Add sync execution after install.
6. Build the Settings tab to persist registry URL and defaults.
7. Extend the Project tab with update and remove actions.
