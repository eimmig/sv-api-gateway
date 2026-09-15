# Session Handoff — api-gateway

> Estado atual, não histórico. O diário cronológico é o `progress.md` — este arquivo é reescrito
> a cada sessão para responder "o que a próxima sessão precisa saber agora".

**Última atualização:** 2026-09-15

## Objetivo atual

`feat-001`..`feat-014` `done`. Backlog deste serviço esgotado — nenhuma feature `not-started`
elegível agora.

## Concluído nesta sessão (2026-09-15)

- [x] **`feat-014` fechada** (CD automático — job `deploy` em `ci.yml`, `kubectl rollout restart
      deployment/api-gateway` contra `KUBE_CONFIG`/`ci-deployer` de `infra/feat-007`). Terceira
      aplicação idêntica do padrão já revisado em `bets-service feat-018`/`stats-service feat-019`
      na mesma sessão — única diferença o nome do `Deployment`. Story SV-429, subtasks
      SV-430/SV-431, PRs #42/#43/#44, CI+SonarCloud verdes. `Delivery Reviewer`: PASS (revisão
      condensada).
- [x] **Fechamento em 2 disparos de `--sync-status`, corrigindo o erro cometido nas 2 features
      anteriores desta sessão** (`bets-service feat-018`/`stats-service feat-019`, onde a última
      subtask e a feature foram marcadas `done` na mesma edição, pulando o estado `Review` no
      board): aqui, `feat-014.2` foi marcada `done` sozinha primeiro (`--sync-status` → story caiu
      em `Review` corretamente), e só numa edição separada, depois do merge `story -> develop`
      real, a feature virou `done` (`--sync-status` → `Review -> Done`). Padrão correto a manter
      nos 3 repositórios restantes de `epic-028`.
- [x] Disparo real do job `deploy` adiado (mesma decisão de `bets-service`/`stats-service`) —
      promoção `develop -> main` é decisão de release mais ampla, não desta feature.

## Bloqueios / Riscos

Nenhum.

## Próxima sessão — por onde começar

1. Rodar `./init.sh` (deve passar).
2. Backlog deste serviço vazio. Trabalhar noutro harness — ver `feature_list.json` da raiz
   (`epic-028`: 3 repositórios restantes — `auth-service feat-016`, `telegram-integration
   feat-010`, `web feat-030`).
