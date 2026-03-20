#!/usr/bin/env python3
"""
Validate skills/*/ layout, meta.json schema, and SKILL.md frontmatter.
Exit code 0 if all checks pass; otherwise print errors and exit 1.
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SKILLS_DIR = ROOT / "skills"

REQUIRED_META_KEYS = (
    "name",
    "title",
    "description",
    "tags",
    "owner",
    "status",
    "version",
    "summary",
    "use_cases",
    "install_hint",
)

NAME_RE = re.compile(r"^[a-z0-9]+(-[a-z0-9]+)*$")
DRIVE_ABS_RE = re.compile(r"[A-Za-z]:\\|/Users/|/home/[^/]+/", re.MULTILINE)
# Relaxed SemVer 2.0.0: core x.y.z plus optional pre-release / build (no leading zeros required on parts)
SEMVER_RE = re.compile(
    r"^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)"
    r"(?:-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?"
    r"(?:\+[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?$"
)


def parse_frontmatter(md: str) -> str | None:
    if not md.startswith("---"):
        return None
    end = md.find("\n---", 3)
    if end == -1:
        return None
    return md[3:end].strip()


def frontmatter_name(fm: str) -> str | None:
    m = re.search(r"(?m)^name:\s*(.+?)\s*$", fm)
    if not m:
        return None
    return m.group(1).strip()


def validate_skill_dir(skill_dir: Path) -> list[str]:
    errors: list[str] = []
    rel = skill_dir.relative_to(ROOT)
    name = skill_dir.name

    if not NAME_RE.match(name):
        errors.append(f"{rel}: folder name must match {NAME_RE.pattern}")

    skill_md = skill_dir / "SKILL.md"
    meta_path = skill_dir / "meta.json"

    if not skill_md.is_file():
        errors.append(f"{rel}: missing SKILL.md")
        return errors
    if not meta_path.is_file():
        errors.append(f"{rel}: missing meta.json")
        return errors

    try:
        meta = json.loads(meta_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        errors.append(f"{rel}/meta.json: invalid JSON ({e})")
        return errors

    for key in REQUIRED_META_KEYS:
        if key not in meta:
            errors.append(f"{rel}/meta.json: missing required key {key!r}")

    if errors:
        return errors

    if meta["name"] != name:
        errors.append(f"{rel}/meta.json: name {meta['name']!r} must match folder {name!r}")

    for key in ("title", "description", "owner", "status", "summary", "install_hint"):
        val = meta.get(key)
        if not isinstance(val, str) or not str(val).strip():
            errors.append(f"{rel}/meta.json: {key} must be a non-empty string")

    ver = meta.get("version")
    if not isinstance(ver, str) or not str(ver).strip():
        errors.append(f"{rel}/meta.json: version must be a non-empty string")
    elif not SEMVER_RE.match(str(ver).strip()):
        errors.append(
            f"{rel}/meta.json: version must be SemVer (e.g. 1.0.0 or 1.0.0-rc.1), got {ver!r}"
        )

    tags = meta.get("tags")
    if (
        not isinstance(tags, list)
        or len(tags) < 1
        or not all(isinstance(t, str) and t.strip() for t in tags)
    ):
        errors.append(f"{rel}/meta.json: tags must be a non-empty array of non-empty strings")

    use_cases = meta.get("use_cases")
    if (
        not isinstance(use_cases, list)
        or len(use_cases) < 1
        or not all(isinstance(t, str) and t.strip() for t in use_cases)
    ):
        errors.append(f"{rel}/meta.json: use_cases must be a non-empty array of non-empty strings")

    md_text = skill_md.read_text(encoding="utf-8")
    fm = parse_frontmatter(md_text)
    if fm is None:
        errors.append(f"{rel}/SKILL.md: must start with YAML frontmatter (--- ... ---)")
    else:
        if "description:" not in fm:
            errors.append(f"{rel}/SKILL.md: frontmatter must include description:")
        fm_name = frontmatter_name(fm)
        if not fm_name:
            errors.append(f"{rel}/SKILL.md: frontmatter must include name:")
        elif fm_name != meta["name"]:
            errors.append(
                f"{rel}/SKILL.md: frontmatter name {fm_name!r} must match meta.json name {meta['name']!r}"
            )

    if DRIVE_ABS_RE.search(md_text):
        errors.append(
            f"{rel}/SKILL.md: avoid absolute filesystem paths in links or text; use repo-relative paths"
        )

    return errors


def main() -> int:
    if not SKILLS_DIR.is_dir():
        print("skills/: directory not found", file=sys.stderr)
        return 1

    skill_dirs = sorted(p for p in SKILLS_DIR.iterdir() if p.is_dir())
    if not skill_dirs:
        print("skills/: no skill directories found", file=sys.stderr)
        return 1

    all_errors: list[str] = []
    for d in skill_dirs:
        all_errors.extend(validate_skill_dir(d))

    if all_errors:
        print("Skill validation failed:\n", file=sys.stderr)
        for msg in all_errors:
            print(f"  - {msg}", file=sys.stderr)
        return 1

    print(f"OK: validated {len(skill_dirs)} skill(s) under skills/")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
