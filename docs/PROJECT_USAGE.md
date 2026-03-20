# Project Usage

## Consumption Model

Each product repository installs only the skills it needs from `skills-cloud`.

Recommended defaults:

- Install locally in the consuming repository
- Use `.agent/skills/` for multi-agent setups
- Pin to tags for stable projects

To browse available skills before installation, use the repository catalog described in [docs/WEB_CATALOG.md](WEB_CATALOG.md).

## Installing OpenSkills

You need **[Node.js](https://nodejs.org/) 20.6 or newer** — the published `openskills` package declares `engines.node: '>=20.6.0'`. Verify:

```powershell
node -v
```

Current **LTS** releases from nodejs.org (e.g. 20.x, 22.x) satisfy this; **Node 18** is below the requirement and may fail.

**Option A — no global install (recommended in docs below):** run commands as `npx openskills …`. npm downloads the CLI for that run; nothing to install globally.

**Option B — global CLI** (then you can type `openskills` instead of `npx openskills`):

```powershell
node -v
npm install -g openskills
openskills --version
```

Upstream project: [numman-ali/openskills](https://github.com/numman-ali/openskills).

## Typical Workflow

1. Install OpenSkills (see [Installing OpenSkills](#installing-openskills) above).
2. Choose the needed skills for the project.
3. Install from the team skills repository.
4. Run `npx openskills sync`.
5. Review the generated `AGENTS.md`.
6. Commit the installed skills and sync output according to team policy.

## Suggested Commands

These are examples. Adjust the source URL to match your hosting setup.

**Latest default branch** (OpenSkills clones `HEAD` of the default branch only):

```powershell
npx openskills install git@github.com:your-org/skills-cloud.git
npx openskills sync
```

**Pinned tag or branch** (historical or stable release): use a local shallow clone, then install from the folder—see [docs/VERSIONING_AND_OPENSKILLS.md](VERSIONING_AND_OPENSKILLS.md) or run:

```powershell
powershell -ExecutionPolicy Bypass -File <path-to-skills-cloud-repo>\scripts\install-skills-at-ref.ps1 -Source git@github.com:your-org/skills-cloud.git -Ref v1.0.0
npx openskills sync
```

**Single skill only** (GitHub shorthand: clone only that folder from the repo; still uses a full shallow clone under the hood):

```powershell
npx openskills install your-org/skills-cloud/skills/backend-api
npx openskills sync
```

Replace `your-org/skills-cloud` and `backend-api` with your registry and skill id. The web catalog’s OpenSkills section shows the same commands when the configured remote is GitHub.

If you prefer a universal multi-agent path, add `-Universal` to the helper script or:

```powershell
npx openskills install git@github.com:your-org/skills-cloud.git --universal
npx openskills sync
```

## Pushing local skill edits back to the registry

OpenSkills **`install` / `sync` / `update` (if available)** only pull skills **into** your project. There is **no** command to upload changes from a consuming repo to the skills registry—you use **Git** on the skills repository.

**If you edited an installed copy** (e.g. under `.claude/skills/<skill-id>/` or `.agent/skills/<skill-id>/`, depending on how you installed):

1. **Clone** the team skills repo (if you do not already have it).
2. **Copy** the whole skill directory from the project into `skills/<skill-id>/` in that clone (replace `SKILL.md`, `meta.json`, `references/`, `scripts/`, etc.).
3. In the skills repo root, run **`python scripts/validate-skills.py`** and **`powershell -ExecutionPolicy Bypass -File .\scripts\generate-catalog.ps1`**.
4. **Commit** `skills/<skill-id>/` and `catalog/data/` (and any other touched files), **push**, and open a **PR**.

**To avoid copy-paste:** develop the skill **inside** the skills repo, then in the product project run **`npx openskills install <path-to-local-clone>`** (and `npx openskills sync`). After edits, reinstall or run **`npx openskills update`** if your OpenSkills version records the source. Pinning and non-default branches are covered in [docs/VERSIONING_AND_OPENSKILLS.md](VERSIONING_AND_OPENSKILLS.md).

See also [CONTRIBUTING.md](../CONTRIBUTING.md) for new skills and review expectations.

## Selecting Skills Per Project

Use a small, curated project skill set.

Examples:

- API service: `backend-api`, `release-triage`
- Frontend app: `frontend-react`, `release-triage`
- Full-stack app: `backend-api`, `frontend-react`, `release-triage`

Start by browsing the catalog, then shortlist the smallest useful set of skills for the project.

## Upgrade Policy

- Prefer upgrades through pull requests
- Upgrade to a reviewed tag or release branch
- Re-run `npx openskills sync` after upgrade
- Review whether skill descriptions or triggers changed

## What Projects Should Not Do

- Do not install the entire catalog by default
- Do not modify installed shared skills ad hoc without feeding changes back upstream
- Do not rely on untagged production upgrades unless the team accepts that risk

## Recommended Project Maintainer Checklist

1. Confirm which skills are actually used
2. Remove stale skills from the local project install set
3. Re-sync `AGENTS.md` after changes
4. Review upgrade notes before bumping versions
