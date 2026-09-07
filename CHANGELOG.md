# Changelog

Todas as mudanças notáveis deste serviço são documentadas neste arquivo. Formato baseado em
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/). Toda feature que altera este
serviço adiciona uma entrada em `[Unreleased]` — verificado automaticamente pela pipeline de CI
(ver `docs/CI-CD.md`).

## [Unreleased]

### Fixed

- Chave do projeto no SonarCloud corrigida para `eimmig_sv-api-gateway`. O SonarCloud gera a chave como
  `<org>_<repo>` ao importar um repositório do GitHub; a forma sem prefixo, usada até aqui, faria a
  análise falhar com projeto inexistente.
- [SV-147](https://stakevault.atlassian.net/browse/SV-147) - Setup do projeto
- [SV-148](https://stakevault.atlassian.net/browse/SV-148) - Bootstrap do pom.xml e esqueleto de pacotes
- [SV-149](https://stakevault.atlassian.net/browse/SV-149) - Gate de cobertura JaCoCo 80%
- [SV-150](https://stakevault.atlassian.net/browse/SV-150) - Scaffold de i18n (MessageSource) e teste smoke
- [SV-151](https://stakevault.atlassian.net/browse/SV-151) - Health checks do Actuator
- [SV-152](https://stakevault.atlassian.net/browse/SV-152) - Logging estruturado
- [SV-153](https://stakevault.atlassian.net/browse/SV-153) - CHANGELOG e verificacao final
