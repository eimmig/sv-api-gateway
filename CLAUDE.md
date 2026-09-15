# CLAUDE.md — api-gateway

Único ponto de entrada HTTP público da plataforma. Java 25 + Spring Boot 4.x + **Spring Cloud
Gateway Server WebMVC** (`spring-cloud-starter-gateway-server-webmvc` — bloqueante/servlet, não
o Gateway reativo/WebFlux, decisão de 2026-09-07, ver `../../docs/DECISIONS-LOG.md`, para manter
a mesma pilha síncrona dos outros 3 serviços Java). Parte do harness multinível do monorepo —
leia `../../CLAUDE.md` (raiz) para
invariantes cross-service antes deste arquivo, e `../../docs/services/api-gateway.md` para o
desenho completo (rotas, validação de token, credencial de serviço). Arquitetura interna,
build tool, testes e formato de API são normativos e já decididos em `../../docs/CONVENTIONS.md`,
`../../docs/TESTING.md` e `../../docs/API-CONTRACTS.md` — leia-os antes de `feat-001`.

## Por que este serviço existe

`../../docs/API-CONTRACTS.md` (seção "Confiança entre serviços") decide que `bets-service` e
`stats-service` **não revalidam o token PASETO** — apenas confiam no header `X-User-Id`. Esse
header só pode ser confiável se algo o injeta de forma centralizada depois de validar o token de
fato. Este serviço é esse "algo". Sem ele, o modelo de confiança descrito na documentação não
tem implementação real.

## Regras específicas deste serviço

- **Uma feature por vez (One feature at a time)**: escolha exatamente uma feature `not-started`
  de `feature_list.json` cujas dependências já estejam `done`.
- **Escopo restrito (stay in scope)**: não edite código de `auth-service`, `bets-service`,
  `stats-service` ou `telegram-integration` a partir desta pasta — este serviço só roteia e
  autentica, não implementa regra de negócio de nenhum outro serviço.
- **Sem banco de dados próprio**: este serviço é stateless (não é uma exceção à regra Database
  per Service — ele simplesmente não persiste nada). Não crie um Postgres para ele.
- **Não segue o layout hexagonal `domain/`/`application/`/`adapter/`** dos outros três serviços
  Java (ver `../../docs/CONVENTIONS.md`) — não há domínio de negócio aqui, só roteamento e
  filtros de segurança. Estrutura sugerida:
  `src/main/java/com/stakevault/betting/gateway/{config,filter,route}/`.
- **Maven** (não Gradle) — mesma decisão dos demais serviços Java em `../../docs/CONVENTIONS.md`.
- **Validação de token PASETO**: único serviço, junto com `auth-service`, que manipula a chave
  PASETO. Cliente envia o token em `Authorization: Bearer <token>` (ver
  `../../docs/API-CONTRACTS.md` seção "Confiança entre serviços" — nunca fixado antes de
  `feat-002`). Valida o token em toda rota autenticada e injeta `X-User-Id` **e** `X-Tenant-Id`
  (claims `userId`/`tenantId` do token — deixaram de ser o mesmo valor em 2026-08-02, ver
  `../../docs/DECISIONS-LOG.md`) antes de rotear para `bets-service`/`stats-service`. Nunca
  repassa o token PASETO original para os serviços downstream — eles não sabem validá-lo e não
  devem precisar.
