# Governance

## Scope

This repository is the central source for team-managed agent skills. Projects consume skills from here, but install them locally so they stay in control of timing and version adoption.

## Ownership

Every skill should declare:

- A primary owner
- A backup owner
- The team or domain it serves
- A review group for breaking changes

Recommended place for owner metadata:

- [CODEOWNERS](../.github/CODEOWNERS) for folder ownership (uncomment and set handles)
- [Pull request template](../.github/pull_request_template.md) for rollout notes
- [CHANGELOG.md](../CHANGELOG.md) for project-facing release summaries
- `python scripts/validate-skills.py` (and CI) for required files and `meta.json` shape

## Versioning Policy

Versioning happens at **two** levels: the **Git repository** (what consuming projects pin) and each **skill’s `meta.json`** (what humans and the catalog read).

### Repository releases (Git tags)

Use semantic versioning for **repository** tags (for example `v1.2.0`).

- **Patch**: wording cleanup, examples, non-behavioral changes
- **Minor**: backward-compatible workflow improvements or new skills
- **Major**: trigger changes, breaking workflow changes, renamed skills, deleted files, or changed required inputs

Consumer projects should install with OpenSkills from a **tag or release branch**, not from moving `main`, unless they accept drift.

### Per-skill version in meta.json

Each skill’s `skills/<name>/meta.json` field **`version`** is its own SemVer string (for example `1.2.0`). It is **independent of the repo tag number**: one repo release can ship several skills, each with its own bumped `version`.

Use the same MAJOR / MINOR / PATCH meaning as above, but **scoped to that skill**:

| Bump | Typical reasons (this skill only) |
|------|-----------------------------------|
| **MAJOR** | Breaking per [Breaking changes](#breaking-changes) (triggers, required steps, removed files, incompatible script behavior) |
| **MINOR** | New optional workflow, new references/scripts that do not break existing use |
| **PATCH** | Clarifications, typos, non-behavioral fixes |

**When to bump:** Any PR that changes **when or how** an agent should follow this skill (including `description` / trigger text that changes behavior) should bump at least **PATCH**; breaking or materially new behavior should bump **MINOR** or **MAJOR** as appropriate. Pure catalog copy or metadata typo fixes can stay **PATCH**.

OpenSkills does not read `meta.json`; it loads `SKILL.md` from the cloned repo. The `version` field is for **documentation, review, and the web catalog** so teams can see drift and write release notes per skill.

To install an **older snapshot** of the registry, consumers must use a **Git tag or commit**, not `meta.json` alone—see [VERSIONING_AND_OPENSKILLS.md](VERSIONING_AND_OPENSKILLS.md).

## Change Classes

### Safe changes

- Clarifying language
- Better examples
- New optional references
- New optional scripts

### Review-required changes

- Trigger condition changes
- Output format changes
- New required project files or commands
- New external dependencies
- Skill renames or folder moves

### Breaking changes

- Removing a skill
- Changing when a skill should trigger in a way that alters agent behavior
- Requiring new repo conventions from consuming projects
- Changing scripts relied on by existing project workflows

## Pull Request Expectations

Every pull request adding or modifying a skill should answer:

1. What user/project problem does this skill solve?
2. What are the trigger conditions?
3. What changed for consuming projects?
4. Is the change backward compatible?
5. Does any project need a migration step?

## Quality Checklist

- `meta.json` `version` bumped when the skill’s behavior or triggers change (see [Per-skill version in meta.json](#per-skill-version-in-metajson))
- `SKILL.md` stays concise
- References are one level deep from `SKILL.md`
- Scripts are directly executable and documented in the skill
- Names and descriptions are specific enough to trigger correctly
- The skill does not assume tools unavailable in most target projects

## Rollout Model

- Draft skills can be iterated in feature branches
- Stable skills merge to `main`
- Teams publish tagged releases at a predictable cadence
- Consumer projects upgrade on demand through normal pull request review
