# Session Handoff — api-gateway

## Current Objective

- Goal: `epic-008` (api-gateway) — first feature (`feat-001`, project bootstrap) delivered.
- Current status: `feat-001` `done`, merged into `develop`. 5 features remain
  (`feat-002`..`feat-006`).
- Branch / commit: `develop` @ `3ce4ba4` (merge of `feature/SV-147`).

## Completed This Session

- [x] `feat-001` (Setup do projeto) fully implemented, reviewed, and merged — see
      `progress.md` for the full breakdown (6 subtasks, SV-148..153, story SV-147).
- [x] Root harness gap found and closed: `feat-006` (X-Correlation-Id filter) added to the
      backlog — it was never assigned to any of the original `feat-001..005`.
- [x] Decision recorded: Spring Cloud Gateway Server WebMVC (blocking), not reactive.

## Verification Evidence

| Check | Command | Result | Notes |
|---|---|---|---|
| Build/test | `./mvnw clean verify` | 9 tests, 0 failures | JaCoCo 80% gate real (100% on `LocaleConfig`) |
| Local harness | `./init.sh` | pass | service + root |
| CI (subtask gate) | GitHub Actions | pass | 6 PRs, one per subtask |
| CI (full gate) | GitHub Actions + SonarCloud | pass | `feature/SV-147` → `develop`, zero-issue gate included |
| Delivery Reviewer | review-suite skill | PASS | |
| Test Suite Auditor | codebase-audit-suite skill | CONCERNS | 2 findings deferred to `feat-002` by design |

## Files Changed

- Full service bootstrap (see `progress.md` "Arquivos modificados").

## Decisions Made

- Spring Cloud Gateway Server WebMVC over the reactive Gateway (`docs/DECISIONS-LOG.md`,
  2026-09-07).
- `X-Correlation-Id` filter gets its own feature (`feat-006`), not folded into `feat-002`.
- CI hardening folded into `feat-001.1` (renamed), not a separate proactive subtask — a reactive
  fix after the subtask's own PR failed CI (docs/CI-CD.md already warned this repo still had
  the gap; the Plan Review missed it).

## Blockers / Risks

- None open. Two Test Suite Auditor findings are deliberately deferred to `feat-002` (see
  `progress.md`) — not a defect, matches `auth-service feat-001.5`'s accepted precedent.

## Next Session Startup

1. Read `../../CLAUDE.md`, `docs/DECISIONS-LOG.md` (2026-09-07 entry), this service's `CLAUDE.md`.
2. Read `feature_list.json` — `feat-002` (PASETO validation) is next, or `feat-006`
   (correlation-id filter, independent, could run in parallel).
3. Run `./init.sh` (should pass).
4. `Plan Reviewer` before coding either feature — read `docs/CI-CD.md` in full this time, not
   just a keyword search, given what happened in `feat-001.1`.

## Recommended Next Step

- `feat-002` (PASETO validation + X-User-Id/X-Tenant-Id injection) — first feature to read
  `PASETO_LOCAL_KEY` (same key as `auth-service`) and create this service's `.env.example`.
