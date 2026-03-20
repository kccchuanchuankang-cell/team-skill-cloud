# Skill versions vs OpenSkills installs

## Two different “versions”

| What | Meaning |
|------|--------|
| **`meta.json` → `version`** | SemVer for **this skill’s** changelog and the web catalog. Human-facing; OpenSkills **does not read it** when loading `SKILL.md`. |
| **Git ref (tag / commit / branch)** | The **actual snapshot** of every file in the registry. This is what determines which `SKILL.md` content you get. |

So: **`meta.json` version describes the skill; Git describes the install snapshot.** They are related only by team discipline (e.g. bump skill versions in the same commit you tag).

## What OpenSkills does today

Upstream [OpenSkills](https://github.com/numman-ali/openskills) clones the repository with a **shallow clone of the default branch** and does **not** expose a `--branch` / `@tag` argument on `install`. So:

```bash
npx openskills install git@github.com:your-org/skills-cloud.git
```

always follows the registry’s **default branch HEAD**, not an old tag.

## How to install a **historical** snapshot

You must get a checkout at the desired **tag or commit**, then point OpenSkills at that **local directory** (supported today).

### Option A — Tag (recommended for releases)

```powershell
git clone --depth 1 -b v1.2.0 git@github.com:your-org/skills-cloud.git .\_skills-src
npx openskills install .\_skills-src
npx openskills sync
Remove-Item -Recurse -Force .\_skills-src
```

Or use the helper: [scripts/install-skills-at-ref.ps1](../scripts/install-skills-at-ref.ps1).

### Option B — Specific commit

Shallow single-branch clone cannot always check out an arbitrary old commit; clone then fetch that commit:

```powershell
git clone git@github.com:your-org/skills-cloud.git .\_skills-src
Set-Location .\_skills-src
git fetch origin <commit-sha> --depth 1
git checkout <commit-sha>
Set-Location ..
npx openskills install .\_skills-src
npx openskills sync
Remove-Item -Recurse -Force .\_skills-src
```

### Option C — Submodule / vendor

Pin the whole `skills-cloud` repo in a consumer project as a **submodule** at a fixed commit; run `openskills install` against that path after `git submodule update`.

## Team practice

1. **Tag the registry** for anything production consumers should pin (e.g. `v1.2.0`).
2. **Bump `meta.json` `version`** per skill when that skill’s behavior or triggers change ([GOVERNANCE.md](GOVERNANCE.md#per-skill-version-in-metajson)).
3. In consumer repos, **record the Git tag (or SHA)** you installed from in README or internal docs—not only the per-skill SemVer.
4. If you need first-class `openskills install ...@v1.2.0`, that is an **upstream feature request** to [numman-ali/openskills](https://github.com/numman-ali/openskills); until then, use local clone + `install <path>` as above.
