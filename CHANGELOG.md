# Changelog - API GP Premium

Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/),
e este projeto adere ao [Versionamento Semântico](https://semver.org/lang/pt-BR/).

## [1.2.1] - 2025-01-25

### ✨ Novas Funcionalidades
- **FEATURE**: Implementado soft delete com ativação automática de configuração anterior
  - Atualizado endpoint `DELETE /api/configuracao-maquina/{id}` para ativar automaticamente a configuração anterior
  - Adicionado método `findPreviousActiveBymaquinaIdExcludingId()` no repository
  - Implementada lógica de ativação automática após soft delete
  - Garantia de que sempre há uma configuração ativa (quando existe histórico)

### 🧪 Testes
- Adicionados 4 novos testes para validar soft delete e ativação automática:
  - `testDeletarConfiguracao_ComConfiguracaoExistente_DeveRealizarSoftDelete`
  - `testDeletarConfiguracao_ComConfiguracaoInexistente_DeveRetornarNotFound`
  - `testDeletarConfiguracao_ComConfiguracaoAnteriorExistente_DeveManterConfiguracaoAnteriorAtiva`
  - `testDeletarConfiguracao_SemConfiguracaoAnterior_DeveApenasRealizarSoftDelete`

### 📚 Documentação
- Atualizada documentação em `docs/MULTIPLAS_CONFIGURACOES.md` com seção sobre soft delete
- Documentado comportamento de ativação automática
- Adicionados exemplos de fluxo de exclusão

## [1.2.0] - 2025-01-25

### ✨ Novas Funcionalidades
- **FEATURE**: Implementado suporte a múltiplas configurações por celular
  - Removida constraint única de `celular_id` para permitir múltiplas configurações
  - Adicionado controle de versioning baseado em timestamp (`dt_create`)
  - Implementado conceito de "configuração ativa" (mais recente por celular)
  - Novos endpoints:
    - `GET /api/configuracao-maquina/celular/{celularId}/ativa` - Busca configuração ativa
    - `GET /api/configuracao-maquina/celular/{celularId}/historico` - Lista histórico de configurações
  - Novos métodos no repository:
    - `findActiveByCelularId()` - Busca configuração mais recente por celular
    - `findByCelularIdAndDtDeleteIsNullOrderByDtCreateDesc()` - Lista configurações por celular

### 🗄️ Banco de Dados
- **MIGRATION**: V1.2.0 - Removida constraint única `uq_celular` da tabela `maquina_configuracao`
- Adicionado índice `idx_celular_dt_create` para otimizar consultas por celular e data
- Atualizado comentário da tabela para refletir novo comportamento

### 🧪 Testes
- Adicionados testes unitários para novos endpoints
- Implementados testes para validação de múltiplas configurações por celular
- Testes de busca de configuração ativa e histórico
- Validação de ordenação por data de criação

### 📚 Documentação
- Atualizada documentação da API com novos endpoints
- Documentado novo comportamento de configurações múltiplas
- Adicionados exemplos de uso da nova funcionalidade

## [1.1.0] - 2025-01-25

### 🔧 Correções Críticas
- **HOTFIX**: Corrigido problema de `matriz_id` null em ConfiguracaoMaquinaModel
  - Adicionada validação @NotNull para matriz_id
  - Implementada verificação de existência da matriz antes da criação
  - Adicionados testes unitários para validação
  - Corrigido mapeamento JPA para garantir integridade referencial

### 📚 Documentação
- Criada documentação técnica completa do projeto
  - `VERSION_SUMMARY.md`: Resumo executivo do backend
  - `ARCHITECTURE_EVOLUTION.md`: Evolução arquitetural detalhada
  - `HOTFIX_MATRIZ_ID.md`: Documentação específica da correção crítica
  - `docs/README.md`: Índice da documentação técnica
- Atualizado `README.md` principal com referências à nova documentação

### 🧪 Testes
- Adicionados testes unitários para ConfiguracaoMaquinaController
- Implementados testes de validação para matriz_id
- Criados testes manuais para verificação da correção

### 📊 Status do Projeto
- **Versão**: v1.1 - STABLE
- **Status**: Pronto para produção
- **Cobertura de testes**: Funcionalidades principais validadas
- **Documentação**: Completa e atualizada

## [Não Lançado]

### Corrigido
- **[CRÍTICO]** Correção do erro de `matriz_id` nulo no endpoint de criação de configuração de máquina
  - Adicionada injeção do `MatrizRepository` no `ConfiguracaoMaquinaController`
  - Implementada validação de existência da matriz antes da criação da configuração
  - Corrigida definição do `MatrizModel` na `ConfiguracaoMaquinaModel` antes do save
  - Adicionado tratamento de erro 404 quando matriz não é encontrada
  - **Impacto**: Resolve falha na criação de configurações que resultava em `matriz_id` nulo no banco de dados

### Adicionado
- Validação robusta para verificação de existência de matriz no processo de criação de configuração
- Mensagens de erro mais descritivas para casos de matriz não encontrada
- Testes unitários para validação da correção implementada
- **[DOCUMENTAÇÃO]** Criação da documentação técnica completa do projeto
  - `docs/VERSION_SUMMARY.md` - Resumo executivo da evolução e estado atual
  - `docs/ARCHITECTURE_EVOLUTION.md` - Evolução arquitetural detalhada
  - `docs/HOTFIX_MATRIZ_ID.md` - Documentação da correção crítica
  - `docs/README.md` - Índice de navegação da documentação
  - Atualização do `README.md` principal com referências à documentação técnica
  - **Impacto**: Facilita onboarding de novos desenvolvedores e manutenção do projeto

## Estrutura Atual do Projeto

### Arquitetura
- **Framework**: Spring Boot
- **Banco de Dados**: MySQL (produção) / H2 (testes)
- **Autenticação**: JWT
- **Documentação**: Swagger/OpenAPI 3
- **Testes**: JUnit 5 + Mockito

### Módulos Principais

#### Controllers
- `AutenticacaoController` - Gerenciamento de autenticação e autorização
- `UsuarioController` - CRUD de usuários
- `ConfiguracaoMaquinaController` - **[RECÉM CORRIGIDO]** Gerenciamento de configurações de máquinas
- `ProducaoController` - Controle de produção
- `RegistroMaquinaController` - Registro e controle de máquinas

#### Models
- `UsuarioModel` - Entidade de usuários
- `ConfiguracaoMaquinaModel` - Configurações de máquinas
- `MatrizModel` - Matrizes de produção
- `RegistroMaquinaModel` - Registro de máquinas
- `ProducaoModel` - Dados de produção

#### Repositories
- Implementação de repositórios JPA para todas as entidades
- Queries customizadas para operações específicas
- Soft delete implementado onde necessário

#### DTOs
- DTOs de criação, atualização e resposta para todas as entidades
- Validações usando Bean Validation
- Separação clara entre dados de entrada e saída

### Funcionalidades Implementadas

#### ✅ Autenticação e Autorização
- Login com JWT
- Controle de perfis de usuário
- Middleware de autenticação

#### ✅ Gestão de Usuários
- CRUD completo de usuários
- Associação com perfis
- Soft delete

#### ✅ Configuração de Máquinas **[RECÉM CORRIGIDO]**
- Criação de configurações com validação de matriz
- Associação correta entre configuração, máquina e matriz
- Listagem paginada de configurações
- Atualização e exclusão (soft delete)

#### ✅ Controle de Produção
- Registro de dados de produção
- Associação com máquinas e configurações
- Relatórios e consultas

#### ✅ Registro de Máquinas
- Cadastro e controle de máquinas
- Status e informações técnicas
- Histórico de operações

### Configurações de Ambiente

#### Desenvolvimento
- Perfil `dev` com H2 em memória
- Hot reload habilitado
- Logs detalhados

#### Teste
- Perfil `test` com H2 em memória
- Dados de teste pré-carregados
- Configurações otimizadas para testes

#### Produção
- Perfil `prod` com MySQL
- Configurações de segurança
- Logs otimizados

### Qualidade de Código

#### Padrões Implementados
- Checkstyle configurado
- Padrões de nomenclatura consistentes
- Documentação Swagger completa
- Tratamento de exceções padronizado

#### Testes
- Testes unitários com Mockito
- Testes de integração
- Cobertura de código monitorada

### Banco de Dados

#### Estrutura
- Relacionamentos bem definidos
- Constraints de integridade
- Índices otimizados
- Soft delete implementado

#### Migrações
- Scripts de baseline
- Versionamento de schema
- Dados de exemplo para desenvolvimento

### Segurança

#### Implementações
- Autenticação JWT
- Validação de entrada
- Proteção contra SQL Injection
- CORS configurado
- Headers de segurança

### Monitoramento

#### Endpoints de Saúde
- Actuator configurado
- Health checks
- Métricas de aplicação

## Próximos Passos Sugeridos

### Melhorias de Curto Prazo
1. Implementar cache para consultas frequentes
2. Adicionar logs de auditoria
3. Melhorar cobertura de testes
4. Implementar rate limiting

### Melhorias de Médio Prazo
1. Implementar notificações em tempo real
2. Adicionar métricas de performance
3. Implementar backup automático
4. Adicionar dashboard de monitoramento

### Melhorias de Longo Prazo
1. Migração para arquitetura de microserviços
2. Implementação de Event Sourcing
3. Integração com sistemas externos
4. Implementação de IA/ML para análise preditiva

---

**Última Atualização**: Janeiro 2025
**Versão Atual**: Em desenvolvimento
**Responsável**: Equipe de Desenvolvimento GP Premium