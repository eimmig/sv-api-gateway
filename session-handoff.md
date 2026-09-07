# Session Handoff — api-gateway

## Current Objective

- Goal: `epic-008` (api-gateway) — `feat-001` (bootstrap) and `feat-002` (PASETO validation)
  delivered.
- Current status: `feat-001`/`feat-002` `done`, both merged into `develop`. 4 features remain
  (`feat-003`, `feat-004`, `feat-005`, `feat-006`).
- Branch / commit: `develop` @ `fb8a877` (merge of `feature/SV-154`).

## Completed This Session

- [x] `feat-002` (PASETO validation filter, `X-User-Id`/`X-Tenant-Id` injection) fully
      implemented, reviewed, and merged — see `progress.md` for the full breakdown (4 subtasks,
      SV-155..158, story SV-154).
- [x] Real gap found and closed: token transport (`Authorization: Bearer`) was never fixed in the
      vault before this feature — now in `docs/API-CONTRACTS.md`.
- [x] Real gotcha found and documented: any blocking filter in this service must exclude
      `/actuator/**` or it silently breaks health checks (`docs/CONVENTIONS.md`).

## Verification Evidence

| Check | Command | Result | Notes |
|---|---|---|---|
| Build/test | `./mvnw clean verify` | 24 tests, 0 failures | JaCoCo 80% gate real |
| Local harness | `./init.sh` | pass | service + root |
| CI (subtask gates) | GitHub Actions | pass | 4 PRs, one per subtask |
| CI (full gate) | GitHub Actions + SonarCloud | pass | 2 real SonarCloud findings (`java:S1075`) fixed before it went green |
| Delivery Reviewer | review-suite skill | PASS | |
| Test Suite Auditor | codebase-audit-suite skill | CONCERNS → fixed | 2 findings (asymmetric claim coverage, conflated tampered/wrong-key test) resolved before closing |

## Decisions Made

- `Authorization: Bearer <token>` as the token transport (`docs/API-CONTRACTS.md`, 2026-09-07).
- Single generic `error.invalid-token` message for every rejection cause (missing/malformed/
  tampered/expired) — matches the project's anti-enumeration philosophy, matches what
  `CLAUDE.md`/`docs/services/api-gateway.md` already documented.
- Blocking filters in this service must exclude `/actuator/**` from the start
  (`docs/CONVENTIONS.md`) — applies to `feat-004`'s `X-Service-Key` filter too.

## Blockers / Risks

- None open.

## Next Session Startup

1. Read `../../CLAUDE.md`, `docs/DECISIONS-LOG.md` (2026-09-07 entries), this service's
   `CLAUDE.md`.
2. Read `feature_list.json` — `feat-003` (routing) or `feat-006` (correlation-id filter,
   independent) are both eligible. WIP max 1 — don't start both in parallel sessions.
3. Run `./init.sh` (should pass).
4. `Plan Reviewer` before coding either feature — read `docs/CI-CD.md` and `docs/CONVENTIONS.md`
   in full, not just a keyword search (lesson from `feat-001`).

## Recommended Next Step

- `feat-003` (routing to auth-service/bets-service/stats-service) is the natural next step —
  first feature that gives this service an actual purpose beyond validating tokens in isolation.
  `feat-006` (correlation-id filter) is independent and could run in parallel in a different
  session.
