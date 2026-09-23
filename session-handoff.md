# Session Handoff — api-gateway

> Estado atual, não histórico. O diário cronológico é o `progress.md` — este arquivo é reescrito
> a cada sessão para responder "o que a próxima sessão precisa saber agora".

**Última atualização:** 2026-09-23

## Objetivo atual

`feat-001`..`feat-016` `done`. Backlog deste serviço esgotado — nenhuma feature `not-started`
elegível agora.

## Concluído nesta sessão (2026-09-23)

- [x] **`feat-016` fechada** (Reformulação de marca StakeVault -> Arka, continuação do `epic-032`
      da raiz - 4º e último dos 4 serviços Java, mesmo plano base de `auth-service feat-019`).
      Único ponto real de marca: `pom.xml` linha 15 (`<description>`). Plan Reviewer condensado
      (READY) + Delivery Reviewer (PASS). Story SV-558, PRs #48-50, CI+SonarCloud verdes.
- [x] Mesmo residual de ambiente (processos `java.exe` órfãos travando o `repackage` local do
      Maven no Windows, mesmo lock nos 4 serviços Java) documentado em
      `services/auth-service/progress.md` - `mvn test` local verde, `mvn verify` completo
      confirmado pelo CI (Linux). Fecha a parte de `api-gateway` do `epic-032` - último dos 4
      serviços Java, resta só `infra/` na ordem sugerida.

## Bloqueios / Riscos

Mesmo risco documentado em `bets-service`/`stats-service` (não exclusivo daqui): a promoção
`develop -> main` deste repositório fica pausada até o usuário decidir o caminho de rede do
`KUBE_CONFIG` de `epic-028` (não alcança o cluster a partir de runners hospedados) — ver
`services/bets-service/session-handoff.md` e `docs/services/infra.md` pro detalhe.

## Próxima sessão — por onde começar

1. Rodar `./init.sh` (deve passar).
2. Backlog deste serviço vazio. Ver `feature_list.json` da raiz para o próximo epic elegível.
3. **Não promover `develop -> main`** até o usuário decidir o caminho de rede do `KUBE_CONFIG`.