- **Credencial de serviço para `telegram-integration` (`feat-004`)**: `telegram-integration` não
  tem token PASETO (não há usuário logado no fluxo do bot). Ele se autentica com um header
  `X-Service-Key` (segredo estático por ambiente, `gateway.service-key`/`SERVICE_KEY`, ver
  `.env.example` e `../../docs/OBSERVABILITY-AND-CONFIG.md`) em vez de um token PASETO, e informa
  **qual** usuário do Telegram fez a chamada via um segundo header, `X-Telegram-User-Id`
  (decisão de 2026-09-07, ver `../../docs/DECISIONS-LOG.md` — nenhuma nota fixava isso antes;
  corpo da requisição continua idêntico ao do formulário web). `ServiceKeyAuthenticationFilter`
  (`OncePerRequestFilter`, só processa requisições com `X-Service-Key` presente —
  `PasetoAuthenticationFilter` pula essas mesmas requisições via `shouldNotFilter`, os dois
  nunca processam a mesma chamada): valida a chave (constant-time, `MessageDigest.isEqual`,
  mesmo padrão de `AdminApiKeyFilter` de `auth-service`), chama internamente
  `GET /api/v1/telegram-accounts/{telegramUserId}` em `auth-service` via `RestClient` (reaproveita
  `gateway.auth-service-url` de `feat-003` — mesmo destino, sem URL nova) para resolver
  `userId`/`tenantId`, e injeta os dois via `ResolvedIdentityRequestWrapper` (mesma classe de
  `feat-002`, genérica o bastante para os dois mecanismos de autenticação) antes de rotear para
  `bets-service`. Não aceite `X-Service-Key`, `X-Telegram-User-Id`, `X-User-Id` nem `X-Tenant-Id`
  repassados adiante — sempre derivados aqui. Respostas: `401` chave ausente/inválida ou
  `X-Telegram-User-Id` ausente; `404` sem vínculo (`auth-service` retorna 404, repassado);
  `503` se a chamada a `auth-service` falhar por qualquer outro motivo (rede, 5xx) — nunca deixa
  a exceção crua vazar como HTML não-RFC7807.
  > **Resolvido em 2026-08-02** (ver `../../docs/DECISIONS-LOG.md` item 15): `auth-service`
  > resolve esse lookup consultando o diretório `TELEGRAM_LINK` no schema `public` — não assume
  > mais um `USER` global nem precisa varrer schemas.
- **Não roteia a criação de tenant**: o operador chama a rota administrativa de cada serviço
  (`auth-service`, `bets-service`, `stats-service`) diretamente, autenticado por
  `X-Admin-Api-Key` — 3 chamadas manuais separadas, fora da tabela de roteamento deste serviço.
  Ver `../../docs/API-CONTRACTS.md` e `../../docs/DECISIONS-LOG.md` item 3.
- Erros de autenticação/roteamento seguem `application/problem+json` (RFC 7807) — ver
  `../../docs/API-CONTRACTS.md` — mesmo formato dos demais serviços, para o front-end não
  precisar tratar um formato de erro diferente vindo do Gateway. `title`/`detail` localizados
  por `Accept-Language` (`pt-BR`/`en-US`/`es`, ver `../../docs/CONVENTIONS.md` seção
  "Internacionalização (i18n)") — `type` continua um slug fixo em inglês.
- **Filtro global de `X-Correlation-Id` (`feat-006`, implementado em 2026-09-08)**:
  `CorrelationIdFilter` (`@Order(Ordered.HIGHEST_PRECEDENCE)`, sem `shouldNotFilter`) gera o
  header quando ausente/em branco, propaga quando já vem do cliente, injeta no MDC
  (`correlationId`) e repassa no request roteado via `CorrelationIdRequestWrapper` para
  `auth-service`/`bets-service`/`stats-service` — ver `../../docs/OBSERVABILITY-AND-CONFIG.md`.
  Roda para **toda** rota, autenticada (`feat-002`) ou via `X-Service-Key` (`feat-004`), rodando
  antes dos dois (precedência mais alta) para que os próprios logs de rejeição desses filtros já
  carreguem o correlation id — filtro independente, não acoplado ao de validação PASETO. Também
  ecoa o header na response (decisão além do contrato documentado, não contradiz nada). Lacuna
  encontrada em 2026-09-07 (o backlog original de `feat-001..005` não cobria isso); fechada nesta
  feature. `bets-service` ainda não consome o header real no envelope de evento — gap sinalizado
  em `docs/services/bets-service.md`, fora do escopo deste serviço.
- Roteamento: `/api/v1/users/**`, `/api/v1/auth/**` e `/api/v1/telegram-links/**` →
  `auth-service`; `/api/v1/betting-houses/**`, `/api/v1/bets/**`, `/api/v1/transactions/**` →
  `bets-service`; `/api/v1/statistics/**` → `stats-service`. Rotas sempre em inglês (ver
  `../../docs/API-CONTRACTS.md`) — atualize esta lista e `../../docs/services/api-gateway.md` no
  mesmo commit se novas rotas forem adicionadas. **`/api/v1/telegram-links/**` acrescentada em
  `feat-003`** (achado real): não coberta por nenhum dos outros dois prefixos de `auth-service`,
  apesar de já existir e exigir `X-User-Id`/`X-Tenant-Id` como `/api/v1/users` (`auth-service
  feat-006`) — ausência era lacuna real da tabela de roteamento, não decisão deliberada.
  `/api/v1/telegram-accounts/**` (confirmação via bot e lookup interno) fica fora da tabela até
  `feat-004` (credencial de serviço), que a introduz com autenticação diferente
  (`X-Service-Key`, não PASETO) — não faz sentido essa rota aceitar um caminho de autenticação
  que `feat-003` ainda não implementa.
