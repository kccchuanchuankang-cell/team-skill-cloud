---
name: backend-api
description: Use when the user is building or modifying backend APIs, handlers, service layers, request validation, response formats, or contract-sensitive server-side behavior.
---

# Backend API

## When To Use

Use this skill for:

- New API endpoints
- Request and response contract changes
- Controller, handler, or service-layer changes
- Input validation and error handling
- Backend refactors that could affect API behavior

Do not use this skill for generic infrastructure-only changes unless API behavior is involved.

## Workflow

1. Find the request entry point, validation layer, service layer, and tests.
2. Read [references/api-standards.md](references/api-standards.md) before changing behavior.
3. Preserve existing contracts unless the task explicitly asks for a change.
4. Update tests when behavior changes.
5. If contract drift risk is high, run the helper script in `scripts/`.

## Validation

- Check request validation paths
- Check success and error response shapes
- Check logging or tracing expectations if the project uses them
- Validate backward compatibility assumptions
