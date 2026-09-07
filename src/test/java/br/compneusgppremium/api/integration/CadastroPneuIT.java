package br.compneusgppremium.api.integration;

import br.compneusgppremium.api.controller.model.MarcaModel;
import br.compneusgppremium.api.controller.model.MedidaModel;
import br.compneusgppremium.api.controller.model.ModeloModel;
import br.compneusgppremium.api.controller.model.PaisModel;
import br.compneusgppremium.api.controller.model.StatusCarcacaModel;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import br.compneusgppremium.api.repository.MarcaRepository;
import br.compneusgppremium.api.repository.MedidaRepository;
import br.compneusgppremium.api.repository.ModeloRepository;
import br.compneusgppremium.api.repository.PaisRepository;
import br.compneusgppremium.api.repository.StatusCarcacaRepository;
import br.compneusgppremium.api.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
class CadastroPneuIT {
    @Container
    static final MySQLContainer<?> BANCO = new MySQLContainer<>("mysql:8.4.6");
    @DynamicPropertySource
    static void banco(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", BANCO::getJdbcUrl);
        registry.add("spring.datasource.username", BANCO::getUsername);
        registry.add("spring.datasource.password", BANCO::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.defer-datasource-initialization", () -> false);
        registry.add("spring.sql.init.mode", () -> "never");
    }
    @Autowired MockMvc mvc;
    @Autowired MarcaRepository marcas;
    @Autowired ModeloRepository modelos;
    @Autowired MedidaRepository medidas;
    @Autowired PaisRepository paises;
    @Autowired UsuarioRepository usuarios;
    @Autowired StatusCarcacaRepository statusCarcacas;
    private Integer marcaId;
    private Integer modeloId;
    private Integer medidaId;
    private Integer paisId;
    private RequestPostProcessor autenticado;
    @BeforeEach
    void preparar() {
        MarcaModel marca = new MarcaModel(); marca.setDescricao("Marca IT"); marcaId = marcas.save(marca).getId();
        ModeloModel modelo = new ModeloModel(); modelo.setDescricao("Modelo IT"); modelo.setMarca(marca);
        modeloId = modelos.save(modelo).getId();
        MedidaModel medida = new MedidaModel(); medida.setDescricao("205/55R16"); medidaId = medidas.save(medida).getId();
        PaisModel pais = new PaisModel(); pais.setDescricao("Brasil"); paisId = paises.save(pais).getId();
        if (!statusCarcacas.existsById(1)) {
            StatusCarcacaModel inicial = new StatusCarcacaModel(); inicial.setDescricao("Inicial"); statusCarcacas.save(inicial);
        }
        UsuarioModel usuario = new UsuarioModel(); usuario.setLogin("operador-" + java.util.UUID.randomUUID());
        usuario.setNome("Operador IT"); usuario.setSenha("nao-utilizada-no-teste"); usuarios.save(usuario);
        autenticado = authentication(new UsernamePasswordAuthenticationToken(usuario, null, java.util.List.of()));
    }
    private String entrada(final Integer marca, final String dot) {
        return """
                {"numero_etiqueta":"IT-%s","marca":{"id":%d},"modelo":{"id":%d},
                 "medida":{"id":%d},"pais":{"id":%d},"dot":"%s"}
                """.formatted(java.util.UUID.randomUUID(), marca, modeloId, medidaId, paisId, dot);
    }
    @Test
    void rejeitaMarcaIncompativelMesmoComConfirmacao() throws Exception {
        mvc.perform(post("/api/carcaca?confirmarCombinacaoNova=true").with(autenticado)
                .contentType("application/json").content(entrada(marcaId + 99, "2323")))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("MARCA_MODELO_INCOMPATIVEL"));
    }
    @Test
    void rejeitaSemanaInvalidaMesmoComConfirmacao() throws Exception {
        mvc.perform(post("/api/carcaca?confirmarCombinacaoNova=true").with(autenticado)
                .contentType("application/json").content(entrada(marcaId, "5426")))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("DOT_INVALIDO"));
    }
    @Test
    void persisteComDtoSemExporSenha() throws Exception {
        mvc.perform(post("/api/carcaca?confirmarCombinacaoNova=true").with(autenticado)
                .contentType("application/json").content(entrada(marcaId, "2323")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.modelo.marca.id").value(marcaId))
                .andExpect(jsonPath("$.criadoPor.senha").doesNotExist());
    }
    @Test
    void impedeCamposAusentesMesmoComConfirmacao() throws Exception {
        mvc.perform(post("/api/carcaca?confirmarCombinacaoNova=true").with(autenticado)
                .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void edicaoInvalidaPreservaValorAnterior() throws Exception {
        String resposta = mvc.perform(post("/api/carcaca?confirmarCombinacaoNova=true").with(autenticado)
                .contentType("application/json").content(entrada(marcaId, "2323")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resposta);
        Integer id = json.get("id").asInt();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/carcaca/" + id)
                .param("confirmarCombinacaoNova", "true").with(autenticado)
                .contentType("application/json").content(entrada(marcaId, "0026")))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/carcaca/" + id)
                .with(autenticado)).andExpect(jsonPath("$.dot").value("2323"));
    }

    @Test
    void repositorioNaoOfereceEscritaDiretaPorPatch() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/carcaca/1")
                .with(autenticado).contentType("application/json").content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
