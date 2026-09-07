package br.compneusgppremium.api.integration;

import br.compneusgppremium.api.controller.model.*;
import br.compneusgppremium.api.controller.dto.ResultadoLeituraDTO;
import br.compneusgppremium.api.repository.*;
import br.compneusgppremium.api.leitura.repository.*;
import br.compneusgppremium.api.service.LeituraCarcacaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Sem transação de teste: cada requisição precisa persistir mesmo após falha do OCR. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class SessaoLeituraIT {
    @Container static final MySQLContainer<?> BANCO = new MySQLContainer<>("mysql:8.4.6");
    private static final Path EVIDENCIAS = criarDiretorio();
    private static Path criarDiretorio() {
        try { return Files.createTempDirectory("fabrica-evidencias-it-"); }
        catch (java.io.IOException erro) { throw new IllegalStateException(erro); }
    }
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
        registry.add("pneus.evidencias.diretorio", EVIDENCIAS::toString);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired MarcaRepository marcas;
    @Autowired ModeloRepository modelos;
    @Autowired MedidaRepository medidas;
    @Autowired PaisRepository paises;
    @Autowired ConfirmacaoLeituraRepository confirmacoes;
    @Autowired SessaoLeituraRepository sessoes;
    @Autowired ExecucaoLeituraRepository execucoes;
    @Autowired TentativaLeituraRepository tentativas;
    @Autowired br.compneusgppremium.api.leitura.service.TentativaPersistenciaService persistencia;
    @Autowired CarcacaRepository carcacas;
    @MockitoBean LeituraCarcacaService leitura;
    private RequestPostProcessor operador;
    private Integer operadorId;
    private Integer marcaId;
    private Integer modeloId;
    private Integer medidaId;
    private Integer paisId;
    private String foto;

    @BeforeEach
    void preparar() throws Exception {
        var usuario = usuario(); operadorId = usuario.getId(); operador = autenticar(usuario);
        var marca = new MarcaModel(); marca.setDescricao("Marca " + UUID.randomUUID()); marcas.save(marca); marcaId = marca.getId();
        var modelo = new ModeloModel(); modelo.setDescricao("Modelo IT"); modelo.setMarca(marca); modelos.save(modelo); modeloId = modelo.getId();
        var medida = new MedidaModel(); medida.setDescricao("205/55R16"); medidas.save(medida); medidaId = medida.getId();
        var pais = new PaisModel(); pais.setDescricao("Brasil"); paises.save(pais); paisId = pais.getId();
        var bytes = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "png", bytes);
        foto = Base64.getEncoder().encodeToString(bytes.toByteArray());
    }
    private UsuarioModel usuario() {
        var usuario = new UsuarioModel(); usuario.setNome("Operador IT"); usuario.setLogin(UUID.randomUUID().toString());
        usuario.setSenha("nao-utilizada"); return usuarios.save(usuario);
    }
    private RequestPostProcessor autenticar(final UsuarioModel usuario) {
        return authentication(new UsernamePasswordAuthenticationToken(usuario, null, List.of()));
    }
    private JsonNode corpo(final ResultActions resultado) throws Exception {
        return json.readTree(resultado.andReturn().getResponse().getContentAsString());
    }
    private JsonNode abrir(final String chave) throws Exception {
        return corpo(mvc.perform(post("/api/v2/leitura-sessoes").with(operador).header("Idempotency-Key", chave)
                .contentType("application/json").content("{}" )).andExpect(status().isCreated()));
    }
    private Map<String, Object> entradaTentativa() {
        return Map.of("campo", "MARCA", "foto_base64", foto, "versaoSessao", 0);
    }
    private ResultActions tentar(final String sessao, final String chave, final Map<String, Object> entrada) throws Exception {
        return mvc.perform(post("/api/v2/leitura-sessoes/" + sessao + "/tentativas").with(operador)
                .header("Idempotency-Key", chave).contentType("application/json").content(json.writeValueAsBytes(entrada)));
    }
    private JsonNode confirmar(final JsonNode sessao, final String campo, final Integer item, final String valor) throws Exception {
        var entrada = new LinkedHashMap<String, Object>(); entrada.put("campo", campo); entrada.put("itemFinalId", item);
        entrada.put("valorFinal", valor); entrada.put("origem", "MANUAL"); entrada.put("motivo", "Conferência visual");
        entrada.put("versaoSessao", sessao.get("versao").asLong()); entrada.put("operadorId", -999);
        return corpo(mvc.perform(post("/api/v2/leitura-sessoes/" + sessao.get("id").asText() + "/confirmacoes")
                .with(operador).contentType("application/json").content(json.writeValueAsBytes(entrada))).andExpect(status().isOk()));
    }
    private ResultActions abandonar(final String sessao, final String motivo, final long versao) throws Exception {
        return mvc.perform(post("/api/v2/leitura-sessoes/" + sessao + "/abandono").with(operador)
                .contentType("application/json")
                .content(json.writeValueAsBytes(Map.of("motivo", motivo, "versaoSessao", versao))));
    }
    @Test
    void falhaTecnicaPreservaFotoETentativaSemDuplicarInferencia() throws Exception {
        when(leitura.analisarComEvidencia(any(), anyString(), any(), any())).thenThrow(new IllegalStateException("motor offline"));
        String sessao = abrir(UUID.randomUUID().toString()).get("id").asText();
        JsonNode tentativa = corpo(tentar(sessao, "reenvio", entradaTentativa()).andExpect(status().isServiceUnavailable()));
        tentar(sessao, "reenvio", entradaTentativa()).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.tentativaId").value(tentativa.get("tentativaId").asText()));
        mvc.perform(get("/api/v2/leitura-sessoes/" + sessao).with(operador)).andExpect(status().isOk())
                .andExpect(jsonPath("$.tentativas.length()").value(1)).andExpect(jsonPath("$.tentativas[0].estado").value("ERRO_TECNICO"));
        byte[] armazenada = mvc.perform(get("/api/v2/leitura-imagens/" + tentativa.get("imagemId").asText()).with(operador))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(armazenada).isEqualTo(Base64.getDecoder().decode(foto));
        verify(leitura, times(1)).analisarComEvidencia(any(), anyString(), any(), any());
    }
    @Test
    void impedeReutilizarChaveComConteudoDiferente() throws Exception {
        when(leitura.analisarComEvidencia(any(), anyString(), any(), any())).thenThrow(new IllegalStateException());
        String sessao = abrir(UUID.randomUUID().toString()).get("id").asText();
        tentar(sessao, "chave", entradaTentativa()).andExpect(status().isServiceUnavailable());
        var alterada = new LinkedHashMap<>(entradaTentativa()); alterada.put("campo", "DOT");
        tentar(sessao, "chave", alterada).andExpect(status().isConflict());
        verify(leitura, times(1)).analisarComEvidencia(any(), anyString(), any(), any());
    }
    @Test
    void outroOperadorNaoAcessaSessaoOuImagem() throws Exception {
        when(leitura.analisarComEvidencia(any(), anyString(), any(), any())).thenThrow(new IllegalStateException());
        String sessao = abrir(UUID.randomUUID().toString()).get("id").asText();
        var tentativa = corpo(tentar(sessao, "chave", entradaTentativa()));
        var outro = autenticar(usuario());
        mvc.perform(get("/api/v2/leitura-sessoes/" + sessao).with(outro)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v2/leitura-imagens/" + tentativa.get("imagemId").asText()).with(outro)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v2/leitura-sessoes").with(outro).header("Idempotency-Key", sessao)
                .contentType("application/json").content("{}")).andExpect(status().isForbidden());
    }
    @Test
    void trocaMarcaInvalidaDependentesEPreservaEventos() throws Exception {
        var sessao = abrir(UUID.randomUUID().toString());
        sessao = confirmar(sessao, "MARCA", marcaId, null);
        sessao = confirmar(sessao, "MODELO", modeloId, null);
        sessao = confirmar(sessao, "MEDIDA", medidaId, null);
        sessao = confirmar(sessao, "PAIS", paisId, null);
        var outra = new MarcaModel(); outra.setDescricao("Outra"); marcas.save(outra);
        var atual = confirmar(sessao, "MARCA", outra.getId(), null);
        assertThat(atual.get("confirmacoes").size()).isEqualTo(1);
        assertThat(atual.get("versao").asLong()).isEqualTo(5);
        assertThat(confirmacoes.findAll().stream().filter(evento -> evento.getSessaoId().equals(atual.get("id").asText())))
                .hasSize(5).allMatch(evento -> evento.getOperadorId().equals(operadorId));
        final var desatualizada = sessao;
        mvc.perform(post("/api/v2/leitura-sessoes/" + sessao.get("id").asText() + "/confirmacoes").with(operador)
                .contentType("application/json").content(json.writeValueAsBytes(Map.of("campo", "DOT", "valorFinal", "2323",
                        "origem", "MANUAL", "motivo", "Conferência", "versaoSessao", desatualizada.get("versao").asLong()))))
                .andExpect(status().isConflict());
    }
    @Test
    void rejeitaImagemInvalidaAntesDeInferencia() throws Exception {
        String sessao = abrir(UUID.randomUUID().toString()).get("id").asText();
        var entrada = new LinkedHashMap<>(entradaTentativa()); entrada.put("foto_base64", "bmFvLWltYWdlbQ==");
        tentar(sessao, "invalida", entrada).andExpect(status().isBadRequest());
        verifyNoInteractions(leitura);
    }
    @Test
    void cadastroEReaberturaSaoIdempotentesAposVinculo() throws Exception {
        String chave = UUID.randomUUID().toString(); var sessao = abrir(chave);
        sessao = confirmar(sessao, "MARCA", marcaId, null);
        sessao = confirmar(sessao, "MODELO", modeloId, null);
        sessao = confirmar(sessao, "MEDIDA", medidaId, null);
        sessao = confirmar(sessao, "PAIS", paisId, null);
        sessao = confirmar(sessao, "DOT", null, "2323");
        String etiqueta = "SESSAO-" + UUID.randomUUID();
        var entrada = Map.of("sessaoId", chave, "versaoSessao", sessao.get("versao").asLong(),
                "numeroEtiqueta", etiqueta, "confirmarCombinacaoNova", true, "motivoCombinacaoNova", "Combinação conferida");
        var primeiro = corpo(mvc.perform(post("/api/v2/carcacas").with(operador).header("Idempotency-Key", "salvar")
                .contentType("application/json").content(json.writeValueAsBytes(entrada))).andExpect(status().isCreated()));
        mvc.perform(post("/api/v2/carcacas").with(operador).header("Idempotency-Key", "salvar")
                .contentType("application/json").content(json.writeValueAsBytes(entrada)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(primeiro.get("id").asInt()));
        assertThat(abrir(chave).get("status").asText()).isEqualTo("VINCULADA");
        assertThat(carcacas.buscarEtiqueta(etiqueta)).hasSize(1);
        assertThat(sessoes.findById(chave).orElseThrow().getMotivoCombinacaoNova()).isEqualTo("Combinação conferida");
    }
    @Test
    void persisteExtracaoOriginalMesmoAposCorrecao() throws Exception {
        var resultado = new ResultadoLeituraDTO("MARCA", "SUGESTAO", List.of("CONFIRMACAO_OBRIGATORIA"),
                "Marca observada", marcaId, "Marca observada", List.of(), 0.99, null, "Confira", "sha256-pesos-teste");
        var extracao = new br.compneusgppremium.api.ocr.OcrResposta("PADDLEOCR", "3.3.2", "sha256-pesos-teste",
                "MARCA", List.of(new br.compneusgppremium.api.ocr.OcrResposta.Linha("Marca observada", 0.99,
                List.of(List.of(0, 0), List.of(10, 0), List.of(10, 10), List.of(0, 10)))), 100);
        when(leitura.analisarComEvidencia(any(), anyString(), any(), any())).thenReturn(
                new br.compneusgppremium.api.leitura.dto.ExecucaoLeituraDTO(resultado, extracao,
                        Map.of(marcaId, "Marca observada"), "pre-v1", "normal-v1", "politica-v1", 0.9, true,
                        new br.compneusgppremium.api.leitura.dto.IdentificacaoExecucaoDTO("teste", "hash-teste")));
        var sessao = abrir(UUID.randomUUID().toString());
        var tentativa = corpo(tentar(sessao.get("id").asText(), "primeira", entradaTentativa()).andExpect(status().isOk()));
        var entrada = Map.of("campo", "MARCA", "tentativaId", tentativa.get("tentativaId").asText(), "itemFinalId", marcaId,
                "origem", "CORRIGIDA", "motivo", "Grafia conferida", "versaoSessao", 0);
        mvc.perform(post("/api/v2/leitura-sessoes/" + sessao.get("id").asText() + "/confirmacoes").with(operador)
                .contentType("application/json").content(json.writeValueAsBytes(entrada))).andExpect(status().isOk());
        var evidencia = execucoes.findById(tentativa.get("tentativaId").asText()).orElseThrow();
        assertThat(evidencia.getSnapshotJson()).contains("Marca observada", "sha256-pesos-teste", "regiao", "politica-v1");
        assertThat(evidencia.getHashCatalogo()).hasSize(64);
    }
    @Test
    void falhaDeLeituraPreservaExtracaoEIdentificacaoNaEvidencia() throws Exception {
        var resultado = new ResultadoLeituraDTO("MARCA", "ERRO_TECNICO", List.of("FALHA_RESOLUCAO"),
                "Marca observada", null, null, List.of(), null, null,
                "Não foi possível interpretar a leitura; informe manualmente.", "sha256-pesos-teste");
        var extracao = new br.compneusgppremium.api.ocr.OcrResposta("PADDLEOCR", "3.3.2", "sha256-pesos-teste",
                "MARCA", List.of(new br.compneusgppremium.api.ocr.OcrResposta.Linha("Marca observada", 0.99,
                List.of(List.of(0, 0), List.of(10, 0), List.of(10, 10), List.of(0, 10)))), 100);
        var evidencia = new br.compneusgppremium.api.leitura.dto.ExecucaoLeituraDTO(resultado, extracao,
                Map.of(marcaId, "Marca observada"), "pre-v1", "normal-v1", "politica-v1", 0.9, true,
                new br.compneusgppremium.api.leitura.dto.IdentificacaoExecucaoDTO("1.11.0", "hash-configuracao"));
        when(leitura.analisarComEvidencia(any(), anyString(), any(), any())).thenThrow(
                new br.compneusgppremium.api.leitura.FalhaLeituraComEvidencia("FALHA_RESOLUCAO",
                        "Não foi possível interpretar a leitura; informe manualmente.",
                        org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, evidencia,
                        new IllegalStateException("catálogo inconsistente")));
        var sessao = abrir(UUID.randomUUID().toString());
        // O operador recebe 503 com o identificador da tentativa; a extração já paga não pode se perder.
        var tentativa = corpo(tentar(sessao.get("id").asText(), "unica", entradaTentativa())
                .andExpect(status().isServiceUnavailable()));
        assertThat(tentativa.get("resultado").get("estado").asText()).isEqualTo("ERRO_TECNICO");
        assertThat(tentativa.get("tentativaId").asText()).isNotBlank();
        var persistida = execucoes.findById(tentativa.get("tentativaId").asText()).orElseThrow();
        assertThat(persistida.getSnapshotJson()).contains("Marca observada", "sha256-pesos-teste",
                "hash-configuracao", "1.11.0");
        assertThat(persistida.getHashCatalogo()).hasSize(64);
    }

    @Test
    void abandonoPreservaEvidenciaEImpedeContinuarNaMesmaSessao() throws Exception {
        when(leitura.analisarComEvidencia(any(), anyString(), any(), any()))
                .thenThrow(new IllegalStateException("motor offline"));
        String sessao = abrir(UUID.randomUUID().toString()).get("id").asText();
        var tentativa = corpo(tentar(sessao, "antes-do-abandono", entradaTentativa())
                .andExpect(status().isServiceUnavailable()));
        var abandonada = corpo(abandonar(sessao, "Pneu recusado na inspeção visual", 0).andExpect(status().isOk()));
        assertThat(abandonada.get("status").asText()).isEqualTo("ABANDONADA");
        assertThat(abandonada.get("motivoAbandono").asText()).isEqualTo("Pneu recusado na inspeção visual");
        // Abandonar encerra a coleta, não apaga o que já foi coletado.
        assertThat(abandonada.get("tentativas").size()).isEqualTo(1);
        byte[] armazenada = mvc.perform(get("/api/v2/leitura-imagens/" + tentativa.get("imagemId").asText())
                .with(operador)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(armazenada).isEqualTo(Base64.getDecoder().decode(foto));
        var registro = sessoes.findById(sessao).orElseThrow();
        assertThat(registro.getAbandonadaPor()).isEqualTo(operadorId);
        assertThat(registro.getAbandonadaEm()).isNotNull();
        tentar(sessao, "depois-do-abandono", entradaTentativa()).andExpect(status().isConflict());
    }

    @Test
    void reenvioDoAbandonoNaoViraConflito() throws Exception {
        String sessao = abrir(UUID.randomUUID().toString()).get("id").asText();
        abandonar(sessao, "Foto impossível na lateral", 0).andExpect(status().isOk());
        abandonar(sessao, "Foto impossível na lateral", 0).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABANDONADA"));
    }

    @Test
    void abandonoExigeMotivoEVersaoVigente() throws Exception {
        String sessao = abrir(UUID.randomUUID().toString()).get("id").asText();
        mvc.perform(post("/api/v2/leitura-sessoes/" + sessao + "/abandono").with(operador)
                .contentType("application/json")
                .content(json.writeValueAsBytes(Map.of("motivo", " ", "versaoSessao", 0))))
                .andExpect(status().isBadRequest());
        abandonar(sessao, "Versão desatualizada no dispositivo", 99).andExpect(status().isConflict());
    }

    @Test
    void duasRequisicoesSimultaneasNaoDisparamDuasInferencias() throws Exception {
        var entrou = new java.util.concurrent.CountDownLatch(1);
        var liberar = new java.util.concurrent.CountDownLatch(1);
        when(leitura.analisarComEvidencia(any(), anyString(), any(), any())).thenAnswer(chamada -> {
            entrou.countDown();
            if (!liberar.await(10, java.util.concurrent.TimeUnit.SECONDS)) { throw new IllegalStateException("Timeout do teste"); }
            throw new IllegalStateException("Indisponibilidade simulada");
        });
        String sessao = abrir(UUID.randomUUID().toString()).get("id").asText();
        var executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var primeira = executor.submit(() -> tentar(sessao, "simultanea", entradaTentativa()));
            assertThat(entrou.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            tentar(sessao, "simultanea", entradaTentativa()).andExpect(status().isConflict());
            liberar.countDown(); primeira.get(10, java.util.concurrent.TimeUnit.SECONDS).andExpect(status().isServiceUnavailable());
            verify(leitura, times(1)).analisarComEvidencia(any(), anyString(), any(), any());
        } finally { liberar.countDown(); executor.shutdownNow(); }
    }
    @Test
    void recuperaInterrupcaoSemAceitarResultadoTardio() throws Exception {
        when(leitura.analisarComEvidencia(any(), anyString(), any(), any())).thenThrow(new IllegalStateException());
        String sessao = abrir(UUID.randomUUID().toString()).get("id").asText();
        var resposta = corpo(tentar(sessao, "interrompida", entradaTentativa()));
        var tentativa = tentativas.findById(resposta.get("tentativaId").asText()).orElseThrow();
        // Simula reinício ocorrido depois de persistir a captura e antes de concluir a inferência.
        tentativa.setEstado("PROCESSANDO"); tentativa.setRespostaJson(null);
        tentativa.setCriadaEm(java.time.Instant.now().minusSeconds(600)); tentativas.saveAndFlush(tentativa);
        assertThat(persistencia.recuperarInterrompidas(java.time.Instant.now().minusSeconds(300))).isEqualTo(1);
        var tardio = new ResultadoLeituraDTO("MARCA", "SUGESTAO", List.of(), "Tardio", marcaId, "Tardio",
                List.of(), 0.99, null, "Confira", "pesos");
        assertThat(persistencia.concluir(tentativa.getId(), tardio, null).estado()).isEqualTo("ERRO_TECNICO");
        mvc.perform(get("/api/v2/leitura-sessoes/" + sessao).with(operador))
                .andExpect(jsonPath("$.tentativas[0].resultado.motivos[0]").value("RESULTADO_INCERTO"));
    }
    @AfterAll
    static void removerEvidencias() throws Exception {
        try (var arquivos = Files.walk(EVIDENCIAS)) {
            for (var arquivo : arquivos.sorted(Comparator.reverseOrder()).toList()) Files.delete(arquivo);
        }
    }
}
