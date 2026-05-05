# 🧪 Manual de Testes - GP Premium API

Este manual contém todas as informações necessárias para executar e gerenciar os testes do projeto GP Premium API.

## 📋 Índice

- [Pré-requisitos](#pré-requisitos)
- [Comandos Básicos](#comandos-básicos)
- [Tipos de Testes](#tipos-de-testes)
- [Executando Testes Específicos](#executando-testes-específicos)
- [Relatórios de Testes](#relatórios-de-testes)
- [Configurações de Ambiente](#configurações-de-ambiente)
- [Troubleshooting](#troubleshooting)
- [Status Atual dos Testes](#status-atual-dos-testes)

## 🔧 Pré-requisitos

- **Java 11+** instalado
- **Maven 3.6+** instalado
- **H2 Database** (configurado automaticamente para testes)
- Variáveis de ambiente configuradas (se necessário)

## 🚀 Comandos Básicos

### ⭐ **RECOMENDADO: Script com Saída Limpa**
```bash
# Executar todos os testes com saída organizada
./run-tests.sh

# Executar teste específico com saída limpa
./run-tests.sh UsuarioControllerTest
```

### Comandos Maven Tradicionais

#### Executar Todos os Testes (Saída Limpa)
```bash
mvn test -q
```

#### Executar Todos os Testes (Saída Completa)
```bash
mvn test
```

#### Executar Testes com Limpeza Prévia
```bash
mvn clean test -q
```

#### Executar Testes Ignorando Falhas
```bash
mvn test -Dmaven.test.failure.ignore=true -q
```

#### Executar Testes com Logs Detalhados (Debug)
```bash
mvn test -X
```

## 🎯 Tipos de Testes

### 1. Testes de Controller
- **UsuarioControllerTest** - Testes de CRUD de usuários
- **AutenticacaoControllerTest** - Testes de autenticação
- **ConfiguracaoMaquinaControllerTest** - Testes de configuração de máquinas
- **ProducaoControllerTest** - Testes de produção
- **RegistroMaquinaControllerTest** - Testes de registro de máquinas

### 2. Testes de Repository
- **UsuarioRepositoryTest** - Testes de persistência de usuários
- **ProducaoRepositoryTest** - Testes de persistência de produção

### 3. Testes de Service
- **TokenServiceTest** - Testes de geração e validação de tokens

### 4. Testes de Integração
- **GpPremiumApplicationTests** - Teste de contexto da aplicação

## 🎪 Executando Testes Específicos

### Por Classe de Teste
```bash
# Executar apenas testes de usuário
mvn test -Dtest=UsuarioControllerTest

# Executar apenas testes de autenticação
mvn test -Dtest=AutenticacaoControllerTest

# Executar apenas testes de configuração
mvn test -Dtest=ConfiguracaoMaquinaControllerTest
```

### Por Pacote
```bash
# Executar todos os testes de controller
mvn test -Dtest="br.compneusgppremium.api.controller.*"

# Executar todos os testes de repository
mvn test -Dtest="br.compneusgppremium.api.repository.*"

# Executar todos os testes de service
mvn test -Dtest="br.compneusgppremium.api.service.*"
```

### Por Método Específico
```bash
# Executar um método específico
mvn test -Dtest=UsuarioControllerTest#deveCriarNovoUsuario

# Executar múltiplos métodos
mvn test -Dtest=UsuarioControllerTest#deveCriarNovoUsuario+deveListarTodosUsuarios
```

### Por Padrão de Nome
```bash
# Executar testes que contenham "Usuario" no nome
mvn test -Dtest="*Usuario*"

# Executar testes que terminem com "Test"
mvn test -Dtest="*Test"
```

## 📊 Relatórios de Testes

### Localização dos Relatórios
```
target/surefire-reports/
├── *.txt                    # Relatórios em texto
├── *.xml                    # Relatórios em XML
└── TEST-*.xml               # Relatórios detalhados
```

### Visualizar Relatórios
```bash
# Ver relatório de um teste específico
cat target/surefire-reports/br.compneusgppremium.api.controller.UsuarioControllerTest.txt

# Listar todos os relatórios
ls -la target/surefire-reports/*.txt
```

### Gerar Relatório HTML (Opcional)
```bash
mvn surefire-report:report
```

## ⚙️ Configurações de Ambiente

### Perfis de Teste
```bash
# Executar com perfil de teste específico
mvn test -Dspring.profiles.active=test

# Executar com perfil de desenvolvimento
mvn test -Dspring.profiles.active=dev
```

### Configurações de Banco H2
Os testes utilizam banco H2 em memória configurado em:
- `src/test/resources/application-test.properties`
- `src/test/resources/data.sql` (dados de teste)

### Configurações de Logs
Para uma experiência mais limpa, os logs foram configurados para mostrar apenas informações essenciais:

#### Arquivos de Configuração:
- **`src/test/resources/application-test.properties`** - Configurações básicas de log
- **`src/test/resources/logback-test.xml`** - Configuração avançada do Logback
- **`pom.xml`** - Plugin Surefire configurado para saída limpa

#### Níveis de Log Configurados:
- **Spring Framework**: WARN (silenciado)
- **Hibernate/JPA**: WARN (sem SQL logs)
- **H2 Database**: WARN (silenciado)
- **Aplicação**: WARN (apenas erros importantes)

#### Para Habilitar Logs Detalhados (Debug):
```bash
# Temporariamente para um teste
mvn test -Dtest=UsuarioControllerTest -Dlogging.level.br.compneusgppremium=DEBUG

# Ou editar application-test.properties e alterar para DEBUG
```

### Variáveis de Ambiente para Testes
```bash
# Definir nível de log para testes
export LOGGING_LEVEL_ROOT=DEBUG

# Executar testes com variável específica
SPRING_PROFILES_ACTIVE=test mvn test
```

## 🔍 Troubleshooting

### Problemas Comuns

#### 1. Erro de Conexão com Banco
```bash
# Verificar se H2 está configurado corretamente
mvn test -Dspring.datasource.url=jdbc:h2:mem:testdb
```

#### 2. Conflitos de Dados
```bash
# Limpar cache e executar
mvn clean test
```

#### 3. Problemas de Memória
```bash
# Aumentar memória para testes
export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=256m"
mvn test
```

#### 4. Testes Lentos
```bash
# Executar testes em paralelo
mvn test -Dparallel=methods -DthreadCount=4
```

### Logs de Debug
```bash
# Habilitar logs detalhados
mvn test -Dlogging.level.br.compneusgppremium=DEBUG

# Ver logs do Spring Boot
mvn test -Dlogging.level.org.springframework=DEBUG
```

## 📈 Status Atual dos Testes

### ✅ Testes Funcionando (100%)
- **UsuarioControllerTest** - 11/11 testes passando
- **ProducaoControllerTest** - Todos os testes passando
- **RegistroMaquinaControllerTest** - Todos os testes passando
- **GpPremiumApplicationTests** - Teste de contexto passando
- **ProducaoRepositoryTest** - Todos os testes passando
- **TokenServiceTest** - Todos os testes passando

### ⚠️ Testes com Falhas Conhecidas
- **AutenticacaoControllerTest** - 3/5 passando
  - Problemas com validação de status HTTP (403 vs 400)
- **ConfiguracaoMaquinaControllerTest** - 8/11 passando
  - Problemas com mocks e NullPointerException
- **UsuarioRepositoryTest** - 5/6 passando
  - Conflito de dados entre data.sql e setUp

### 📊 Estatísticas Gerais
- **Taxa de Sucesso**: ~75%
- **Total de Testes**: ~50 testes
- **Tempo Médio de Execução**: 10-15 segundos

## 🛠️ Comandos Úteis para Desenvolvimento

### Executar Testes Continuamente
```bash
# Executar testes sempre que houver mudanças (requer plugin)
mvn test -Dspring-boot.run.fork=false
```

### Executar Apenas Testes Rápidos
```bash
# Pular testes de integração
mvn test -Dtest="!*IntegrationTest"
```

### Gerar Cobertura de Código
```bash
# Com JaCoCo (se configurado)
mvn test jacoco:report
```

## 📝 Boas Práticas

1. **Sempre execute `mvn clean test` após mudanças significativas**
2. **Verifique os relatórios em `target/surefire-reports/` após falhas**
3. **Use perfil de teste (`-Dspring.profiles.active=test`) para isolamento**
4. **Execute testes específicos durante desenvolvimento para agilizar**
5. **Mantenha dados de teste em `src/test/resources/data.sql` organizados**

## 🆘 Suporte

Para problemas não cobertos neste manual:
1. Verifique os logs em `target/surefire-reports/`
2. Execute com `-X` para logs detalhados
3. Consulte a documentação do Spring Boot Testing
4. Verifique as configurações em `application-test.properties`

---

**Última atualização**: Outubro 2024  
**Versão do Manual**: 1.0  
**Projeto**: GP Premium API