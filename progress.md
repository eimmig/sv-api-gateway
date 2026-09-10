# Log de Progresso — api-gateway

## Estado Atual (Current State)

**Última atualização:** 2026-09-08
**Feature ativa:** nenhuma (`feat-001..004` e `feat-006` fechadas; só `feat-005` resta)

## `feat-006` fechada — filtro global de X-Correlation-Id (2026-09-08)

Lacuna real encontrada na auditoria pré-codificação de `epic-008` (2026-09-07): `feat-001..005`
nunca cobriam o filtro de `X-Correlation-Id` já documentado em
`docs/OBSERVABILITY-AND-CONFIG.md`, apesar de `bets-service` já depender dele para popular
`correlationId` no envelope de evento. `CorrelationIdFilter` (novo) roda com
`@Order(Ordered.HIGHEST_PRECEDENCE)` e sem `shouldNotFilter` (toda rota, inclusive
`/actuator/**`) — gera `UUID.randomUUID()` quando o header chega ausente/em branco, propaga
quando presente, `MDC.put`/`remove("correlationId")` em torno do `chain.doFilter`, ecoa o header
na response (decisão minha, além do contrato documentado, mas não contradiz nada) e encaminha via
`CorrelationIdRequestWrapper` (mesmo padrão de `ResolvedIdentityRequestWrapper`) para o proxy HTTP
repassar a `auth-service`/`bets-service`/`stats-service`. Roda antes de
`PasetoAuthenticationFilter`/`ServiceKeyAuthenticationFilter` (nenhum dos dois tem `@Order`,
default `LOWEST_PRECEDENCE`) para que os próprios logs de rejeição desses filtros já carreguem o
correlation id.

**Achado real do Plan Review, corrigido antes de codificar**: o plano original propunha
acrescentar uma feature nova (`feat-013`) a `services/bets-service/feature_list.json` sinalizando
que `BetEventEnvelope` ainda não consome o header real — violaria "stay in scope" deste
`CLAUDE.md` (feature_list.json é artefato de harness de outro serviço, não vault). Corrigido:
sinalizado só via `docs/services/bets-service.md` (repositório raiz `sv-harness`), sem tocar em
nada dentro de `services/bets-service/`.

**Achado real do Delivery Reviewer, corrigido antes de fechar**: os 2 testes de integração novos
só exercitavam a rota pública `/api/v1/auth/login` (sem `ResolvedIdentityRequestWrapper` por
cima) — não provavam a composição de wrappers aninhados nas rotas autenticadas. Corrigido
estendendo os 2 testes de integração já existentes (PASETO, `X-Service-Key`) com asserts de
`X-Correlation-Id`, em vez de deixar a lacuna. Test Suite Auditor: PASS — matriz final cobre as 4
combinações reais (sem auth/PASETO/`X-Service-Key` × header ausente/presente).

