## Summary

<!-- What does this PR change? -->

## Skill checklist (delete if not a skill change)

- [ ] Problem this skill / change solves:
- [ ] Trigger conditions (when should an agent use this?):
- [ ] Impact on consuming projects (install path, new commands, breaking behavior):
- [ ] Backward compatible? If not, migration notes added (CHANGELOG / PR body):
- [ ] Bumped `version` in `skills/<name>/meta.json` when triggers or workflow behavior changed ([docs/GOVERNANCE.md](docs/GOVERNANCE.md#per-skill-version-in-metajson)):
- [ ] Ran `python scripts/validate-skills.py` locally
- [ ] Ran `powershell -ExecutionPolicy Bypass -File .\scripts\generate-catalog.ps1` and committed `catalog/data/skills.json` + `skills.js`

See [docs/GOVERNANCE.md](docs/GOVERNANCE.md) for change classes and review expectations.
