# Contributing

1. Read [docs/GOVERNANCE.md](docs/GOVERNANCE.md) for ownership, versioning, and what counts as a breaking change.
2. New or updated skills: copy [templates/skill-template/SKILL.md](templates/skill-template/SKILL.md), add `meta.json` beside it, keep `SKILL.md` short and put detail under `references/`.
3. If you change how or when a skill applies, bump `version` in that skill’s `meta.json` per [docs/GOVERNANCE.md](docs/GOVERNANCE.md#per-skill-version-in-metajson).

4. Before opening a PR:
   - `python scripts/validate-skills.py`
   - `powershell -ExecutionPolicy Bypass -File .\scripts\generate-catalog.ps1`
   - Commit changes under `catalog/data/skills.json` and `catalog/data/skills.js` together with skill edits.
5. Fill in the [pull request template](.github/pull_request_template.md).
6. Optional: enable [CODEOWNERS](.github/CODEOWNERS) by uncommenting lines and setting your team handles.
