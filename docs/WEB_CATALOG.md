# Web Catalog

## Purpose

The web catalog gives teammates a human-friendly view of the skills in this repository.

It is intentionally simple:

- Static files only
- No database
- No backend service
- Generated from repository metadata

## Does the site auto-update from `skills/`?

- **Browser / static JS:** No. The page loads **generated** [catalog/data/skills.js](../catalog/data/skills.js). It does not read the filesystem or Git at runtime.
- **GitHub Pages:** Yes, for the **deployed** site. The [deploy workflow](../.github/workflows/deploy-pages.yml) runs `export-site.ps1`, which always runs `generate-catalog.ps1` before upload, so each deploy matches the `skills/` tree on `main` at that commit.
- **Local preview:** After you add or edit skills, run `scripts/generate-catalog.ps1`, or keep [scripts/watch-catalog.ps1](../scripts/watch-catalog.ps1) running so `skills.js` / `skills.json` regenerate on save; then refresh the browser.

Committed `catalog/data/*` should stay in sync for CI (`validate-skills` checks `git diff`).

## Source Of Truth

Each skill folder should contain:

- `SKILL.md` for agent instructions
- `meta.json` for catalog metadata

The catalog data file is generated from the `meta.json` files and should not be edited manually.

## OpenSkills commands on the site

- [catalog/catalog-config.json](catalog/catalog-config.json) can **override** generated values; by default `scripts/generate-catalog.ps1` **auto-fills** clone URLs and source links from **`git remote get-url origin`** and the current **HEAD** (branch name, or commit SHA when detached—e.g. GitHub Actions).
- Run `scripts/generate-catalog.ps1` so [catalog/data/skills.js](catalog/data/skills.js) embeds `window.CATALOG_CONFIG` next to `window.SKILLS_DATA`.
- The skill detail panel shows install + `sync`, optional `--universal`, and `npx openskills read <skill-name>` with copy buttons.
- **Repository paths** in the UI stay **repo-relative** (`skills/<id>/SKILL.md`). When generation runs inside a clone, **GitHub** and **GitLab** remotes also get `skill_web_url` / `meta_web_url` for “open in browser” links.
- Set **`disable_git_origin`: true** in `catalog-config.json` to turn off auto-detection and use only defaults + manual overrides.

### `catalog-config.json` fields (optional overrides)

| Key | Role |
|-----|------|
| `skills_repo_ssh` / `skills_repo_https` | OpenSkills install examples |
| `repo_web_base` | e.g. `https://github.com/org/repo` — used to build source links |
| `source_ref` | Branch or tag name for URLs (override when you want links to always show `main` while building from a SHA) |
| `config_note` | Shown above OpenSkills commands |
| `disable_git_origin` | If `true`, skip reading `git` entirely |

## Current Architecture

- [catalog/catalog-config.json](catalog/catalog-config.json): catalog-level settings (skills repo URLs for command snippets). **Save as UTF-8** so PowerShell `generate-catalog.ps1` parses Chinese `config_note` correctly on Windows.
- [catalog/index.html](../catalog/index.html): page shell
- [catalog/favicon.svg](../catalog/favicon.svg): site icon
- [catalog/styles.css](../catalog/styles.css): visual styling
- [catalog/app.js](../catalog/app.js): client-side rendering and filtering
- [catalog/data/skills.js](../catalog/data/skills.js): generated skill dataset
- [scripts/generate-catalog.ps1](../scripts/generate-catalog.ps1): generation script
- [scripts/validate-skills.py](../scripts/validate-skills.py): layout and metadata checks (also run in CI)
- [scripts/export-site.ps1](../scripts/export-site.ps1): static deploy artifact export

## Metadata Shape

Each `meta.json` should contain:

- `name`
- `title`
- `description`
- `tags`
- `owner`
- `status`
- `version` — SemVer string for this skill (see [GOVERNANCE.md](GOVERNANCE.md#per-skill-version-in-metajson); validated by `validate-skills.py`)
- `summary`
- `use_cases`
- `install_hint`

You can extend the schema later, but keep it lightweight at first.

## Recommended Workflow

1. Add or update a skill's `SKILL.md`.
2. Add or update the skill's `meta.json`.
3. If OpenSkills URLs changed, update `catalog/catalog-config.json`.
4. Run the catalog generator script.
5. Open the catalog page and confirm the skill renders correctly.
6. Commit the skill and generated catalog output together.

For publish steps, see [DEPLOYMENT.md](DEPLOYMENT.md).

## Future Extensions

- Tag filters by domain or team
- Owner pages
- Version or release history
- Example project mappings
