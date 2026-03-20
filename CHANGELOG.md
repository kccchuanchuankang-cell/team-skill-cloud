# Changelog

All notable changes to this registry are documented here. The project uses [Semantic Versioning](https://semver.org/) for **repository releases** (see [docs/GOVERNANCE.md](docs/GOVERNANCE.md)).

## [Unreleased]

### Added

- `generate-catalog.ps1` auto-detects `git` origin (GitHub/GitLab) for clone URLs, `repo_web_base`, `source_ref`, and per-skill `skill_web_url` / `meta_web_url`; optional `disable_git_origin` in `catalog-config.json`
- `scripts/watch-catalog.ps1` to regenerate catalog data when `skills/` changes during local dev
- [docs/VERSIONING_AND_OPENSKILLS.md](docs/VERSIONING_AND_OPENSKILLS.md) and `scripts/install-skills-at-ref.ps1` for installing from a Git tag/branch when OpenSkills URL install tracks only default branch
- Skill layout and `meta.json` validation (`scripts/validate-skills.py`) and CI workflow
- Pull request template for skill changes
- `CODEOWNERS` placeholder for team assignment
- Catalog `skill_path` / `meta_path` as repository-relative paths for portable links

### Changed

- Catalog static site UI strings are **Simplified Chinese**; skill titles/descriptions still come from `meta.json`
- `generate-catalog.ps1` reads `catalog-config.json` and each `meta.json` as **UTF-8** for non-ASCII text on Windows
- Example `SKILL.md` reference links use repo-relative paths instead of machine-specific paths
