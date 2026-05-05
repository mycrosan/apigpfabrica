# Guia de Desenvolvimento - GP Premium

## Setup do Ambiente

### Pré-requisitos
- **Java 11** (OpenJDK ou Oracle JDK)
- **Maven 3.6+**
- **MySQL 8.0+**
- **IDE** com suporte a Lombok (IntelliJ IDEA, Eclipse, VS Code)

### Configuração da IDE

#### IntelliJ IDEA
1. Instalar plugin do Lombok
2. Habilitar "Annotation Processing" em Settings
3. Configurar Checkstyle plugin (opcional)

#### Eclipse
1. Instalar Lombok via jar: `java -jar lombok.jar`
2. Reiniciar Eclipse
3. Verificar se anotações estão sendo processadas

#### VS Code
1. Instalar extensão "Language Support for Java"
2. Instalar extensão "Lombok Annotations Support"

### Configuração do Banco de Dados

```sql
CREATE DATABASE gp_premium;
CREATE USER 'gp_user'@'localhost' IDENTIFIED BY 'gp_password';
GRANT ALL PRIVILEGES ON gp_premium.* TO 'gp_user'@'localhost';
FLUSH PRIVILEGES;
```

### Configuração do Projeto

1. **Clone do repositório**
```bash
git clone [repository-url]
cd apigppremium
```

2. **Configuração do application.properties**
```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/gp_premium
spring.datasource.username=gp_user
spring.datasource.password=gp_password

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# Swagger
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

3. **Compilação e execução**
```bash
mvn clean compile
mvn spring-boot:run
```

4. **Acesso à aplicação**
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## Fluxo de Desenvolvimento

### 1. Antes de Começar
- [ ] Ler [CODING_STANDARDS.md](./CODING_STANDARDS.md)
- [ ] Configurar IDE com Lombok
- [ ] Verificar se testes passam: `mvn test`

### 2. Desenvolvimento de Features

#### Criando uma Nova Entidade
```java
@Entity(name = "exemplo")
@Table(name = "exemplo")
@Schema(description = "Descrição da entidade")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExemploModel {
    // Implementação seguindo padrões
}
```

#### Criando um Controller
```java
@RestController
@RequestMapping("/api/v1/exemplo")
@Tag(name = "Exemplo", description = "Operações de exemplo")
@CrossOrigin(origins = "*")
public class ExemploController {
    // Implementação seguindo padrões
}
```

### 3. Testes
```bash
# Executar todos os testes
mvn test

# Executar testes específicos
mvn test -Dtest=ExemploControllerTest

# Executar com coverage
mvn test jacoco:report
```

### 4. Validação de Código
```bash
# Checkstyle
mvn checkstyle:check

# Compilação
mvn clean compile

# Verificação completa
mvn clean verify
```

## Comandos Úteis

### Maven
```bash
# Limpar e compilar
mvn clean compile

# Executar aplicação
mvn spring-boot:run

# Executar testes
mvn test

# Gerar documentação
mvn javadoc:javadoc

# Verificar dependências
mvn dependency:tree

# Atualizar dependências
mvn versions:display-dependency-updates
```

### Git
```bash
# Criar branch para feature
git checkout -b feature/nome-da-feature

# Commit seguindo padrão
git commit -m "feat: adicionar nova funcionalidade"

# Push da branch
git push origin feature/nome-da-feature
```

## Estrutura de Commits

### Padrão Conventional Commits
```
<tipo>(<escopo>): <descrição>

[corpo opcional]

[rodapé opcional]
```

### Tipos
- `feat`: Nova funcionalidade
- `fix`: Correção de bug
- `docs`: Documentação
- `style`: Formatação
- `refactor`: Refatoração
- `test`: Testes
- `chore`: Tarefas de manutenção

### Exemplos
```
feat(usuario): adicionar endpoint de criação de usuário
fix(auth): corrigir validação de token JWT
docs: atualizar README com instruções de setup
refactor(model): migrar ConfiguracaoMaquinaModel para Lombok
```

## Debugging

### Logs
```java
// Configurar logger
private static final Logger logger = LoggerFactory.getLogger(ClassName.class);

// Usar níveis apropriados
logger.debug("Informação de debug: {}", variable);
logger.info("Operação realizada com sucesso");
logger.warn("Situação que merece atenção");
logger.error("Erro crítico: {}", e.getMessage(), e);
```

### Profiles
```bash
# Desenvolvimento
mvn spring-boot:run -Dspring.profiles.active=dev

# Teste
mvn spring-boot:run -Dspring.profiles.active=test

# Produção
mvn spring-boot:run -Dspring.profiles.active=prod
```

## Troubleshooting

### Problemas Comuns

#### Lombok não funciona
1. Verificar se plugin está instalado
2. Verificar se annotation processing está habilitado
3. Limpar cache da IDE e recompilar

#### Erro de compilação
1. Verificar versão do Java (deve ser 11)
2. Limpar target: `mvn clean`
3. Verificar dependências: `mvn dependency:resolve`

#### Testes falhando
1. Verificar configuração do banco de teste
2. Verificar se migrations estão atualizadas
3. Limpar dados de teste entre execuções

#### Swagger não carrega
1. Verificar se aplicação está rodando
2. Verificar URL: http://localhost:8080/swagger-ui/index.html
3. Verificar logs de erro na aplicação

## Performance

### Monitoramento
- Usar Spring Boot Actuator
- Configurar métricas com Micrometer
- Monitorar queries SQL com logs

### Otimizações
- Usar `@Transactional` adequadamente
- Implementar cache quando necessário
- Usar paginação para listas grandes
- Otimizar queries N+1 com `@EntityGraph`

## Segurança

### Checklist
- [ ] Validar todas as entradas
- [ ] Usar HTTPS em produção
- [ ] Implementar rate limiting
- [ ] Sanitizar dados de saída
- [ ] Usar prepared statements
- [ ] Configurar CORS adequadamente

---

## Links Úteis

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Lombok Documentation](https://projectlombok.org/)
- [Swagger/OpenAPI](https://swagger.io/)
- [JPA/Hibernate](https://hibernate.org/)
- [Maven Documentation](https://maven.apache.org/)

---

**Última atualização**: 28/09/2025