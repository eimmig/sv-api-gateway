# Log de Progresso — api-gateway

## Estado Atual (Current State)

**Última atualização:** 2026-08-01 00:00
**Feature ativa:** nenhuma

## Status

### O que está pronto

- [x] Harness deste serviço criado.

### Em andamento

- Nenhuma feature iniciada.

### Próximos passos (Next Steps)

1. `feat-001` — inicializar o projeto Spring Boot 4.x com Spring Cloud Gateway.

## Bloqueios / Riscos

- Depende de `auth-service` (epic-002) emitir tokens PASETO e expor o endpoint de lookup
  `GET /api/v1/telegram-accounts/{telegramUserId}` antes de `feat-002`/`feat-004` poderem ser
  testados ponta a ponta — `feat-001` pode ser feito de forma independente.

## Decisões tomadas

- Build tool: **Maven**. Framework de roteamento: **Spring Cloud Gateway**. Não segue o layout
  hexagonal dos outros serviços Java (sem domínio de negócio) — decisão registrada em
  `CLAUDE.md` deste serviço e em `../../docs/CONVENTIONS.md`.
- Serviço criado nesta sessão para preencher uma lacuna encontrada em auditoria do harness: a
  documentação (`../../docs/API-CONTRACTS.md`) já descrevia um "API Gateway" como único
  validador de PASETO, mas nenhum epic/serviço/harness existia para ele até agora.
- Autenticação de `telegram-integration` (sem usuário logado) via credencial de serviço
  estática (`X-Service-Key`), não OAuth2 client-credentials completo — proporcional ao escopo
  do TCC (só há um cliente de serviço-a-serviço no sistema hoje).

## Arquivos modificados nesta sessão

- `CLAUDE.md`, `feature_list.json`, `init.sh`, `progress.md`, `session-handoff.md` — criados.

## Evidência de conclusão

- Não aplicável ainda.

## Notas para a próxima sessão

Ver `../../docs/services/api-gateway.md` para a tabela de rotas completa e o fluxo de
resolução `telegramUserId -> userId` antes de começar `feat-004`.
