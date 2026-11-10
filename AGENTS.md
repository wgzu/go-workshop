# Agent Instructions — Compose Multiplatform Project

This file defines the mandatory workflow for coding agents working in this repository.

## 1. Read the project guidelines first

**Before analyzing, planning, editing, or generating any code, read [`COMPOSE_GUIDELINES.md`](./COMPOSE_GUIDELINES.md) in full.**

Treat it as the primary source of truth for:
- Project structure and module boundaries
- Architecture and dependency direction
- Kotlin and Compose coding conventions
- State management, navigation, and dependency injection
- Networking, persistence, testing, and performance practices

Do not start implementation until you have reviewed the relevant sections. Revisit the document when switching tasks or touching a different architectural layer.

If `COMPOSE_GUIDELINES.md` is missing or unreadable, **stop and report the issue** instead of inventing replacement architecture rules.

## 2. Instruction priority

Follow, in order:
1. The user's explicit requirements for the current task
2. This `AGENTS.md` file
3. `COMPOSE_GUIDELINES.md`
4. Existing project conventions, when they do not conflict with the above

If instructions conflict, explain the conflict briefly and follow the higher-priority instruction. Do not silently depart from the guidelines.

## 3. Workflow for every task

1. **Read:** Review `COMPOSE_GUIDELINES.md` and the files relevant to the task.
2. **Locate:** Identify the correct feature/module and its existing contracts before modifying anything.
3. **Plan:** For non-trivial changes, outline the smallest change that meets the request and preserves module boundaries.
4. **Implement:** Use existing patterns, components, and dependencies where possible. Avoid unrelated refactors and speculative abstractions.
5. **Validate:** Run relevant formatting, compilation, and tests when available. If validation cannot run, state what was not verified.
6. **Report:** Summarize changes, affected files, and any assumptions or verification gaps.

## 4. Non-negotiable architecture checks

- Keep features isolated; do not add direct feature-to-feature dependencies.
- Keep `core` modules independent of feature implementations.
- Keep domain logic independent of Compose, Ktor, SQLDelight, and platform SDKs.
- Keep API/database access out of composables and ViewModels; use domain contracts and repositories.
- Use immutable UI state and unidirectional event flow.
- Keep platform-specific code at the platform boundary.
- Use dependency injection and explicit constructor dependencies rather than hidden globals.
- Reuse the established design system and navigation strategy.
- Do not add new libraries, Gradle modules, or patterns without a concrete need.
- Include or update relevant tests when behavior changes.

These are quick checks, **not a substitute** for reading the complete guidelines.

## 5. Scope and changes to the guidelines

Do not modify `AGENTS.md` or `COMPOSE_GUIDELINES.md` unless the user explicitly requests it.

If a task requires deviating from the guidelines, identify the specific deviation, explain why, and keep it narrowly scoped. Never rewrite the guidelines merely to justify an implementation.

## 6. Completion checklist

Before responding, confirm:
- [ ] I read `COMPOSE_GUIDELINES.md` before implementation.
- [ ] My changes respect module boundaries and dependency direction.
- [ ] I followed existing Compose/Kotlin conventions.
- [ ] I avoided unnecessary dependencies or unrelated changes.
- [ ] I ran relevant checks, or explained why I could not.
- [ ] I summarized the result and any outstanding issues.
