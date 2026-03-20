# Release Process

Use this reference for release-focused reviews.

## Priority Order

1. User-visible regressions
2. Contract or schema drift
3. Missing validation or missing tests
4. Operational risk and rollback complexity

## Review Questions

1. What changed that users or clients will notice?
2. What code paths lost coverage or confidence?
3. What is hardest to roll back quickly?
4. What assumptions should be confirmed before shipping?
