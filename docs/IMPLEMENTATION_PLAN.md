# Implementation Plan

## Objective

Stand up a team-owned `skills-cloud` repository that publishes reusable skills for internal projects, supports selective installation, and provides a safe update path.

## Success Criteria

- A project can install only the skills it needs from this repository
- Installed skills can be synced into the project's `AGENTS.md`
- Skill changes are reviewable, versioned, and documented
- Teams know who owns each skill and how upgrades are adopted
- Teammates can browse the available skill catalog from a simple web page

## Phase 1: Foundation

### Deliverables

- Create the base repository structure
- Define naming conventions for skills
- Define governance and versioning rules
- Create 2-3 exemplar skills
- Add a browsable web catalog
- Document the project install/update workflow

### Exit Criteria

- Repository structure is stable
- At least one pilot project can consume a skill
- Maintainers agree on release and ownership rules
- Catalog renders current skills from repository metadata

## Phase 2: Pilot Adoption

### Deliverables

- Select 2-3 real consumer projects
- Install only the skills relevant to each project
- Gather agent behavior feedback
- Tighten trigger descriptions and references
- Gather feedback on catalog discoverability and taxonomy

### Exit Criteria

- Pilot projects can upgrade without surprises
- Maintainers have examples of successful usage
- Common gaps have been converted into references or scripts
- Teams can find relevant skills from the catalog without repo spelunking

## Phase 3: Operationalization

### Deliverables

- Add release notes process
- Add CODEOWNERS or equivalent ownership mapping
- Add a pull request template for skill changes
- Add basic validation checks for required file layout
- Add metadata validation for `meta.json` and generated catalog output

### Exit Criteria

- New skills follow a standard review path
- Breaking changes are clearly flagged
- Consumer teams trust tagged releases
- Catalog remains accurate after normal repository changes

## Phase 4: Scale-Out

### Deliverables

- Expand domain coverage with more skills
- Retire duplicated project-local prompts and docs
- Establish regular release cadence
- Track adoption by project and skill
- Add richer catalog filters and examples if the simple catalog proves useful

### Exit Criteria

- Skills are reused across multiple teams
- Consumers treat the repository as the canonical source
- New projects can bootstrap quickly from the catalog

## Recommended Working Agreements

- Build around workflows, not departments
- Favor small skills over giant umbrella skills
- Keep references scoped and searchable
- Use scripts only where determinism or repetition justifies them
- Prefer additive changes; save reorganizations for major releases

## Initial Backlog

1. Confirm target install model: tag-based or branch-based consumption
2. Decide default install path: `.agent/skills` is recommended for multi-agent environments
3. Add repository release process
4. Add skill owner metadata
5. Add one backend, one frontend, and one operational skill
6. Add a static catalog backed by repository metadata
7. Pilot with two consumer repositories

## Risks And Mitigations

### Risk: Skills become long internal wikis

Mitigation:
Keep `SKILL.md` procedural and move detail into references.

### Risk: Projects unexpectedly change behavior after upgrades

Mitigation:
Prefer tagged releases and document breaking changes.

### Risk: Overlapping skills confuse triggering

Mitigation:
Keep descriptions precise and define skill boundaries early.

### Risk: Nobody owns stale skills

Mitigation:
Assign owners and review inactive skills quarterly.

### Risk: The web catalog drifts from actual skill instructions

Mitigation:
Treat `meta.json` as required alongside `SKILL.md` and regenerate catalog data in normal review flow.

## Milestone Recommendation

- Week 1: finalize structure and publish exemplar skills
- Week 2: pilot in 1-2 repositories
- Week 3: refine triggers, release first tagged version
- Week 4: onboard additional teams
