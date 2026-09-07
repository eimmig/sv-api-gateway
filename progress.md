# Log de Progresso — api-gateway

## Estado Atual (Current State)

**Última atualização:** 2026-09-07
**Feature ativa:** nenhuma (`feat-001`/`feat-002` fechadas; `feat-003` ou `feat-006` são as
próximas elegíveis)

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

## `feat-002` fechada — primeiro filtro de autenticação real do serviço (2026-09-07, mesmo dia)

Decisão registrada antes de codificar: token PASETO transportado em `Authorization: Bearer
<token>` — nenhuma nota do vault fixava isso até então (achado real, corrigido em
`docs/API-CONTRACTS.md` "Confiança entre serviços").

`PasetoAuthenticationFilter` (`OncePerRequestFilter`): decripta via `Paseto.decrypt` (`catch
RuntimeException` amplo — `paseto4j` não tem um único tipo de exceção pra token inválido,
confirmado via `javap`, documentado em `docs/CONVENTIONS.md`), valida presença de
`userId`/`tenantId` e `exp` estritamente no futuro, injeta `X-User-Id`/`X-Tenant-Id` via
`ResolvedIdentityRequestWrapper` (nunca repassa `Authorization` nem aceita identidade vinda do
cliente), exclui `/actuator/**` via `shouldNotFilter`. 4 subtasks (SV-155..158, story SV-154) —
`feat-002.3` fechada sem commit próprio (escopo já entregue dentro de `feat-002.2`, mesmo padrão
inseparável implementação+teste de `auth-service AdminApiKeyFilter`/`Test`).

**Achados reais corrigidos**:
- `/code-review` (2 passes): NPE em claim nula dentro do wrapper, falta de validação de
  `userId`/`tenantId` presentes (só `exp` era checado), `Authorization` original ainda alcançável
  no request repassado (contradizia o requisito da própria feature), limite de expiração `<` em
  vez de `<=` (semântica RFC 7519 — token só é válido estritamente antes do `exp`).
- Regressão real só encontrada rodando `mvn verify` (não pelo `/code-review` do diff isolado): o
  filtro, uma vez virando `@Component`, passou a bloquear `/actuator/health` do `feat-001.4` —
  corrigido com `shouldNotFilter`. Novo gotcha documentado em `docs/CONVENTIONS.md`: todo filtro
  *bloqueante* deste serviço precisa dessa exclusão.
- SonarCloud (`java:S1075`, gate `feature -> develop`) pegou o `/actuator/` hardcoded — corrigido
  lendo `management.endpoints.web.base-path` (mais correto: sobrevive a reconfiguração) — e depois
  pegou até a concatenação do delimitador `"/"` na comparação de prefixo, corrigido evitando
  concatenação (comparação por `charAt` com literal `char`, não `String`). Duas rodadas de fix
  reais no mesmo PR antes do gate passar.
- Test Suite Auditor: cobertura assimétrica (`userId` ausente nunca testado, só `tenantId`) e
  "token adulterado" conflado com "token de chave errada" (caminhos de exceção distintos da
  biblioteca — confirmado pelos nomes de exceção diferentes no log:
  `IllegalArgumentException` vs `IllegalStateException`). Ambos fechados antes de marcar `done`.

Delivery Reviewer: `PASS`. Test Suite Auditor: `CONCERNS` → corrigido antes de fechar. 24 testes
totais / 0 falhas, gate JaCoCo 80% real. `./init.sh` do serviço e da raiz verdes. Pipeline
completa (incl. SonarCloud zero-issue) verde no PR `feature/SV-154` → `develop`.

## Notas para a próxima sessão

Ler `docs/DECISIONS-LOG.md` (entrada 2026-09-07) antes de `feat-003`/`feat-006`, para não
redescobrir a decisão de Gateway Server WebMVC. `PASETO_LOCAL_KEY` é a MESMA chave de
`auth-service` — não gerar uma independente. Qualquer filtro *bloqueante* novo (`feat-004`,
`X-Service-Key`) precisa excluir `/actuator/**` desde o início (ver `docs/CONVENTIONS.md`) — não
redescobrir a regressão de `feat-002`. Ao escrever literal de path/delimitador em Java, esperar
que o gate `feature -> develop` do SonarCloud (`java:S1075`) reprove até um `char` de barra
concatenado — preferir comparação por `charAt`/`regionMatches` a concatenação de string desde o
início, nesses casos.
