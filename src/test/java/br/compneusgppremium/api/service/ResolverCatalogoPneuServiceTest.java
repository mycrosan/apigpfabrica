package br.compneusgppremium.api.service;

import br.compneusgppremium.api.config.PoliticaPneuProperties;
import br.compneusgppremium.api.controller.model.MarcaModel;
import br.compneusgppremium.api.controller.model.MedidaModel;
import br.compneusgppremium.api.controller.model.ModeloModel;
import br.compneusgppremium.api.controller.model.PaisModel;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.ocr.OcrResposta;
import br.compneusgppremium.api.repository.MarcaRepository;
import br.compneusgppremium.api.repository.MedidaRepository;
import br.compneusgppremium.api.repository.ModeloRepository;
import br.compneusgppremium.api.repository.PaisRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResolverCatalogoPneuServiceTest {
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private final MarcaRepository marcas = mock(MarcaRepository.class);
    private final ModeloRepository modelos = mock(ModeloRepository.class);
    private final MedidaRepository medidas = mock(MedidaRepository.class);
    private final PaisRepository paises = mock(PaisRepository.class);
    private final ValidacaoFormatoPneuService formatos = new ValidacaoFormatoPneuService(
            new PoliticaPneuProperties(FUSO, 2000, false, 0, Set.of("PASSEIO"),
                    Set.of("METRICA_RADIAL", "POLEGADAS_RADIAL", "FLUTUACAO")),
            Clock.fixed(Instant.parse("2026-09-05T15:00:00Z"), FUSO));
    private final ResolverCatalogoPneuService resolvedor =
            new ResolverCatalogoPneuService(marcas, modelos, medidas, paises, formatos);

    private static OcrResposta.Linha linha(final String texto, final double escore) {
        return new OcrResposta.Linha(texto, escore, List.of());
    }

    private static MarcaModel marca(final Integer id, final String descricao) {
        MarcaModel marca = new MarcaModel();
        marca.setId(id);
        marca.setDescricao(descricao);
        return marca;
    }

    private static ModeloModel modelo(final Integer id, final String descricao, final MarcaModel marca) {
        ModeloModel modelo = new ModeloModel();
        modelo.setId(id);
        modelo.setDescricao(descricao);
        modelo.setMarca(marca);
        return modelo;
    }

    private static MedidaModel medida(final Integer id, final String descricao) {
        MedidaModel medida = new MedidaModel();
        medida.setId(id);
        medida.setDescricao(descricao);
        return medida;
    }

    private static PaisModel pais(final Integer id, final String descricao) {
        PaisModel pais = new PaisModel();
        pais.setId(id);
        pais.setDescricao(descricao);
        return pais;
    }

    @Test
    void modeloSemMarcaNaoAbreOCatalogoInteiro() {
        assertThatThrownBy(() -> resolvedor.snapshot("MODELO", null, null))
                .isInstanceOf(CadastroPneuException.class)
                .hasFieldOrPropertyWithValue("codigo", "CONTEXTO_INVALIDO");
    }

    @Test
    void marcaInexistenteInterrompeAntesDaLeitura() {
        when(marcas.existsById(9)).thenReturn(false);
        assertThatThrownBy(() -> resolvedor.snapshot("MARCA", 9, null))
                .isInstanceOf(CadastroPneuException.class)
                .hasFieldOrPropertyWithValue("codigo", "CONTEXTO_INVALIDO");
    }

    @Test
    void modeloDeOutraMarcaNaoEAceitoComoContexto() {
        MarcaModel outra = marca(2, "Outra");
        when(marcas.existsById(1)).thenReturn(true);
        when(modelos.findById(7)).thenReturn(Optional.of(modelo(7, "Modelo", outra)));
        assertThatThrownBy(() -> resolvedor.snapshot("MEDIDA", 1, 7))
                .isInstanceOf(CadastroPneuException.class)
                .hasFieldOrPropertyWithValue("codigo", "CONTEXTO_INVALIDO");
    }

    @Test
    void catalogoDeModeloTrazSomenteOsDaMarcaInformada() {
        MarcaModel alvo = marca(1, "Marca A");
        when(marcas.existsById(1)).thenReturn(true);
        when(modelos.findAll()).thenReturn(List.of(modelo(10, "Do alvo", alvo),
                modelo(11, "De outra", marca(2, "Marca B")), modelo(12, "Sem marca", null)));
        assertThat(resolvedor.snapshot("MODELO", 1, null)).containsExactly(Map.entry(10, "Do alvo"));
    }

    @Test
    void campoDesconhecidoNaoProduzCatalogoVazioSilencioso() {
        assertThatThrownBy(() -> resolvedor.snapshot("PRESSAO", null, null))
                .isInstanceOf(CadastroPneuException.class)
                .hasFieldOrPropertyWithValue("codigo", "CAMPO_INVALIDO");
    }

    @Test
    void catalogoDeDotEVazioEImutavel() {
        Map<Integer, String> snapshot = resolvedor.snapshot("DOT", null, null);
        assertThat(snapshot).isEmpty();
        assertThatThrownBy(() -> snapshot.put(1, "0126")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void medidaCasaIgnorandoEspacosVirgulaECaixa() {
        when(medidas.findAll()).thenReturn(List.of(medida(3, "205/55R16")));
        var candidatos = resolvedor.resolver("MEDIDA", List.of(linha(" 205 / 55 r16 ", 0.97)), null, null);
        assertThat(candidatos).singleElement()
                .satisfies(candidato -> assertThat(candidato.id()).isEqualTo(3));
    }

    @Test
    void medidaForaDoCatalogoNaoRecebeItemParecido() {
        when(medidas.findAll()).thenReturn(List.of(medida(3, "205/55R16")));
        assertThat(resolvedor.resolver("MEDIDA", List.of(linha("205/60R16", 0.99)), null, null)).isEmpty();
    }

    @Test
    void paisExigePrefixoDeOrigemNaLateral() {
        when(paises.findAll()).thenReturn(List.of(pais(5, "Índia")));
        assertThat(resolvedor.resolver("PAIS", List.of(linha("made in india", 0.95)), null, null))
                .singleElement().satisfies(candidato -> assertThat(candidato.id()).isEqualTo(5));
        assertThat(resolvedor.resolver("PAIS", List.of(linha("india", 0.95)), null, null)).isEmpty();
    }

    @Test
    void linhaSemTextoOuComEscoreInvalidoEDescartada() {
        when(marcas.findAll()).thenReturn(List.of(marca(1, "Michelin")));
        assertThat(resolvedor.resolver("MARCA",
                List.of(linha(null, 0.99), linha("Michelin", Double.NaN)), null, null)).isEmpty();
    }

    @Test
    void mesmoItemEmDuasLinhasMantemOMaiorEscore() {
        when(marcas.findAll()).thenReturn(List.of(marca(1, "Michelin")));
        var candidatos = resolvedor.resolver("MARCA",
                List.of(linha("michelin", 0.70), linha("MICHELIN", 0.93)), null, null);
        assertThat(candidatos).singleElement()
                .satisfies(candidato -> assertThat(candidato.escore()).isEqualTo(0.93));
    }

    @Test
    void dotAceitaQuatroDigitosIsoladosDentroDaFaixa() {
        var candidatos = resolvedor.resolver("DOT", List.of(linha("dot bx7k 3625", 0.91)), null, null);
        assertThat(candidatos).singleElement().satisfies(candidato -> {
            assertThat(candidato.texto()).isEqualTo("3625");
            assertThat(candidato.id()).isNull();
        });
    }

    @Test
    void dotColadoAOutrosCaracteresNaoEExtraido() {
        assertThat(resolvedor.resolver("DOT", List.of(linha("AB3625", 0.99)), null, null)).isEmpty();
        assertThat(resolvedor.resolver("DOT", List.of(linha("36251", 0.99)), null, null)).isEmpty();
    }

    @Test
    void dotComSemanaOuAnoImpossivelNaoViraCandidato() {
        assertThat(resolvedor.resolver("DOT", List.of(linha("0025", 0.99)), null, null)).isEmpty();
        assertThat(resolvedor.resolver("DOT", List.of(linha("5425", 0.99)), null, null)).isEmpty();
        assertThat(resolvedor.resolver("DOT", List.of(linha("3699", 0.99)), null, null)).isEmpty();
    }

    @Test
    void dotRepetidoNaoDuplicaCandidato() {
        assertThat(resolvedor.resolver("DOT", List.of(linha("3625 3625", 0.99)), null, null)).hasSize(1);
    }

    @Test
    void dotAmbiguoPreservaTodosOsCandidatosSemEscolher() {
        var candidatos = resolvedor.resolver("DOT", List.of(linha("3625 0126", 0.99)), null, null);
        assertThat(candidatos).extracting("texto").containsExactlyInAnyOrder("3625", "0126");
    }
}
