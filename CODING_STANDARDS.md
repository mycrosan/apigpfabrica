# Padrões de Codificação - GP Premium

## Informações do Projeto
- **Framework**: Spring Boot 2.4.4
- **Versão Java**: 11
- **Banco de Dados**: MySQL com JPA/Hibernate
- **Documentação**: Swagger/OpenAPI 3
- **Redução de Boilerplate**: Lombok 1.18.20

## 1. Estrutura de Pacotes

```
br.compneusgppremium.api/
├── config/           # Configurações do Spring
├── controller/       # Controllers REST
│   ├── dto/         # Data Transfer Objects
│   └── model/       # Entidades JPA
├── repository/       # Repositórios JPA
├── service/         # Lógica de negócio
└── util/            # Classes utilitárias
```

## 2. Convenções de Nomenclatura

### Classes
- **Entidades**: Sufixo `Model` (ex: `UsuarioModel`, `ConfiguracaoMaquinaModel`)
- **DTOs**: Sufixo `DTO` (ex: `UsuarioDTO`, `ProducaoFilterDTO`)
- **Controllers**: Sufixo `Controller` (ex: `UsuarioController`)
- **Services**: Sufixo `Service` (ex: `UsuarioService`)
- **Repositories**: Sufixo `Repository` (ex: `UsuarioRepository`)

### Métodos e Variáveis
- **camelCase** para métodos e variáveis
- **Métodos booleanos**: Prefixo `is`, `has`, `can` (ex: `isDeleted()`, `hasPermission()`)
- **Getters/Setters**: Gerados automaticamente pelo Lombok

### Constantes
- **UPPER_SNAKE_CASE** para constantes (ex: `MAX_RETRY_ATTEMPTS`)

## 3. Uso do Lombok

### Anotações Obrigatórias para Entidades
```java
@Data                    // Gera getters, setters, toString, equals, hashCode
@NoArgsConstructor      // Construtor sem argumentos (obrigatório para JPA)
@AllArgsConstructor     // Construtor com todos os argumentos
```

### Anotações para DTOs
```java
@Data                    // Para DTOs simples
@AllArgsConstructor     // Quando necessário construtor completo
```

### Evitar
- Getters/setters manuais quando Lombok está disponível
- Construtores manuais que conflitem com anotações Lombok

## 4. Entidades JPA

### Estrutura Padrão
```java
@Entity(name = "nome_tabela")
@Table(name = "nome_tabela")
@Schema(description = "Descrição da entidade")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExemploModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "ID único", example = "1")
    private Long id;
    
    // Campos obrigatórios de auditoria
    @Column(name = "dt_create", nullable = false)
    @Schema(description = "Data de criação")
    private LocalDateTime dtCreate;
    
    @Column(name = "dt_update")
    @Schema(description = "Data de atualização")
    private LocalDateTime dtUpdate;
    
    @Column(name = "dt_delete")
    @Schema(description = "Data de exclusão (soft delete)")
    private LocalDateTime dtDelete;
    
    // Métodos de ciclo de vida
    @PrePersist
    protected void onCreate() {
        dtCreate = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        dtUpdate = LocalDateTime.now();
    }
    
    // Métodos de negócio
    public void softDelete() {
        this.dtDelete = LocalDateTime.now();
    }
    
    public boolean isDeleted() {
        return dtDelete != null;
    }
}
```

### Relacionamentos
```java
// Many-to-One
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "usuario_id", nullable = false)
@Schema(description = "Usuário associado")
private UsuarioModel usuario;

// One-to-Many
@OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
@Schema(description = "Lista de configurações")
private List<ConfiguracaoModel> configuracoes;
```

## 5. Controllers REST

### Estrutura Padrão
```java
@RestController
@RequestMapping("/api/v1/exemplo")
@Tag(name = "Exemplo", description = "Operações relacionadas a exemplo")
@CrossOrigin(origins = "*")
public class ExemploController {
    
    @Autowired
    private ExemploService exemploService;
    
    @GetMapping
    @Operation(summary = "Listar todos os exemplos")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista recuperada com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro interno do servidor")
    })
    public ResponseEntity<List<ExemploDTO>> listarTodos() {
        // implementação
    }
}
```

