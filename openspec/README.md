# OpenSpec

OpenSpec is a specification-driven workflow for planning and implementing changes. It uses a **spec-driven** schema where each change progresses through a sequence of artifacts before implementation begins.

## Directory Structure

```
openspec/
├── config.yaml          # Schema selection and project-level settings
├── specs/               # Accumulated specs (merged after changes are archived)
└── changes/             # Active and archived changes
    ├── <change-name>/   # One directory per change
    │   ├── proposal.md
    │   ├── design.md
    │   ├── specs/
    │   │   └── <capability>/spec.md
    │   └── tasks.md
    └── archive/         # Completed changes moved here after implementation
```

## Artifacts

Each change produces artifacts in dependency order:

### proposal.md

The starting point. Defines **what** the change is and **why** it's needed. Contains:

- **Problem** — what's missing or broken today
- **Vision** — the end state we're working toward
- **Proposal** — high-level approach and architecture
- **Scope / Non-goals** — boundaries of the change

### design.md

The technical design. Explains **how** the change will be implemented. Contains:

- **Context** — current state, constraints, existing patterns
- **Goals / Non-Goals** — what this design achieves and excludes
- **Decisions** — key technical choices with rationale and alternatives considered
- **Risks / Trade-offs** — known limitations and mitigations

### specs/ (per capability)

Detailed specifications that define **what the system should do**. One spec file per capability, organized as `specs/<capability>/spec.md`. Each spec contains:

- **Requirements** — normative statements using SHALL/MUST
- **Scenarios** — testable WHEN/THEN cases for each requirement

Specs use delta operations (ADDED, MODIFIED, REMOVED) when modifying existing capabilities.

### tasks.md

The implementation checklist. Breaks down the work into numbered, checkboxed tasks grouped by concern. Tasks are ordered by dependency and each should be completable in one session. The apply phase tracks progress by parsing checkbox state (`- [ ]` / `- [x]`).

## Workflow Commands

| Command | Purpose |
|---------|---------|
| `/opsx:propose <name>` | Create a new change and generate all artifacts |
| `/opsx:apply` | Implement tasks from an active change |
| `/opsx:explore` | Think through ideas before or during a change |
| `/opsx:archive` | Archive a completed change |

## Artifact Dependency Graph

```
proposal ──► design ──► tasks
         └─► specs ──┘
```

The `tasks` artifact requires both `design` and `specs` to be complete before it can be created.
