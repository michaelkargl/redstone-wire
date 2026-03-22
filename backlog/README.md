# Backlog

Project backlog for PresenceCube, managed via [Backlog.md](https://backlog.md). All task and milestone management is done through MCP tools — never edit the markdown files directly.

## Directory Structure

```
backlog/
├── config.yml        # Project settings (prefix, statuses, date format)
├── milestones/       # Milestone definitions with descriptions
├── tasks/            # Active tasks (To Do, In Progress, Done)
├── drafts/           # Draft tasks not yet committed to the backlog
├── decisions/        # Architectural or process decisions
├── docs/             # Supporting documents referenced by tasks
├── completed/        # Tasks moved here during periodic cleanup
└── archive/          # Canceled, duplicate, or invalid tasks
```

## Task Naming Convention

Tasks follow the pattern `<prefix>-<number> - <Title>.md`:

- **PC-001** — top-level parent task
- **PC-001.01** — subtask of PC-001 (first child)
- **PC-001.02** — subtask of PC-001 (second child)

The prefix `PC` is configured in `config.yml` and stands for PresenceCube.

## Task Lifecycle

```
Draft → To Do → In Progress → Done → (completed/)
                                   ↘ (archive/)  ← canceled/invalid only
```

- **Draft**: Ideas not yet committed
- **To Do**: Committed work, ready to start
- **In Progress**: Actively being worked on
- **Done**: Finished, stays in `tasks/` until periodic cleanup moves it to `completed/`
- **Archive**: Only for canceled, duplicate, or invalid tasks — not for completed work

## Milestones

Milestones group related tasks into deliverable goals. Each milestone has an ID (e.g., `m-0`) and a description. Tasks reference their milestone by ID.

## Key Rules

- **Tools only**: Use MCP tools (`task_create`, `task_edit`, `task_view`, etc.) to manage tasks. Direct file edits break metadata consistency.
- **Search first**: Before creating a task, search for existing ones to avoid duplicates.
- **Acceptance criteria**: Each task defines verifiable acceptance criteria — the "what", not the "how".
- **Dependencies**: Tasks can declare dependencies on other tasks by ID.
- **Parent-child**: Subtasks use dotted IDs (e.g., PC-001.03 is a child of PC-001).

## Integration with OpenSpec

Tasks are often generated from [OpenSpec](../openspec/README.md) change artifacts. The typical flow:

1. `/opsx:propose` creates a change with proposal, design, specs, and tasks
2. Backlog tasks are created from the OpenSpec task list, with references back to the spec files
3. `/opsx:apply` implements the tasks, checking off items in both the OpenSpec tasks.md and backlog acceptance criteria