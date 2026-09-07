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