- **`POST /api/v1/auth/login` é a única rota pública do Gateway** (achado real de `feat-003`,
  corrigido antes de existir tráfego real): o filtro de PASETO (`feat-002`) rodava em toda rota
  não-actuator, e a tabela de rotas antes de `feat-003` estava vazia, então o bug ficou latente —
  a primeira rota real adicionada seria bloqueada para sempre (ninguém consegue logar se o login
  também exige um token PASETO que só o login emite). `PasetoAuthenticationFilter.shouldNotFilter`
  ganhou uma segunda exclusão (`/api/v1/auth/login`, além de `/actuator/**`) — qualquer rota
  pública nova do Gateway segue o mesmo padrão, adicionada à mesma checagem, nunca via
  configuração externa enquanto houver só essa exceção.
- **CORS (`feat-012`, achado real de 2026-09-11)**: `CorsConfig` registra `CorsFilter` (biblioteca
  do Spring) via `FilterRegistrationBean` com `.setOrder(Ordered.HIGHEST_PRECEDENCE)` **explícito**
  — `@Order` direto no método `@Bean` **não** ordena o registro do Filter no Spring Boot (gotcha
  real, ver `../../docs/CONVENTIONS.md` seção "Backend Java"), e sem rodar antes de
  `PasetoAuthenticationFilter`/`ServiceKeyAuthenticationFilter` o preflight `OPTIONS` do navegador
  toma `401` antes do `CorsFilter` responder. Origem(ns) via `CORS_ALLOWED_ORIGINS`
  (`../../docs/OBSERVABILITY-AND-CONFIG.md`), sem `allowCredentials` (token vai em `Authorization`,
  nunca cookie).
- **CI/CD (`feat-005`)**: pipeline em `.github/workflows/ci.yml`, **dentro deste repositório**
  (este serviço é seu próprio repositório Git, não um monorepo — ver
  `../../docs/DECISIONS-LOG.md` "Topologia") — changelog, i18n, build, testes, SonarCloud.
  Scripts de validação em `.github/scripts/` (duplicados aqui, não compartilhados com os outros
  serviços). Ver `../../docs/CI-CD.md`. Toda feature adiciona uma entrada em `CHANGELOG.md`
  deste serviço (verificado automaticamente pelo CI quando este repositório existir no GitHub).
- **Skills de agente prioritárias**: `Plan Reviewer` antes de codificar, `Delivery Reviewer` +
  `Test Suite Auditor` antes de marcar `done` (claude-code-skills) — mapeamento completo em
  `../../docs/AGENT-SKILLS.md`. Instaladas em 2026-08-02 (escopo `user`), ver
  `../../docs/DECISIONS-LOG.md`.

## Definição de pronto (Definition of Done)

Uma feature deste serviço só está `done` quando (done only when):

> **Antes de começar** (não é item de `done`, é pré-requisito de `in-progress`): o campo
> `plan_review` daquela feature em `feature_list.json` precisa estar preenchido com o
> resultado do `Plan Reviewer` — ver `CLAUDE.md` da raiz, seção "Regras de trabalho".


- [ ] Implementada e rodando via `./init.sh` sem erro (`mvn verify`, gate de cobertura incluso).
- [ ] Testes seguindo `../../docs/TESTING.md`: token válido roteia com `X-User-Id` correto, token
      ausente/inválido retorna 401 RFC 7807, `X-Service-Key` inválida retorna 401, nenhuma rota
      aceita `X-User-Id` vindo do cliente, e ao menos um teste confirma `title`/`detail`
      diferentes por `Accept-Language`.
- [ ] `Delivery Reviewer` e `Test Suite Auditor` rodados
      contra a feature (ver `../../docs/AGENT-SKILLS.md`).
- [ ] `CHANGELOG.md` deste serviço tem uma entrada em `[Unreleased]` descrevendo a mudança.
- [ ] `feature_list.json` atualizado com status e evidência.
- [ ] `../../feature_list.json` (raiz) atualizado se este foi o marco que fecha `epic-008`.

## Fim de sessão (End of Session)

Antes de encerrar (before ending a session): atualize `progress.md` deste serviço, atualize
`feature_list.json`, e deixe `./init.sh` passando (clean, restartable state) — stay in scope:
não edite código de outro serviço aqui, mesmo que a dúvida seja sobre um contrato consumido por
ele (ajuste a nota do vault e sinalize, não o código do outro serviço).

## Verificação

```bash
./init.sh
```
