package br.compneusgppremium.api.service;
import br.compneusgppremium.api.config.OcrLocalProperties;
import br.compneusgppremium.api.controller.dto.ResultadoLeituraDTO.Candidato;
import br.compneusgppremium.api.leitura.service.IdentificacaoExecucaoService;
import br.compneusgppremium.api.ocr.OcrLocalClient;
import br.compneusgppremium.api.ocr.OcrResposta;
import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class LeituraCarcacaServiceTest {
    private final OcrLocalClient motor = mock(OcrLocalClient.class);
    private final ImagemLeituraService imagens = mock(ImagemLeituraService.class);
    private final ResolverCatalogoPneuService catalogo = mock(ResolverCatalogoPneuService.class);
    private IdentificacaoExecucaoService identificacao(final OcrLocalProperties ocr) {
        var politica = new br.compneusgppremium.api.config.PoliticaPneuProperties(
                java.time.ZoneId.of("America/Sao_Paulo"), 2000, false, 0, Set.of("PASSEIO"), Set.of("METRICA_RADIAL"));
        return new IdentificacaoExecucaoService(new org.springframework.beans.factory.support.DefaultListableBeanFactory()
                .getBeanProvider(org.springframework.boot.info.BuildProperties.class), ocr, politica);
    }
    private LeituraCarcacaService servico(final boolean aprovado, final String texto, final List<Candidato> candidatos) {
        when(imagens.validar(anyString())).thenReturn(new byte[] {1});
        when(motor.ler(anyString(), anyString())).thenReturn(new OcrResposta("PADDLEOCR", "3.3.2", "pesos-fixos",
                "MEDIDA", List.of(new OcrResposta.Linha(texto, 0.98, List.of())), 10));
        when(catalogo.snapshot(anyString(), any(), any())).thenReturn(Map.of(1, "205/55R16"));
        when(catalogo.resolverSnapshot(anyString(), anyList(), anyMap(), any())).thenReturn(candidatos);
        var politica = new OcrLocalProperties(URI.create("http://localhost:8091"), "token-interno-teste", 15,
                true, aprovado ? Set.of("MEDIDA") : Set.of(), 0.9, 8388608, 20000000, 3, 30);
        return new LeituraCarcacaService(motor, politica, imagens, catalogo, identificacao(politica));
    }
    @Test
    void preservaVersaoDoPreprocessamentoRecebidaDoOcr() {
        var servico = servico(false, "", List.of());
        when(motor.ler(anyString(), anyString())).thenReturn(new OcrResposta("PADDLEOCR", "3.3.2", "pesos-perfil",
                "DOT", List.of(), 10, "exif-transpose-rgb-dot-relevo-v1"));
        var evidencia = servico.analisarComEvidencia(LeituraCarcacaService.Campo.DOT, "foto", null, null);
        assertThat(evidencia.preprocessamento()).isEqualTo("exif-transpose-rgb-dot-relevo-v1");
    }
    @Test
    void ilegivelNaoProduzValorOuId() {
        var resultado = servico(true, "", List.of()).analisar(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        assertThat(resultado.estado()).isEqualTo("ILEGIVEL"); assertThat(resultado.mensagem()).contains("Não consegui ler");
        assertThat(resultado.itemSugeridoId()).isNull(); assertThat(resultado.valorSugerido()).isNull();
    }
    @Test
    void empateNaoEscolheMedidaPeloHistorico() {
        var resultado = servico(true, "205/55R16 205/65R16", List.of(new Candidato(1, "205/55R16", 0.99),
                new Candidato(2, "205/65R16", 0.98))).analisar(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        assertThat(resultado.estado()).isEqualTo("AMBIGUA"); assertThat(resultado.itemSugeridoId()).isNull();
        assertThat(resultado.candidatos()).hasSize(2);
    }
    @Test
    void foraCatalogoPreservaTranscricao() {
        var resultado = servico(true, "195/99R16", List.of()).analisar(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        assertThat(resultado.estado()).isEqualTo("FORA_CATALOGO"); assertThat(resultado.textoOriginal()).isEqualTo("195/99R16");
    }
    @Test
    void campoSemAprovacaoNaoLiberaSugestao() {
        var resultado = servico(false, "205/55R16", List.of(new Candidato(1, "205/55R16", 0.999)))
                .analisar(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        assertThat(resultado.estado()).isNotEqualTo("SUGESTAO"); assertThat(resultado.itemSugeridoId()).isNull();
        assertThat(resultado.probabilidadeCalibrada()).isNull();
    }
    @Test
    void ollamaPreservaTranscricaoECandidatosSemHerdarAprovacaoDoPaddle() {
        var servico = servico(true, "205/55R16", List.of(new Candidato(1, "205/55R16", null)));
        when(motor.ler(anyString(), anyString())).thenReturn(new OcrResposta("OLLAMA_LOCAL", "runtime-teste",
                "sha256-config", "MEDIDA", List.of(new OcrResposta.Linha("205/55R16", null, List.of())),
                500, "ollama-vision-rgb-v1"));

        var evidencia = servico.analisarComEvidencia(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        var resultado = evidencia.resultado();
        assertThat(resultado.estado()).isEqualTo("AMBIGUA");
        assertThat(resultado.motivos()).contains("OLLAMA_NAO_VALIDADO");
        assertThat(resultado.textoOriginal()).isEqualTo("205/55R16");
        assertThat(resultado.itemSugeridoId()).isNull();
        assertThat(resultado.valorSugerido()).isNull();
        assertThat(resultado.escoreOcrBruto()).isNull();
        assertThat(resultado.probabilidadeCalibrada()).isNull();
        assertThat(resultado.candidatos()).singleElement().satisfies(candidato -> {
            assertThat(candidato.id()).isEqualTo(1);
            assertThat(candidato.escore()).isNull();
        });
        assertThat(evidencia.campoAprovado()).isFalse();
        assertThat(evidencia.extracao().motor()).isEqualTo("OLLAMA_LOCAL");
        assertThat(evidencia.extracao().versaoBiblioteca()).isEqualTo("runtime-teste");
        assertThat(evidencia.preprocessamento()).isEqualTo("ollama-vision-rgb-v1");
        assertThat(evidencia.politica()).isEqualTo("decisao-conservadora-v2");

        var legado = servico.lerCampo(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        assertThat(legado.getId()).isNull();
        assertThat(legado.getConfianca()).isEqualTo("BAIXA");
        assertThat(legado.getCandidatos().get(0).getEscore()).isNull();
        assertThat(legado.getMensagem()).contains("em avaliação", "manualmente");
    }

    @Test
    void ollamaNaoPromoveNemSeUmAdaptadorProduzirEscoreAlto() {
        var servico = servico(true, "205/55R16", List.of(new Candidato(1, "205/55R16", 1.0)));
        when(motor.ler(anyString(), anyString())).thenReturn(new OcrResposta("OLLAMA_LOCAL", "runtime-teste",
                "sha256-config", "MEDIDA", List.of(new OcrResposta.Linha("205/55R16", null, List.of())),
                500, "ollama-vision-rgb-v1"));
        var resultado = servico.analisar(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        assertThat(resultado.estado()).isNotEqualTo("SUGESTAO");
        assertThat(resultado.itemSugeridoId()).isNull();
        assertThat(resultado.motivos()).contains("OLLAMA_NAO_VALIDADO");
    }

    @Test
    void ollamaSemTextoContinuaIlegivel() {
        var servico = servico(true, "", List.of());
        when(motor.ler(anyString(), anyString())).thenReturn(new OcrResposta("OLLAMA_LOCAL", "runtime-teste",
                "sha256-config", "MEDIDA", List.of(), 500, "ollama-vision-rgb-v1"));
        var resultado = servico.analisar(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        assertThat(resultado.estado()).isEqualTo("ILEGIVEL");
        assertThat(resultado.mensagem()).contains("Não consegui ler");
        assertThat(resultado.motivos()).contains("TEXTO_AUSENTE", "OLLAMA_NAO_VALIDADO");
        assertThat(resultado.candidatos()).isEmpty();
    }
    @Test
    void baixaConfiancaNaoLiberaSugestao() {
        var resultado = servico(true, "205/55R16", List.of(new Candidato(1, "205/55R16", 0.6)))
                .analisar(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        assertThat(resultado.estado()).isNotEqualTo("SUGESTAO"); assertThat(resultado.itemSugeridoId()).isNull();
    }
    @Test
    void sugestaoAprovadaExigeConfirmacaoENaoInventaProbabilidade() {
        var resultado = servico(true, "205/55R16", List.of(new Candidato(1, "205/55R16", 0.98)))
                .analisar(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        assertThat(resultado.estado()).isEqualTo("SUGESTAO"); assertThat(resultado.itemSugeridoId()).isEqualTo(1);
        assertThat(resultado.motivos()).contains("CONFIRMACAO_OBRIGATORIA"); assertThat(resultado.probabilidadeCalibrada()).isNull();
    }
    @Test
    void ocrIndisponivelPreservaCatalogoEIdentificacaoNaEvidencia() {
        var servico = servico(true, "205/55R16", List.of());
        when(motor.ler(anyString(), anyString())).thenThrow(new br.compneusgppremium.api.exception
                .CadastroPneuException("OCR_INDISPONIVEL", "OCR indisponível; informe o campo manualmente.",
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> servico.analisarComEvidencia(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2))
                .isInstanceOfSatisfying(br.compneusgppremium.api.leitura.FalhaLeituraComEvidencia.class, falha -> {
                    assertThat(falha.getCodigo()).isEqualTo("OCR_INDISPONIVEL");
                    assertThat(falha.getStatus().value()).isEqualTo(503);
                    assertThat(falha.getEvidencia().extracao()).isNull();
                    assertThat(falha.getEvidencia().catalogo()).containsEntry(1, "205/55R16");
                    assertThat(falha.getEvidencia().identificacao().hashConfiguracao()).hasSize(64);
                    assertThat(falha.getEvidencia().resultado().estado()).isEqualTo("ERRO_TECNICO");
                });
    }

    @Test
    void falhaDeResolucaoNaoDescartaAExtracaoJaPaga() {
        var servico = servico(true, "205/55R16", List.of());
        when(catalogo.resolverSnapshot(anyString(), anyList(), anyMap(), any()))
                .thenThrow(new IllegalStateException("catálogo inconsistente"));
        assertThatThrownBy(() -> servico.analisarComEvidencia(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2))
                .isInstanceOfSatisfying(br.compneusgppremium.api.leitura.FalhaLeituraComEvidencia.class, falha -> {
                    assertThat(falha.getCodigo()).isEqualTo("FALHA_RESOLUCAO");
                    assertThat(falha.getEvidencia().extracao().versaoModelo()).isEqualTo("pesos-fixos");
                    assertThat(falha.getEvidencia().resultado().textoOriginal()).isEqualTo("205/55R16");
                    assertThat(falha.getEvidencia().resultado().itemSugeridoId()).isNull();
                });
    }

    @Test
    void contextoInvalidoFalhaAntesDeGastarInferencia() {
        var servico = servico(true, "205/55R16", List.of());
        when(catalogo.snapshot(anyString(), any(), any())).thenThrow(new br.compneusgppremium.api.exception
                .CadastroPneuException("CONTEXTO_INVALIDO", "Confira a marca e o modelo antes da leitura."));
        assertThatThrownBy(() -> servico.analisarComEvidencia(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2))
                .isInstanceOf(br.compneusgppremium.api.exception.CadastroPneuException.class)
                .isNotInstanceOf(br.compneusgppremium.api.leitura.FalhaLeituraComEvidencia.class);
        verify(motor, never()).ler(anyString(), anyString());
    }

    @Test
    void legadoEntregaSugestaoDeCampoAprovado() {
        var resposta = servico(true, "205/55R16", List.of(new Candidato(1, "205/55R16", 0.98)))
                .lerCampo(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        assertThat(resposta.getConfianca()).isEqualTo("ALTA");
        assertThat(resposta.getId()).isEqualTo(1);
        assertThat(resposta.getMensagem()).contains("confirme o valor");
    }

    @Test
    void legadoEntregaDotComOsQuatroDigitos() {
        var ocr = new OcrLocalProperties(URI.create("http://localhost:8091"), "token-interno-teste", 15,
                true, Set.of("DOT"), 0.9, 8388608, 20000000, 3, 30);
        when(imagens.validar(anyString())).thenReturn(new byte[] {1});
        when(motor.ler(anyString(), anyString())).thenReturn(new OcrResposta("PADDLEOCR", "3.3.2", "pesos-fixos",
                "DOT", List.of(new OcrResposta.Linha("3923", 0.99, List.of())), 10));
        when(catalogo.snapshot(anyString(), any(), any())).thenReturn(Map.of());
        when(catalogo.resolverSnapshot(anyString(), anyList(), anyMap(), any()))
                .thenReturn(List.of(new Candidato(null, "3923", 0.99)));
        var resposta = new LeituraCarcacaService(motor, ocr, imagens, catalogo, identificacao(ocr))
                .lerCampo(LeituraCarcacaService.Campo.DOT, "foto", null, null);
        assertThat(resposta.getDot()).isEqualTo("3923");
        assertThat(resposta.getDotCompleto()).isEqualTo("3923");
        assertThat(resposta.getConfianca()).isEqualTo("ALTA");
        assertThat(resposta.getId()).isNull();
    }

    // Mostrar candidatos não é sugerir valor: a lista chega à tela mesmo com o campo fora dos
    // aprovados, para o operador escolher, mas id e dot continuam vazios.
    @Test
    void campoNaoAprovadoAindaOfereceCandidatosParaEscolha() {
        var resposta = servico(false, "205/55R16", List.of(new Candidato(1, "205/55R16", 0.98)))
                .lerCampo(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        assertThat(resposta.getCandidatos()).extracting("id").containsExactly(1);
        assertThat(resposta.getId()).isNull();
        assertThat(resposta.getConfianca()).isEqualTo("BAIXA");
        assertThat(resposta.getMensagem()).contains("Escolha");
    }

    @Test
    void leituraAmbiguaEntregaTodosOsCandidatosSemEscolher() {
        var resposta = servico(true, "255/35R18", List.of(new Candidato(49, "255/35 ZR18", 0.95),
                new Candidato(50, "255/35 R18", 0.94)))
                .lerCampo(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        assertThat(resposta.getCandidatos()).extracting("id").containsExactlyInAnyOrder(49, 50);
        assertThat(resposta.getId()).isNull();
        assertThat(resposta.getEstado()).isEqualTo("AMBIGUA");
    }

    @Test
    void fotoIlegivelNaoOfereceCandidato() {
        var resposta = servico(true, "", List.of())
                .lerCampo(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        assertThat(resposta.getCandidatos()).isEmpty();
        assertThat(resposta.getEstado()).isEqualTo("ILEGIVEL");
        assertThat(resposta.getMensagem()).contains("Não consegui ler");
    }

    @Test
    void legadoNaoEntregaValorComEscoreAbaixoDoMinimo() {
        var resposta = servico(true, "205/55R16", List.of(new Candidato(1, "205/55R16", 0.6)))
                .lerCampo(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        assertThat(resposta.getConfianca()).isEqualTo("BAIXA");
        assertThat(resposta.getId()).isNull();
        assertThat(resposta.getDot()).isNull();
    }

    @Test
    void legadoDistingueCampoNaoAprovadoDeFotoIlegivel() {
        var resposta = servico(false, "205/55R16", List.of(new Candidato(1, "205/55R16", 0.98)))
                .lerCampo(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        assertThat(resposta.getMensagem()).contains("Escolha");
        assertThat(resposta.getConfianca()).isEqualTo("BAIXA");
        assertThat(resposta.getId()).isNull();
        var ilegivel = servico(false, "", List.of())
                .lerCampo(LeituraCarcacaService.Campo.MEDIDA, "foto", 1, 2);
        assertThat(ilegivel.getMensagem()).contains("Não consegui ler");
        assertThat(ilegivel.getCandidatos()).isEmpty();
    }
}