### Mapeamentos HTTP
- **GET**: Recuperação de dados
- **POST**: Criação de recursos
- **PUT**: Atualização completa
- **PATCH**: Atualização parcial
- **DELETE**: Remoção (preferencialmente soft delete)

## 6. Documentação Swagger

### Obrigatório para todas as APIs
```java
@Tag(name = "Nome do Controller", description = "Descrição das operações")
@Operation(summary = "Resumo da operação", description = "Descrição detalhada")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Sucesso"),
    @ApiResponse(responseCode = "400", description = "Requisição inválida"),
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado"),
    @ApiResponse(responseCode = "500", description = "Erro interno")
})
@Schema(description = "Descrição do campo", example = "Exemplo de valor")
```

## 7. Tratamento de Erros

### Padrão de Response
```java
// Sucesso
return ResponseEntity.ok(data);

// Criação
return ResponseEntity.status(HttpStatus.CREATED).body(data);

// Não encontrado
return ResponseEntity.notFound().build();

// Erro de validação
return ResponseEntity.badRequest().body(errorMessage);
```

## 8. Validações

### Bean Validation
```java
@NotNull(message = "Campo obrigatório")
@NotBlank(message = "Campo não pode estar vazio")
@Size(min = 3, max = 255, message = "Tamanho deve estar entre 3 e 255 caracteres")
@Email(message = "Email inválido")
@Pattern(regexp = "regex", message = "Formato inválido")
```

## 9. Configurações de Banco

### Soft Delete Padrão
- Sempre implementar soft delete com campo `dt_delete`
- Método `softDelete()` para marcar como deletado
- Método `isDeleted()` para verificar status

### Auditoria
- Campos obrigatórios: `dt_create`, `dt_update`, `dt_delete`
- Usar `@PrePersist` e `@PreUpdate` para automação

## 10. Testes

### Estrutura de Testes
```java
@DataJpaTest                    // Para testes de repositório
@WebMvcTest                     // Para testes de controller
@SpringBootTest                 // Para testes de integração
```

### Nomenclatura
- Métodos de teste: `should_ReturnExpectedResult_When_ConditionMet()`
- Classes de teste: `ClasseTesteTest`

## 11. Boas Práticas Gerais

### Imports
- Organizar imports automaticamente
- Evitar imports com `*`
- Agrupar imports: Java, Spring, terceiros, projeto

### Comentários
- Javadoc para métodos públicos
- Comentários inline apenas quando necessário
- Documentação Swagger sempre presente

### Performance
- Usar `FetchType.LAZY` por padrão
- Implementar paginação para listas grandes
- Usar DTOs para transferência de dados

### Segurança
- Validar todas as entradas
- Usar HTTPS em produção
- Implementar autenticação JWT
- Sanitizar dados de entrada

## 12. Ferramentas de Qualidade

### Recomendadas
- **SonarQube**: Análise de qualidade de código
- **SpotBugs**: Detecção de bugs
- **Checkstyle**: Verificação de estilo
- **PMD**: Análise estática

### Configuração Maven
```xml
<plugin>
    <groupId>org.sonarsource.scanner.maven</groupId>
    <artifactId>sonar-maven-plugin</artifactId>
</plugin>
```

## 13. Versionamento de API

### Padrão
- URL: `/api/v1/recurso`
- Versionamento semântico para releases
- Manter compatibilidade com versões anteriores

## 14. Logs

### Níveis
- **ERROR**: Erros que impedem funcionamento
- **WARN**: Situações que merecem atenção
- **INFO**: Informações importantes do fluxo
- **DEBUG**: Informações detalhadas para desenvolvimento

### Formato
```java
private static final Logger logger = LoggerFactory.getLogger(ClassName.class);

logger.info("Usuário {} criado com sucesso", usuario.getId());
logger.error("Erro ao processar requisição: {}", e.getMessage(), e);
```

---

## Checklist de Code Review

- [ ] Lombok usado corretamente
- [ ] Documentação Swagger completa
- [ ] Soft delete implementado
- [ ] Validações adequadas
- [ ] Tratamento de erros
- [ ] Testes unitários
- [ ] Nomenclatura consistente
- [ ] Performance considerada
- [ ] Segurança validada

---

**Última atualização**: 28/09/2025
**Versão**: 1.0