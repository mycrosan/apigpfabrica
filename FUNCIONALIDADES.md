# Funcionalidades do Projeto GP Premium

Este documento apresenta as principais funcionalidades implementadas no sistema GP Premium, considerando arquitetura, módulos, segurança, integrações e recursos técnicos.

## 1. Autenticação e Segurança
- Autenticação via JWT (JSON Web Token)
- Controle de acesso por perfis de usuário
- Criptografia de senha com BCrypt
- Filtro de autenticação customizado
- Proteção contra CSRF e sessões stateless

## 2. Gerenciamento de Usuários
- Cadastro, edição e exclusão de usuários
- Consulta de usuários por filtros
- Controle de permissões e perfis

## 3. Gestão de Máquinas e Configurações
- Cadastro e manutenção de máquinas
- Configuração de parâmetros operacionais
- Registro de atividades e status das máquinas

## 4. Controle de Produção
- Registro de produção por máquina
- Controle de qualidade e validação de regras
- Gestão de insumos (cola, camelback, etc.)

## 5. Gestão de Carcaças e Pneus
- Cadastro e acompanhamento de carcaças
- Registro de pneus vulcanizados
- Controle de rejeições e motivos

## 6. Relatórios e Resumos
- Geração de relatórios de produção, qualidade e rejeições
- Resumo de status por período e máquina

## 7. Integração com Banco de Dados
- Persistência via JPA (MySQL)
- Migração de banco com Flyway
- Perfis para ambiente de desenvolvimento e produção

## 8. API RESTful
- Endpoints organizados por módulos (usuário, máquina, produção, carcaça, etc.)
- Documentação automática via Swagger/OpenAPI
- Validação de dados de entrada

## 9. Upload e Download de Arquivos
- Upload de imagens e documentos
- Download de relatórios e arquivos processados

## 10. Testes Automatizados
- Testes unitários e de integração com JUnit e Spring Security Test
- Banco de dados em memória (H2) para testes

## 11. Monitoramento e Logging
- Logging centralizado com Log4j
- Logs de autenticação, erros e operações críticas

## 12. Qualidade de Código
- Plugins de análise estática: Checkstyle, SpotBugs, JaCoCo
- Uso de Lombok para simplificação de modelos

---

Este documento pode ser expandido conforme novas funcionalidades forem implementadas ou detalhadas.
