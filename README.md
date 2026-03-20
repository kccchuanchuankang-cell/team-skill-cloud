# skills-cloud

Team-owned skills registry for AI coding agents.

This repository is the source of truth for reusable internal skills. Each project installs only the skills it needs, then syncs its local `AGENTS.md` so the agent can discover and load those skills on demand.

The repository also includes a lightweight web catalog so teammates can browse available skills without reading the raw folder structure.

## Goals

- Centralize reusable team workflows in one repository
- Let each project choose a small, relevant set of skills
- Keep agent context lean through progressive disclosure
- Make skill changes reviewable, versioned, and safe to roll out

## Repository Layout

```text
skills-cloud/
├── AGENTS.md
├── README.md
├── catalog/
│   ├── app.js
│   ├── favicon.svg
│   ├── index.html
│   ├── styles.css
│   └── data/
├── docs/
│   ├── DEPLOYMENT.md
│   ├── GOVERNANCE.md
│   ├── IMPLEMENTATION_PLAN.md
│   ├── PROJECT_USAGE.md
│   ├── VERSIONING_AND_OPENSKILLS.md
│   └── WEB_CATALOG.md
├── scripts/
│   ├── export-site.ps1
│   ├── generate-catalog.ps1
│   ├── install-skills-at-ref.ps1
│   ├── new-skill.ps1
│   ├── validate-skills.py
│   └── watch-catalog.ps1
├── .github/
│   ├── workflows/
│   ├── pull_request_template.md
│   └── CODEOWNERS
├── skills/
│   ├── backend-api/
│   ├── frontend-react/
│   └── release-triage/
└── templates/
    └── skill-template/
```

## Skill Design Rules

- One skill per folder
- Keep `SKILL.md` short and procedural
- Put detailed references in `references/`
- Put deterministic helpers in `scripts/`
- Avoid generic knowledge dumps

## Recommended Project Workflow

1. Pick the skills a project actually needs.
2. Install them from this repository with OpenSkills.
3. Run `npx openskills sync` in the project repository.
4. Review the generated `AGENTS.md` changes in a pull request.
5. Upgrade skills intentionally by version or tag, not by surprise.

See [docs/PROJECT_USAGE.md](docs/PROJECT_USAGE.md) for the project-side workflow and [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) for the rollout plan. [CONTRIBUTING.md](CONTRIBUTING.md) covers validation and catalog regeneration before a PR.

**Pinning / old versions:** OpenSkills’ `install <git-url>` tracks the default branch only. To install a **tagged** snapshot, use [scripts/install-skills-at-ref.ps1](scripts/install-skills-at-ref.ps1) or the flow in [docs/VERSIONING_AND_OPENSKILLS.md](docs/VERSIONING_AND_OPENSKILLS.md).

## Web Catalog

The catalog is a static site in [catalog/index.html](catalog/index.html). It reads generated data from [catalog/data/skills.js](catalog/data/skills.js), which is built from each skill's `meta.json`.

Regenerate the catalog data after changing skill metadata or [catalog/catalog-config.json](catalog/catalog-config.json). Generation **auto-reads `git remote origin` and HEAD** when `.git` exists (override or disable via `catalog-config.json`; see [docs/WEB_CATALOG.md](docs/WEB_CATALOG.md)):

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\generate-catalog.ps1
```

Optional while editing skills locally: `powershell -ExecutionPolicy Bypass -File .\scripts\watch-catalog.ps1` then refresh the browser—see [docs/WEB_CATALOG.md](docs/WEB_CATALOG.md#does-the-site-auto-update-from-skills).

Open [catalog/index.html](catalog/index.html) in a browser, or serve the repository directory with any static file server.

For deployment, see [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

## Suggested Governance Model

- `main` contains reviewed, releasable skills
- Breaking skill changes require migration notes
- Projects should prefer tagged releases
- Every skill should have an owner
- Every skill should state clear trigger conditions

## Initial Skill Set

- `backend-api`: API implementation and contract-aware backend work
- `frontend-react`: React UI work aligned with team frontend conventions
- `release-triage`: Release checks, regression scanning, and issue triage

## Next Step

Start by reviewing the sample skills in [skills](skills), then adapt the references and scripts to your team's actual standards and tooling. After that, update the `meta.json` files and regenerate the catalog so the web view stays in sync.
# team-skill-cloud
