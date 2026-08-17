# Session Handoff — api-gateway

## Current Objective

- Goal: bootstrap the api-gateway harness (new service, created to close a gap found during a
  harness audit — see root `progress.md` entry dated 2026-08-01).
- Current status: harness created, no code yet.
- Branch / commit: (not committed yet)

## Completed This Session

- [x] Created `CLAUDE.md`, `feature_list.json`, `init.sh`, `progress.md`, `session-handoff.md`.

## Verification Evidence

| Check | Command | Result | Notes |
|---|---|---|---|
| Build/test | `./init.sh` | not run yet | No build file exists yet (feat-001). |

## Files Changed

- All files in this directory — created.

## Decisions Made

- Maven + Spring Cloud Gateway. No hexagonal domain layer (no business domain here).
- Service-to-service auth for `telegram-integration` via a static `X-Service-Key`, resolved to
  a real `X-User-Id` by looking up `telegramUserId` in `auth-service`.

## Blockers / Risks

- End-to-end testing needs `auth-service` (epic-002) to exist first, for token issuance and the
  telegram-account lookup endpoint.

## Next Session Startup

1. Read `../../CLAUDE.md` and `../../docs/services/api-gateway.md`.
2. Read this directory's `CLAUDE.md`, `feature_list.json`, `progress.md`.
3. Run `./init.sh`.

## Recommended Next Step

- Start `feat-001` (project setup) once `epic-002` (auth-service) is far enough along to issue
  tokens against.