2 subtasks (SV-177/178, story SV-176), 2 PRs (#19 filtro+testes, #20 fechamento) com CI verde,
depois PR #21 (story → `develop`) com CI + SonarCloud verdes. 47 testes totais no serviço, 0
falhas, gate JaCoCo 80% real. `./init.sh` do serviço e da raiz verdes.
`docs/services/api-gateway.md` e `docs/services/bets-service.md` atualizados no mesmo commit
lógico do fechamento.

## `feat-004` fechada — credencial de serviço X-Service-Key (2026-09-08)

Impedimento real encontrado ao planejar (antes de codificar): nenhuma nota do vault fixava
**como** o Gateway recebe o `telegramUserId` na chamada `POST /api/v1/bets` com `X-Service-Key`
— todas diziam "o Gateway resolve `telegramUserId -> userId/tenantId`", mas o corpo da
requisição é o mesmo DTO de aposta do formulário web (sem esse campo). Levado ao usuário
(`AskUserQuestion`): **header dedicado `X-Telegram-User-Id`**, não campo no corpo (evita acoplar
o Gateway ao schema do DTO de `bets-service`, que ele hoje não desserializa). Registrado em
`docs/DECISIONS-LOG.md` e propagado para `docs/API-CONTRACTS.md`, `docs/ARCHITECTURE.md`,
`docs/services/{api-gateway,telegram-integration}.md` (repositório raiz) antes de qualquer
código.

`ServiceKeyAuthenticationFilter` (novo): valida `X-Service-Key` (constant-time, mesmo padrão de
`AdminApiKeyFilter` de `auth-service`/`bets-service`/`stats-service`) + `X-Telegram-User-Id`,
chama `GET /api/v1/telegram-accounts/{id}` em `auth-service` via `RestClient` (reaproveita
`gateway.auth-service-url` de `feat-003`), injeta `X-User-Id`/`X-Tenant-Id` via
`ResolvedIdentityRequestWrapper` (mesma classe de `feat-002`) e roteia para `bets-service`.
`PasetoAuthenticationFilter` passa a pular requisições com `X-Service-Key` — os dois filtros
nunca processam a mesma chamada (mesmo predicado de presença do header).

**2 achados reais do próprio self-review, corrigidos antes do merge final** (PR #17, dado que
isto é um segundo caminho de autenticação inteiro, risco alto): (1) `ResolvedIdentityRequestWrapper`
só limpava `Authorization`, não `X-Service-Key`/`X-Telegram-User-Id` — o segredo compartilhado e
o id do chamador vazavam pra `bets-service` em toda chamada via bot; (2) `RestClient.create(url)`
sem timeout — falha lenta do `auth-service` travaria a thread do Gateway indefinidamente;
corrigido com `SimpleClientHttpRequestFactory` (2s conexão/3s leitura). Um terceiro achado do
gate `feature -> develop` do SonarCloud (`java:S7467`, variável de catch não usada) corrigido com
unnamed pattern (`catch (HttpClientErrorException.NotFound _)`, Java 25).

12 testes novos (6 do filtro, 1 de stripping de header, 1 end-to-end reforçado com asserts de
não-vazamento) — 38 testes totais no serviço, 0 falhas. `./init.sh` do serviço e da raiz verdes.
Pipeline completa (SonarCloud zero-issue) verde no PR `feature/SV-169` → `develop`.

## `feat-003` fechada — roteamento para os 3 serviços (2026-09-07, sessão seguinte)

3 subtasks (SV-166..168, story SV-165), 2 achados reais corrigidos antes do merge:

1. **`POST /api/v1/auth/login` ficaria bloqueado para sempre**: o filtro global de PASETO
   (`feat-002`) roda em toda rota exceto as excluídas; antes de `feat-003` não havia rota
   nenhuma, então o bug ficou latente. Corrigido com exceção exata em
   `PasetoAuthenticationFilter.shouldNotFilter` (`gateway.login-path`, configurável — mesmo
   padrão do `management.endpoints.web.base-path` de `feat-002`, inclusive reincidência do
   `java:S1075` do SonarCloud, corrigida do mesmo jeito: parametrizar de verdade, não suprimir).
2. **`/api/v1/telegram-links/**` faltava na tabela de rotas**: endpoint já existia em
   `auth-service` (`feat-006`) mas não estava coberto por nenhum prefixo documentado em nenhuma
   nota do vault — ficaria inalcançável via Gateway. Acrescentado ao grupo de `auth-service`;
   `/api/v1/telegram-accounts/**` continua fora, escopo de `feat-004` (`X-Service-Key`).

**Pré-requisito cross-service resolvido antes de desenhar a tabela**: nenhuma nota fixava porta
HTTP nem URL de destino dos 3 serviços Java (todos no default 8080, colidindo em dev local).
Decisão tomada com o usuário: porta fixa por serviço (`auth-service` 8081, `bets-service` 8082,
`stats-service` 8083) + `AUTH_SERVICE_URL`/`BETS_SERVICE_URL`/`STATS_SERVICE_URL` configuráveis
no Gateway (`docs/DECISIONS-LOG.md`, 2026-09-07). Implementado como 3 features independentes nos
outros serviços (`auth-service feat-008`/SV-159, `bets-service feat-011`/SV-161, `stats-service
feat-008`/SV-163), cada uma com Plan Review/Jira/PR/CI própria, **antes** desta feature poder
prosseguir — os 3 repositórios estavam com epic `done`, reabertos só para essa mudança mínima.

`RouteConfig` (3 `@Bean RouterFunction<ServerResponse>`, Spring Cloud Gateway Server WebMVC —
`GatewayRouterFunctions.route(id).route(predicate, HandlerFunctions.http()).before(BeforeFilterFunctions.uri(url)).build()`)
— API confirmada via `javap` contra os jars reais (`spring-cloud-gateway-server-webmvc` 5.0.3,
`spring-webmvc` 7.0.9), não só conhecimento treinado. Testes de roteamento usam
`com.sun.net.httpserver.HttpServer` (JDK nativo) como stub dos 3 destinos — WireMock rejeitado
por risco de conflito Jackson 2/3 (projeto usa Jackson 3 desde `feat-002`). 5 testes novos de
integração HTTP real + 1 de bypass de login no filtro (30 testes no serviço, 0 falhas).

Delivery Reviewer (passe próprio, sem subagentes — risco alto de autenticação, mitigado com
grounding real de bytecode/versão): PASS. `./init.sh` do serviço e da raiz verdes. Pipeline
completa (SonarCloud zero-issue) verde no PR `feature/SV-165` → `develop`.

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

## `feat-008` — rotear `/api/v1/tipsters` (2026-09-10)

Achado real de `infra/feat-002` (teste de resiliência de `epic-007`, outro repositório): o
Gateway nunca roteava `/api/v1/tipsters/**`, apesar de `TipstersController` existir em
`bets-service` desde a `feat-002` daquele serviço. Não era bug de `feat-007` — aquela feature
deixou a rota de fora *de propósito*, porque `tipsterId` era opcional em `CreateBetRequest` e
nenhum consumidor o preenchia. A decisão ficou obsoleta quando `apps/web feat-008` (catálogos)
ganhou uma aba dedicada de tipsters, sem que ninguém revisitasse o roteamento — `apps/web` só
testa contra `HttpClient` mockado, nunca contra este Gateway de verdade, então não pegou.

Fix mecânico, mesmo padrão de `feat-007`: 1 predicado novo em `RouteConfig.betsServiceRoute`, 1
path novo no `@ValueSource` já parametrizado de `GatewayRoutingIntegrationTest`. Verificado ao
vivo contra o Gateway rodando localmente (404 antes, 201 depois) — não só por `mvn verify`.
`docs/services/api-gateway.md` atualizado no mesmo commit lógico. 1 subtask (SV-275, story
SV-274), 2 PRs (#26 subtask->feature, #27 feature->develop), CI+SonarCloud verdes nos dois.

Com isso, `feature_list.json` deste harness fica 100% `done` (`feat-001..008`) — nenhum trabalho
pendente até surgir novo achado.

## `feat-009` — Dockerfile para imagem de produção (2026-09-10)

Achado real de `infra/feat-004` (migração para Kubernetes, `epic-010` da raiz): este serviço
nunca teve `Dockerfile` próprio. Multi-stage idêntico ao padrão de `auth-service feat-011`
(build `eclipse-temurin:25-jdk-alpine`, runtime `25-jre-alpine`, usuário não-root, porta 8080 —
default do Spring Boot, nunca sobrescrita). Build real e execução real testados contra a infra,
roteando para `auth-service`/`bets-service`/`stats-service` pelos nomes de host da rede:
`/actuator/health` UP. Imagem usada de fato pelos manifests Kubernetes de `infra/feat-004`. 1
subtask (SV-283, story SV-282), 2 PRs (#28 subtask->feature, #29 feature->develop), CI+SonarCloud
verdes nos dois. Com isso, `feature_list.json` deste harness fica 100% `done` (`feat-001..009`).
