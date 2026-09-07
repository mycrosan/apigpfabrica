package br.compneusgppremium.api.integration;
import br.compneusgppremium.api.controller.model.UsuarioModel;
import br.compneusgppremium.api.repository.UsuarioRepository;
import br.compneusgppremium.api.revisao.repository.*;
import br.compneusgppremium.api.revisao.service.*;
import br.compneusgppremium.api.leitura.repository.*;
import br.compneusgppremium.api.leitura.model.*;
import br.compneusgppremium.api.leitura.service.ArquivosLeituraService;
import com.fasterxml.jackson.databind.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties = "pneus.revisao.coleta-habilitada=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class RevisaoLeituraIT {
    @Container static final MySQLContainer<?> BANCO = new MySQLContainer<>("mysql:8.4.6");
    static final Path ARQUIVOS = diretorio();
    static Path diretorio() {
        try { return Files.createTempDirectory("revisao-it-"); }
        catch (java.io.IOException erro) { throw new IllegalStateException(erro); }
    }
    @DynamicPropertySource static void configurar(final DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", BANCO::getJdbcUrl);
        registro.add("spring.datasource.username", BANCO::getUsername);
        registro.add("spring.datasource.password", BANCO::getPassword);
        registro.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registro.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        registro.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registro.add("spring.flyway.enabled", () -> true);
        registro.add("spring.jpa.defer-datasource-initialization", () -> false);
        registro.add("spring.sql.init.mode", () -> "never");
        registro.add("pneus.revisao.diretorio", ARQUIVOS::toString);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UsuarioRepository usuarios;
    @Autowired ItemRevisaoRepository itens;
    @Autowired RespostaRevisaoRepository respostas;
    @Autowired ImagemRevisaoRepository imagens;
    @Autowired SessaoLeituraRepository sessoes;
    @Autowired ConfirmacaoLeituraRepository confirmacoes;
    @Autowired ImpactoCadastroRevisaoService impacto;
    private UsuarioModel primeiro, segundo, terceiro;
    private Map<String, Object> importacao;
    private static final String ROTA = "/api/v2/revisoes-leitura";
    private static final List<List<Integer>> REGIAO = List.of(List.of(0,0), List.of(19,0), List.of(19,9), List.of(0,9));
    @BeforeEach void preparar() throws Exception {
        primeiro = usuario(); segundo = usuario(); terceiro = usuario();
        var bytes = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(20,10,BufferedImage.TYPE_INT_RGB), "png", bytes);
        importacao = Map.of("imagemSha256", ArquivosLeituraService.hash(bytes.toByteArray()),
                "manifestoSha256", ArquivosLeituraService.hash(UUID.randomUUID().toString().getBytes()),
                "fotoBase64", Base64.getEncoder().encodeToString(bytes.toByteArray()),
                "extracao", Map.of("motor", "PaddleOCR", "versaoModelo", "sintetico-v1", "campo", "MARCA",
                        "linhas", List.of(Map.of("texto", "OCR_SINTETICO_OCULTO", "escore", 0.7, "regiao", REGIAO))));
    }
    private UsuarioModel usuario() {
        var usuario = new UsuarioModel(); usuario.setNome("Revisor de teste"); usuario.setLogin(UUID.randomUUID().toString());
        usuario.setSenha("senha-nao-utilizada"); return usuarios.save(usuario);
    }
    private RequestPostProcessor autenticar(final UsuarioModel usuario, final String... permissoes) {
        return authentication(new UsernamePasswordAuthenticationToken(usuario, null,
                Arrays.stream(permissoes).map(SimpleGrantedAuthority::new).toList()));
    }
    private JsonNode corpo(final ResultActions resposta) throws Exception { return json.readTree(resposta.andReturn().getResponse().getContentAsString()); }
    private String importar() throws Exception {
        return corpo(mvc.perform(post(ROTA + "/importacoes").with(autenticar(primeiro, "IMPORTAR_LEITURA"))
                .contentType("application/json").content(json.writeValueAsBytes(importacao)))
                .andExpect(status().isCreated())).get("itens").get(0).asText();
    }
    private JsonNode consultar(final String id, final UsuarioModel revisor) throws Exception {
        return corpo(mvc.perform(get(ROTA + "/" + id).with(autenticar(revisor, "REVISAR_LEITURA"))).andExpect(status().isOk()));
    }
    private Map<String, Object> entrada(final long versao, final String texto, final String legibilidade) {
        var entrada = new LinkedHashMap<String,Object>(); entrada.put("versao", versao); entrada.put("campo", "MEDIDA");
        entrada.put("transcricao", texto); entrada.put("legibilidade", legibilidade); entrada.put("regiao", REGIAO);
        entrada.put("motivo", "Conferência independente da imagem sintética"); entrada.put("revisorId", -999);
        return entrada;
    }
    private ResultActions enviar(final String id, final UsuarioModel revisor, final String chave, final Map<String,Object> entrada) throws Exception {
        return mvc.perform(post(ROTA + "/" + id + "/respostas").with(autenticar(revisor, "REVISAR_LEITURA"))
                .header("Idempotency-Key", chave).contentType("application/json").content(json.writeValueAsBytes(entrada)));
    }
    @Test void protegeFilaImagemImportacaoEIdentidade() throws Exception {
        String id = importar();
        for (String rota : List.of(ROTA, ROTA + "/" + id, ROTA + "/" + id + "/imagem")) {
            mvc.perform(get(rota).with(autenticar(segundo))).andExpect(status().isForbidden());
        }
        mvc.perform(post(ROTA + "/importacoes").with(autenticar(segundo, "REVISAR_LEITURA"))
                .contentType("application/json").content(json.writeValueAsBytes(importacao))).andExpect(status().isForbidden());
        assertThat(importar()).isEqualTo(id);
        assertThat(consultar(id, segundo).get("campoSolicitado").isNull()).isTrue();
    }
    @Test void exigeDuasPessoasMantemRevisaoCegaEReenvioIdempotente() throws Exception {
        String id = importar(), chave = UUID.randomUUID().toString();
        var entrada = entrada(0, "205/55 R16", "LEGIVEL");
        assertThat(consultar(id, primeiro).toString()).doesNotContain("OCR_SINTETICO_OCULTO");
        var primeira = corpo(enviar(id, primeiro, chave, entrada).andExpect(status().isCreated()));
        assertThat(primeira.get("estado").asText()).isEqualTo("PENDENTE");
        assertThat(primeira.get("versao").asLong()).isEqualTo(1);
        assertThat(primeira.toString()).contains("OCR_SINTETICO_OCULTO");
        assertThat(consultar(id, segundo).toString()).doesNotContain("OCR_SINTETICO_OCULTO", "205/55 R16");
        enviar(id, primeiro, chave, entrada).andExpect(status().isCreated());
        var segunda = corpo(enviar(id, segundo, UUID.randomUUID().toString(), entrada(1, "205/55 R16", "LEGIVEL"))
                .andExpect(status().isCreated()));
        assertThat(segunda.get("estado").asText()).isEqualTo("APROVADA");
        assertThat(respostas.findByItemIdAndCicloOrderByCriadaEmAsc(id, 0)).hasSize(2)
                .allMatch(resposta -> resposta.getRevisorId() > 0);
        enviar(id, primeiro, chave, entrada(0, "OUTRO", "LEGIVEL")).andExpect(status().isConflict());
    }
    @Test void divergenciaExigeTerceiroRevisorEJustificativa() throws Exception {
        String id = importar();
        enviar(id, primeiro, UUID.randomUUID().toString(), entrada(0, "205/55 R16", "LEGIVEL")).andExpect(status().isCreated());
        var divergente = corpo(enviar(id, segundo, UUID.randomUUID().toString(), entrada(1, "205/65 R16", "LEGIVEL"))
                .andExpect(status().isCreated()));
        assertThat(divergente.get("estado").asText()).isEqualTo("DIVERGENTE");
        enviar(id, primeiro, UUID.randomUUID().toString(), entrada(2, "205/65 R16", "LEGIVEL")).andExpect(status().isConflict());
        var semMotivo = entrada(2, "205/65 R16", "LEGIVEL"); semMotivo.put("motivo", "");
        enviar(id, terceiro, UUID.randomUUID().toString(), semMotivo).andExpect(status().isUnprocessableEntity());
        assertThat(consultar(id, terceiro).toString()).doesNotContain("205/55 R16", "205/65 R16", "OCR_SINTETICO_OCULTO");
        var finalizada = corpo(enviar(id, terceiro, UUID.randomUUID().toString(), entrada(2, "205/65 R16", "LEGIVEL"))
                .andExpect(status().isCreated()));
        assertThat(finalizada.get("estado").asText()).isEqualTo("APROVADA");
    }
    @Test void ilegivelOuAusenteNaoRecebeRotuloConhecidoDeOutraFoto() throws Exception {
        String id = importar();
        enviar(id, primeiro, UUID.randomUUID().toString(), entrada(0, "205/55 R16", "ILEGIVEL")).andExpect(status().isUnprocessableEntity());
        enviar(id, primeiro, UUID.randomUUID().toString(), entrada(0, null, "ILEGIVEL")).andExpect(status().isCreated());
        var finalizada = corpo(enviar(id, segundo, UUID.randomUUID().toString(), entrada(1, null, "ILEGIVEL"))
                .andExpect(status().isCreated()));
        assertThat(finalizada.get("estado").asText()).isEqualTo("ILEGIVEL");
        assertThat(respostas.findByItemIdAndCicloOrderByCriadaEmAsc(id, 0)).allMatch(resposta -> resposta.getTranscricao() == null);
    }
    @Test void concorrenciaNaoSobrescreveResposta() throws Exception {
        String id = importar(); var inicio = new CountDownLatch(1); var executor = Executors.newFixedThreadPool(2);
        try {
            var primeira = executor.submit(() -> { inicio.await(); return enviar(id, primeiro, UUID.randomUUID().toString(),
                    entrada(0, "205/55 R16", "LEGIVEL")).andReturn().getResponse().getStatus(); });
            var segunda = executor.submit(() -> { inicio.await(); return enviar(id, segundo, UUID.randomUUID().toString(),
                    entrada(0, "205/65 R16", "LEGIVEL")).andReturn().getResponse().getStatus(); });
            inicio.countDown();
            assertThat(List.of(primeira.get(10, TimeUnit.SECONDS), segunda.get(10, TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
            assertThat(respostas.findByItemIdAndCicloOrderByCriadaEmAsc(id, 0)).hasSize(1);
        } finally { executor.shutdownNow(); }
    }
    @Test void confirmacaoNaoAprovaEEdicaoPosteriorSinalizaReavaliacao() throws Exception {
        String id = importar();
        var sessao = new SessaoLeitura(); sessao.setId(UUID.randomUUID().toString()); sessao.setGrupoFisico(UUID.randomUUID().toString());
        sessao.setOperadorId(terceiro.getId()); sessao.setStatus("ABERTA"); sessao.setCriadaEm(Instant.now());
        sessao.setConfirmacoesJson("{}"); sessoes.saveAndFlush(sessao);
        var item = itens.findById(id).orElseThrow(); item.setSessaoId(sessao.getId()); item.setOperadorId(terceiro.getId());
        item.setCampoSolicitado("MEDIDA"); itens.saveAndFlush(item);
        var confirmacao = new ConfirmacaoLeitura(); confirmacao.setId(UUID.randomUUID().toString());
        confirmacao.setSessaoId(sessao.getId()); confirmacao.setOperadorId(terceiro.getId()); confirmacao.setCampo("MEDIDA");
        confirmacao.setOrigem("MANUAL"); confirmacao.setValorFinal("205/65 R16"); confirmacao.setCriadaEm(Instant.now());
        confirmacao.setVersaoSessao(0L); confirmacoes.saveAndFlush(confirmacao); impacto.confirmacao(confirmacao);
        assertThat(itens.findById(id).orElseThrow().getEstado()).isEqualTo("PENDENTE");
        enviar(id, terceiro, UUID.randomUUID().toString(), entrada(2, "205/55 R16", "LEGIVEL")).andExpect(status().isForbidden());
        long versao = consultar(id, primeiro).get("versao").asLong();
        enviar(id, primeiro, UUID.randomUUID().toString(), entrada(versao, "205/55 R16", "LEGIVEL")).andExpect(status().isCreated());
        enviar(id, segundo, UUID.randomUUID().toString(), entrada(versao+1, "205/55 R16", "LEGIVEL")).andExpect(status().isCreated());
        assertThat(itens.findById(id).orElseThrow().isPendenciaCadastro()).isTrue();
        impacto.confirmacao(confirmacao);
        assertThat(itens.findById(id).orElseThrow().isReavaliar()).isTrue();
        assertThat(respostas.findByItemIdAndCicloOrderByCriadaEmAsc(id,0)).hasSize(2);
    }
}
