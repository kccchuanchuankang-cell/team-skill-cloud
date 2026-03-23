# OpenSkills Project Manifest v1

## Purpose

This document defines the minimum project-side files needed to manage installed skills like versioned dependencies.

The goal is to give future clients such as a CLI or IntelliJ IDEA plugin a stable way to answer:

- Which skills does this project want?
- Which exact versions are installed?
- Which registry did they come from?
- Where are they installed locally?

Registry v1 uses two files:

- `openskills.json`
- `openskills.lock.json`

## File 1: `openskills.json`

Purpose:

- Human-edited project manifest
- Declares desired skills and version constraints

Recommended location:

- repository root

### Required fields

- `manifest_version`: string, currently `"1"`
- `registry`: base registry URL
- `install_path`: local skill installation directory
- `skills`: array of desired skills

### Skill entry fields

- `id`: stable skill id
- `version`: version constraint string

Optional:

- `source`: override registry source if a skill comes from a different registry
- `enabled`: boolean for temporary disablement

### Example

```json
{
  "manifest_version": "1",
  "registry": "https://skills.example.com/registry",
  "install_path": ".agent/skills",
  "skills": [
    {
      "id": "backend-api",
      "version": "^0.1.0"
    },
    {
      "id": "release-triage",
      "version": "0.1.0"
    }
  ]
}
```

## File 2: `openskills.lock.json`

Purpose:

- Machine-generated lock file
- Records the exact resolved package versions and checksums

Recommended location:

- repository root

### Required fields

- `lock_version`: string, currently `"1"`
- `registry`: base registry URL used during resolution
- `install_path`: local skill installation directory
- `generated_at`: ISO-8601 UTC timestamp
- `resolved`: array of installed skill records

### Resolved skill fields

- `id`
- `version`
- `package_url`
- `checksum_sha256`
- `installed_path`

Optional:

- `source_index_url`
- `published_at`

### Example

```json
{
  "lock_version": "1",
  "registry": "https://skills.example.com/registry",
  "install_path": ".agent/skills",
  "generated_at": "2026-03-23T12:00:00Z",
  "resolved": [
    {
      "id": "backend-api",
      "version": "0.1.0",
      "package_url": "https://skills.example.com/registry/skills/backend-api/versions/0.1.0.zip",
      "checksum_sha256": "abc123",
      "installed_path": ".agent/skills/backend-api",
      "source_index_url": "https://skills.example.com/registry/skills/backend-api/index.json"
    }
  ]
}
```

## Relationship To Existing OpenSkills Usage

Today the repository primarily documents `npx openskills install ...` and `npx openskills sync`.

Manifest v1 is a forward-looking format intended for:

- a future registry-aware CLI wrapper
- an IntelliJ IDEA plugin
- deterministic install and update flows

It does not replace current Git-based installs immediately. It creates a stable project contract so clients can evolve beyond ad hoc commands.

## Recommended Install Lifecycle

1. User selects skills from the registry.
2. Client updates `openskills.json`.
3. Client resolves versions from the registry.
4. Client downloads packages and installs them to `install_path`.
5. Client writes `openskills.lock.json`.
6. Client runs skill sync for the local project.

## Recommended Update Lifecycle

1. Read `openskills.json`.
2. Compare constraints to latest registry versions.
3. Propose updates.
4. Download and replace installed skill packages.
5. Rewrite `openskills.lock.json`.
6. Re-run sync.

## Recommended Remove Lifecycle

1. Remove the skill from `openskills.json`.
2. Remove the installed skill directory.
3. Rewrite `openskills.lock.json`.
4. Re-run sync.

## Installation Path

Registry v1 recommends:

- `.agent/skills`

Rationale:

- explicit project-local installation
- good fit for multi-agent environments
- easy for IDE plugins to discover

## Constraints And Simplifications In v1

- No transitive skill dependencies
- No peer dependency model
- No platform-specific package variants
- No signed packages yet
- Version constraints are stored as strings; exact resolver behavior can be defined by the future client implementation

## Suggested Next Implementation Steps

1. Document whether `openskills.json` should be committed by default.
2. Decide whether `openskills.lock.json` is always committed or optional in early rollout.
3. Build a registry-aware install script or CLI wrapper that reads these files.
4. Teach the future IDEA plugin to read and write both files.

For the plugin-side architecture that consumes these files, see [IDEA_PLUGIN_V1.md](IDEA_PLUGIN_V1.md).
