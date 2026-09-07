# Log de Progresso — api-gateway

## Estado Atual (Current State)

**Última atualização:** 2026-09-07
**Feature ativa:** nenhuma (`feat-001` fechada; `feat-002` é a próxima elegível)

## Status

### O que está pronto

- [x] Harness deste serviço criado.
- [x] **`feat-001` (Setup do projeto) — `done` em 2026-09-07.** Bootstrap Spring Boot 4.1.1/Java
      25/Maven completo: Spring Cloud Gateway Server WebMVC (bloqueante, decisão de 2026-09-07,
      ver `docs/DECISIONS-LOG.md` — mantém a mesma pilha síncrona dos outros 3 serviços Java em
      vez de introduzir o único serviço reativo/WebFlux do projeto), pacotes
      `config/filter/route` (sem hexagonal — serviço sem domínio de negócio), gate JaCoCo 80%
      (real 100% na única classe com lógica, `LocaleConfig`), scaffold de i18n (`MessageSource`,
      `messages.properties` base desde o início — evita o gotcha de `auth-service feat-003.6`),
      health checks do Actuator (liveness+readiness, testados via HTTP real), logging estruturado
      ECS. 6 subtasks (SV-148..153, story SV-147). Evidência completa em `feature_list.json`.

### Em andamento

- Nenhuma feature iniciada.

### Próximos passos (Next Steps)

1. **`feat-002`** (Validação de token PASETO + injeção de `X-User-Id`/`X-Tenant-Id`) — próxima
   feature elegível (depende só de `feat-001`, `done`). Primeira feature a introduzir a chave
   `PASETO_LOCAL_KEY` (mesma de `auth-service`) — cria `.env.example` pela primeira vez neste
   serviço (achado do Plan Review de `feat-001`: não criar o arquivo antes de existir uma
   variável real). `Plan Reviewer` antes de codificar.
2. `feat-006` (filtro global de `X-Correlation-Id`) também já está elegível (depende só de
   `feat-001`) — pode ser feita em paralelo a `feat-002`/`feat-003`/`feat-004` já que é um filtro
   independente (não acoplado ao de PASETO). Lacuna descoberta nesta sessão: o backlog original
   de `feat-001..005` nunca atribuiu essa responsabilidade a nenhuma feature, apesar de
   `docs/OBSERVABILITY-AND-CONFIG.md` já documentar isso como responsabilidade do Gateway e
   `bets-service` já esperar por ela (`BetEventEnvelope.correlationId`).

## Bloqueios / Riscos

- Nenhum bloqueio aberto. Dois achados do Test Suite Auditor (feat-001) ficam deliberadamente
  em aberto até `feat-002`: `MessagesTest`/`LocaleConfigTest` provam o mecanismo isoladamente,
  não através do bean real do Spring numa resposta HTTP localizada de verdade — mesmo tradeoff
  já aceito em `auth-service feat-001.5` (nenhuma exceção de negócio real existe ainda pra
  localizar). `feat-002` (primeiro filtro real, primeira resposta 401 RFC 7807 localizada) deve
  fechar essa lacuna com cobertura real, não com um teste inventado agora.

## Decisões tomadas

- **Spring Cloud Gateway Server WebMVC** (bloqueante/servlet), não o Gateway reativo/WebFlux —
  decisão tomada com o usuário antes de `feat-001`, registrada em `docs/DECISIONS-LOG.md`
  (2026-09-07). Mesma pilha síncrona dos outros 3 serviços Java.
- **Filtro de `X-Correlation-Id` vira `feat-006` dedicada** — decisão tomada com o usuário
  (`AskUserQuestion`) ao encontrar a lacuna, em vez de embutir no filtro de PASETO (`feat-002`)
  ou adiar sem rastreamento.
- **CI hardening entrou dentro da própria `feat-001.1`** (renomeada para "Endurecer pipeline de
  CI e bootstrap do pom.xml"), não como subtask própria antecipada como em `stats-service` —
  achado reativo (PR quebrou), não proativo; registrado em `docs/CI-CD.md` e no `plan_review` da
  feature como o ponto onde o Plan Review original falhou (buscou por palavra-chave em vez de
  ler a nota inteira).

## Arquivos modificados nesta sessão

- Todo o código de `feat-001`: `pom.xml`, `mvnw`/`mvnw.cmd`/`.mvn/`, `.gitattributes` (regra
  para `mvnw`), `.github/workflows/ci.yml` (endurecido), `.github/scripts/validate-sonar-issues.py`
  (novo), `src/main/...` e `src/test/...` completos, `CHANGELOG.md` (formato corrigido),
  `feature_list.json` (subtasks, plan_review, evidence).
- `CLAUDE.md` deste serviço: groupId corrigido, bullet novo sobre `feat-006`, Gateway Server
  WebMVC citado explicitamente.

## Evidência de conclusão

- `./mvnw clean verify`: 9 testes, 0 falhas, gate JaCoCo 80% passando (100% real na única classe
  com lógica).
- `./init.sh` deste serviço e da raiz: verdes.
- Delivery Reviewer: `PASS`. Test Suite Auditor: `CONCERNS` (2 achados aceitos, diferidos para
  `feat-002` — ver "Bloqueios / Riscos" acima).
- Pipeline de CI real (GitHub Actions + SonarCloud) verde no PR `feature/SV-147` → `develop`
  (gate completo, incluindo zero-issue do SonarCloud).

## Notas para a próxima sessão

Ler `docs/DECISIONS-LOG.md` (entrada 2026-09-07) antes de `feat-002`, para não redescobrir a
decisão de Gateway Server WebMVC. `PASETO_LOCAL_KEY` é a MESMA chave de `auth-service` — não
gerar uma independente.
