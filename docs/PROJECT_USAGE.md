# Project Usage

## Consumption Model

Each product repository installs only the skills it needs from `skills-cloud`.

Recommended defaults:

- Install locally in the consuming repository
- Use `.agent/skills/` for multi-agent setups
- Pin to tags for stable projects

To browse available skills before installation, use the repository catalog described in [docs/WEB_CATALOG.md](WEB_CATALOG.md).

## Typical Workflow

1. Choose the needed skills for the project.
2. Install from the team skills repository.
3. Run `npx openskills sync`.
4. Review the generated `AGENTS.md`.
5. Commit the installed skills and sync output according to team policy.

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

If you prefer a universal multi-agent path, add `-Universal` to the helper script or:

```powershell
npx openskills install git@github.com:your-org/skills-cloud.git --universal
npx openskills sync
```

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
