# OpenSkills Registry v1

## Purpose

This document defines the minimum viable registry format for publishing versioned skills in a way that can be consumed by:

- a lightweight CLI
- an IntelliJ IDEA plugin
- future automation around install, update, and sync

Registry v1 is intentionally simple:

- static files only
- no database
- no server-side search engine
- versioned skill packages
- JSON indexes readable from GitHub Pages or an internal static host

## Design Goals

- Make skills installable by version, not only by Git default branch
- Keep the registry easy to host on GitHub Pages or intranet static hosting
- Allow future clients to browse, install, update, and remove skills deterministically
- Reuse the existing `skills/` source repository as the source of truth

## Non-Goals

- Full dependency resolution between skills
- Multi-tenant auth and permissions
- Online skill editing
- Database-backed analytics
- Runtime package execution on the server

## Model

There are two layers:

1. Source repository
2. Published registry

The source repository stores human-authored skill content under `skills/<skill-id>/`.

The published registry is a generated static artifact containing:

- registry indexes
- per-skill version indexes
- downloadable skill packages
- checksums

## Proposed Output Layout

```text
registry/
├── index.json
├── skills/
│   ├── backend-api/
│   │   ├── index.json
│   │   └── versions/
│   │       ├── 0.1.0.zip
│   │       └── 0.1.0.sha256
│   └── frontend-react/
│       ├── index.json
│       └── versions/
└── packages/
```

`packages/` is optional in v1 if packages are already stored under each skill's `versions/` directory.

## Source Skill Requirements

Each source skill is expected to have:

- `SKILL.md`
- `meta.json`

Optional:

- `references/`
- `scripts/`
- `agents/`
- other files needed by the skill package

`meta.json` remains the source metadata file in the authoring repository. The registry generator maps selected fields into published indexes.

## Registry Root Index

Path:

- `/registry/index.json`

Purpose:

- Entry point for all registry clients
- Returns the list of available skills and pointers to each skill's version index

### Required fields

- `registry_version`: string, currently `"1"`
- `generated_at`: ISO-8601 UTC timestamp
- `skills`: array of skill summaries

### Skill summary object

- `id`: stable skill id
- `title`: display title
- `description`: short description
- `owner`: team or owner slug
- `tags`: array of tag slugs
- `latest_version`: latest published version
- `latest_stable_version`: latest stable published version
- `index_url`: relative or absolute URL to the per-skill index

### Example

```json
{
  "registry_version": "1",
  "generated_at": "2026-03-23T12:00:00Z",
  "skills": [
    {
      "id": "backend-api",
      "title": "Backend API",
      "description": "Backend API implementation and contract-sensitive server work.",
      "owner": "platform-team",
      "tags": ["backend", "api", "validation"],
      "latest_version": "0.1.0",
      "latest_stable_version": "0.1.0",
      "index_url": "/registry/skills/backend-api/index.json"
    }
  ]
}
```

## Per-Skill Index

Path:

- `/registry/skills/<skill-id>/index.json`

Purpose:

- Returns version history and package pointers for one skill

### Required fields

- `id`
- `title`
- `description`
- `owner`
- `versions`

### Version object

- `version`: SemVer string
- `published_at`: ISO-8601 UTC timestamp
- `breaking`: boolean
- `manifest_version`: string, currently `"1"`
- `package_url`: downloadable zip URL
- `checksum_sha256`: SHA-256 of the zip
- `notes_url`: optional URL to changelog or release notes

### Example

```json
{
  "id": "backend-api",
  "title": "Backend API",
  "description": "Backend API implementation and contract-sensitive server work.",
  "owner": "platform-team",
  "versions": [
    {
      "version": "0.1.0",
      "published_at": "2026-03-23T12:00:00Z",
      "breaking": false,
      "manifest_version": "1",
      "package_url": "/registry/skills/backend-api/versions/0.1.0.zip",
      "checksum_sha256": "abc123",
      "notes_url": "/CHANGELOG.md"
    }
  ]
}
```

## Skill Package Format

Registry v1 uses zip packages.

Package naming:

- `<skill-id>-<version>.zip` is recommended for downloaded filenames

Published path:

- `/registry/skills/<skill-id>/versions/<version>.zip`

Package contents:

```text
<skill-id>/
├── SKILL.md
├── meta.json
├── references/
├── scripts/
└── agents/
```

The package should contain the same files the client needs to install locally. Avoid packaging repository-only files that are not part of the skill itself.

## Versioning Rules

Registry v1 distinguishes:

- repository releases
- per-skill versions in `meta.json`

Per-skill version:

- stored in `skills/<skill-id>/meta.json`
- used in registry indexes and package filenames

Repository release:

- optional higher-level release of the whole source repository
- still useful for changelog and Git history

If a skill changes behavior, triggers, or expected usage, bump its per-skill version before publishing.

## Publish Flow

Recommended publish sequence:

1. Update `skills/<skill-id>/`.
2. Validate the repository.
3. Bump `meta.json` version if behavior changed.
4. Generate catalog output.
5. Build registry output.
6. Package changed skills.
7. Generate root and per-skill indexes.
8. Publish the resulting static registry.

## Client Capabilities Expected In v1

Any Registry v1 client should be able to:

- read `index.json`
- browse and search skill summaries locally
- resolve a skill id to its per-skill index
- choose a version
- download the zip package
- verify checksum
- install the package into a local project path

The registry itself does not prescribe UI behavior.

## Suggested Next Implementation Steps

1. Add a build script such as `scripts/build-registry.ps1`.
2. Add a packaging script such as `scripts/package-skill.ps1`.
3. Add a registry output directory such as `registry/`.
4. Decide whether package URLs should be relative paths or full absolute URLs in generated output.
5. Add checksum generation and validation to CI.
