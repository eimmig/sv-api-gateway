# Session Handoff — api-gateway

> Estado atual, não histórico. O diário cronológico é o `progress.md` — este arquivo é reescrito
> a cada sessão para responder "o que a próxima sessão precisa saber agora".

**Última atualização:** 2026-09-15

## Objetivo atual

`feat-001`..`feat-015` `done`. Backlog deste serviço esgotado — nenhuma feature `not-started`
elegível agora.

## Concluído nesta sessão (2026-09-15)

- [x] **`feat-014` fechada** (CD automático — job `deploy` em `ci.yml`). Ver entrada anterior
      neste mesmo handoff / `progress.md` para o detalhe.
- [x] **`feat-015` fechada** (rotear `/api/v1/teams` pra `bets-service` — achado real deixado em
      aberto por `bets-service feat-017` na mesma sessão). Mudança mecânica idêntica aos
      precedentes de `tipsters`/`bankroll`/`settings`. Story SV-441 (subtask SV-442), PRs #45/#46,
      CI+SonarCloud verdes. `Delivery Reviewer`: PASS. Desbloqueia `apps/web feat-021`.
- [x] Fechamento em 2 disparos de `--sync-status` (padrão correto, mantido desde `feat-014`).

## Bloqueios / Riscos

Nenhum.

## Próxima sessão — por onde começar

1. Rodar `./init.sh` (deve passar).
2. Backlog deste serviço vazio. Ver `feature_list.json` da raiz para o próximo epic elegível.
