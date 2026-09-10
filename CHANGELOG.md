# Changelog

Cada linha de `[Unreleased]` é um link para a issue do Jira que a gerou (story ou subtask),
formato `- [chave](url) - título` — sem prosa, sem categoria. Escrita automaticamente por
`tools/jira_story.py` no momento em que a issue é criada (ver `docs/CI-CD.md` seção "Changelog
por serviço"). O "porquê" de cada mudança vive na issue e na mensagem de commit, não aqui.

## [Unreleased]

- [SV-9](https://stakevault.atlassian.net/browse/SV-9) - Alinhar as chaves de projeto ao padrão do SonarCloud
- [SV-147](https://stakevault.atlassian.net/browse/SV-147) - Setup do projeto
- [SV-148](https://stakevault.atlassian.net/browse/SV-148) - Endurecer pipeline de CI e bootstrap do pom.xml
- [SV-149](https://stakevault.atlassian.net/browse/SV-149) - Gate de cobertura JaCoCo 80%
- [SV-150](https://stakevault.atlassian.net/browse/SV-150) - Scaffold de i18n (MessageSource) e teste smoke
- [SV-151](https://stakevault.atlassian.net/browse/SV-151) - Health checks do Actuator
- [SV-152](https://stakevault.atlassian.net/browse/SV-152) - Logging estruturado
- [SV-153](https://stakevault.atlassian.net/browse/SV-153) - CHANGELOG e verificacao final
- [SV-154](https://stakevault.atlassian.net/browse/SV-154) - Validacao de token PASETO e injecao de X-User-Id/X-Tenant-Id
- [SV-155](https://stakevault.atlassian.net/browse/SV-155) - Dependencia paseto4j, .env.example e profile de teste
- [SV-156](https://stakevault.atlassian.net/browse/SV-156) - Filtro de validacao PASETO e injecao de X-User-Id/X-Tenant-Id
- [SV-157](https://stakevault.atlassian.net/browse/SV-157) - Testes do filtro e do wrapper de headers
- [SV-158](https://stakevault.atlassian.net/browse/SV-158) - CHANGELOG e verificacao final
- [SV-165](https://stakevault.atlassian.net/browse/SV-165) - Roteamento para auth-service, bets-service e stats-service
- [SV-166](https://stakevault.atlassian.net/browse/SV-166) - URLs de destino configuraveis (.env.example, application.yml)
- [SV-167](https://stakevault.atlassian.net/browse/SV-167) - RouterFunction beans + correcao do bypass de login (achado real) + testes de roteamento
- [SV-168](https://stakevault.atlassian.net/browse/SV-168) - CHANGELOG, atualizacao do vault (rota telegram-links + login publico) e verificacao final
- [SV-169](https://stakevault.atlassian.net/browse/SV-169) - Credencial de servico (X-Service-Key) para telegram-integration
- [SV-170](https://stakevault.atlassian.net/browse/SV-170) - ServiceKeyAuthenticationFilter (validacao de chave, lookup em auth-service, injecao de identidade) + testes
- [SV-171](https://stakevault.atlassian.net/browse/SV-171) - CHANGELOG, atualizacao do vault (header X-Telegram-User-Id) e verificacao final
- [SV-176](https://stakevault.atlassian.net/browse/SV-176) - Filtro global de X-Correlation-Id
- [SV-177](https://stakevault.atlassian.net/browse/SV-177) - CorrelationIdFilter + CorrelationIdRequestWrapper + testes
- [SV-178](https://stakevault.atlassian.net/browse/SV-178) - CHANGELOG, atualizacao do vault (nota de bets-service sobre correlationId pendente) e verificacao final
- [SV-179](https://stakevault.atlassian.net/browse/SV-179) - Pipeline de CI (GitHub Actions + SonarCloud)
- [SV-180](https://stakevault.atlassian.net/browse/SV-180) - Fechamento formal - description corrigida e verificacao final
- [SV-195](https://stakevault.atlassian.net/browse/SV-195) - Rotear catalogos (/api/v1/sports, /leagues, /markets) para bets-service
- [SV-196](https://stakevault.atlassian.net/browse/SV-196) - Rotear /sports, /leagues, /markets para bets-service + testes + CHANGELOG e verificacao final
- [SV-274](https://stakevault.atlassian.net/browse/SV-274) - Rotear /api/v1/tipsters para bets-service
- [SV-275](https://stakevault.atlassian.net/browse/SV-275) - Rotear /api/v1/tipsters para bets-service + teste + CHANGELOG e verificacao final
- [SV-282](https://stakevault.atlassian.net/browse/SV-282) - Dockerfile para imagem de producao
- [SV-283](https://stakevault.atlassian.net/browse/SV-283) - Dockerfile multi-stage + verificacao real do container contra a infra
- [SV-313](https://stakevault.atlassian.net/browse/SV-313) - Extrair role do PASETO e injetar X-User-Role
- [SV-314](https://stakevault.atlassian.net/browse/SV-314) - PasetoClaims/ResolvedIdentityRequestWrapper/PasetoAuthenticationFilter ganham role
