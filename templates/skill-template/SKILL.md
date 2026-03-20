---
name: example-skill
description: Use when the user needs a specific, repeatable workflow that this skill handles better than general-purpose prompting.
---

# Example Skill

## When To Use

Use this skill when:

- The task matches this workflow directly
- Deterministic steps matter
- There are project-specific conventions that should be followed

## Workflow

1. Inspect the local repository and identify the relevant files.
2. Read only the references needed for the task.
3. Execute the workflow with minimal context expansion.
4. Validate the result with the lightest useful checks.

## References

- If the task needs standards or examples, read files in `references/`.
- If the task needs deterministic automation, use files in `scripts/`.

## Output Expectations

- Keep changes scoped
- Explain assumptions
- Note any validation gaps
