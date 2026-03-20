# API Standards

Use this reference when editing backend APIs.

## Guardrails

- Prefer explicit request validation near the boundary
- Keep error responses consistent within a project
- Preserve field names unless the change is intentional and reviewed
- Avoid mixing transport concerns with core business logic

## Review Questions

1. What existing clients depend on this response shape?
2. Is the validation behavior compatible with current callers?
3. Are tests covering both successful and failing paths?
4. Are nullability and default values handled consistently?
