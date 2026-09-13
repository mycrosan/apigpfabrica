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
    private final SinonimosPaisService sinonimosPais = sinonimosCarregados();
    private final ResolverCatalogoPneuService resolvedor =
            new ResolverCatalogoPneuService(marcas, modelos, medidas, paises, formatos, sinonimosPais);

    private static SinonimosPaisService sinonimosCarregados() {
        var servico = new SinonimosPaisService();
        servico.carregar();
        return servico;
    }

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

    // O marcador "MADE IN" deixou de ser exigido: em foto de perto do campo ele costuma ficar fora do
    // quadro. Quem protege contra ruído é o vocabulário fechado do catálogo, não o prefixo.
    @Test
    void paisEReconhecidoComOuSemOMarcadorDeFabricacao() {
        when(paises.findAll()).thenReturn(List.of(pais(5, "Índia")));
        assertThat(resolvedor.resolver("PAIS", List.of(linha("made in india", 0.95)), null, null))
                .singleElement().satisfies(candidato -> assertThat(candidato.id()).isEqualTo(5));
        assertThat(resolvedor.resolver("PAIS", List.of(linha("india", 0.95)), null, null))
                .singleElement().satisfies(candidato -> assertThat(candidato.id()).isEqualTo(5));
    }

    // Leitura real da foto de país tirada na fábrica: o OCR devolveu o marcador colado ao nome,
    // numa palavra só. Exigir fronteira de palavra fazia a leitura inteira ser descartada.
    @Test
    void marcadorDeFabricacaoColadoAoNomeDoPaisAindaCasa() {
        when(paises.findAll()).thenReturn(List.of(pais(4, "CHINA"), pais(1, "BRASIL")));
        assertThat(resolvedor.resolver("PAIS", List.of(linha("MADEINCHINA", 0.9)), null, null))
                .singleElement().satisfies(candidato -> assertThat(candidato.id()).isEqualTo(4));
    }

    @Test
    void textoDeConstrucaoDoPneuNaoEConfundidoComPais() {
        when(paises.findAll()).thenReturn(List.of(pais(1, "BRASIL"), pais(12, "THAILAND")));
        assertThat(resolvedor.resolver("PAIS",
                List.of(linha("PLIES TREAD POLYESTER", 0.99), linha("SIDEWALL", 0.97)), null, null)).isEmpty();
    }

    @Test
    void paisEmInglesNoPneuCasaComCadastroEmPortugues() {
        when(paises.findAll()).thenReturn(List.of(pais(1, "BRASIL"), pais(5, "JAPAO")));
        assertThat(resolvedor.resolver("PAIS", List.of(linha("MADE IN BRAZIL", 0.96)), null, null))
                .singleElement().satisfies(candidato -> assertThat(candidato.id()).isEqualTo(1));
        assertThat(resolvedor.resolver("PAIS", List.of(linha("MADE IN JAPAN", 0.96)), null, null))
                .singleElement().satisfies(candidato -> assertThat(candidato.id()).isEqualTo(5));
    }

    // "INDUSTRIA BRASILEIRA" é declaração de fabricação impressa no flanco, não inferência pela marca.
    @Test
    void industriaBrasileiraDeclaraFabricacaoNoBrasil() {
        when(paises.findAll()).thenReturn(List.of(pais(1, "BRASIL")));
        assertThat(resolvedor.resolver("PAIS", List.of(linha("INDUSTRIA BRASILEIRA", 0.94)), null, null))
                .singleElement().satisfies(candidato -> assertThat(candidato.id()).isEqualTo(1));
    }

    @Test
    void leituraTruncadaDePaisNaoViraNomeCompleto() {
        when(paises.findAll()).thenReturn(List.of(pais(1, "BRASIL")));
        assertThat(resolvedor.resolver("PAIS", List.of(linha("BRAZ", 0.99)), null, null)).isEmpty();
    }

    // O catálogo guarda o Reino Unido duas vezes; a ambiguidade é do cadastro e quem decide é o operador.
    @Test
    void paisComDoisCadastrosParaOMesmoNomeFicaAmbiguo() {
        when(paises.findAll()).thenReturn(
                List.of(pais(15, "THE UNITED KINGDOM"), pais(21, "GREAT BRITAIN"), pais(1, "BRASIL")));
        assertThat(resolvedor.resolver("PAIS", List.of(linha("MADE IN UK", 0.93)), null, null))
                .extracting("id").containsExactlyInAnyOrder(15, 21);
    }

    @Test
    void pontuacaoDoPaisNaoImpedeOCasamento() {
        when(paises.findAll()).thenReturn(List.of(pais(6, "USA")));
        assertThat(resolvedor.resolver("PAIS", List.of(linha("MADE IN U.S.A.", 0.92)), null, null))
                .singleElement().satisfies(candidato -> assertThat(candidato.id()).isEqualTo(6));
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

    // O detector devolve a mesma marcação em relevo em duas regiões sobrepostas; sem a
    // deduplicação entre linhas isso viraria ambiguidade e nenhuma sugestão chegaria à tela.
    @Test
    void dotRepetidoEmLinhasDiferentesNaoViraAmbiguidade() {
        var candidatos = resolvedor.resolver("DOT",
                List.of(linha("3625", 0.998), linha("3625", 0.997)), null, null);
        assertThat(candidatos).hasSize(1);
        assertThat(candidatos.get(0).texto()).isEqualTo("3625");
    }

    // O flanco imprime a medida cercada de sufixo de uso e índice de carga; o catálogo guarda só a
    // medida. Descartar esses atributos não é aproximar dígito — os dígitos retidos são os da imagem.
    @Test
    void medidaComSufixoDeUsoEIndiceDeCargaCasaComOCatalogo() {
        when(medidas.findAll()).thenReturn(List.of(medida(5, "175/70 R14")));
        assertThat(resolvedor.resolver("MEDIDA", List.of(linha("175/70R14C 88T", 0.88)), null, null))
                .singleElement().satisfies(candidato -> assertThat(candidato.id()).isEqualTo(5));
    }

    @Test
    void medidaColadaAoIndiceDeCargaSeparadoPorEspacoAindaCasa() {
        when(medidas.findAll()).thenReturn(List.of(medida(3, "205/55 R16")));
        assertThat(resolvedor.resolver("MEDIDA", List.of(linha("205/55R16 91V", 0.9)), null, null))
                .singleElement().satisfies(candidato -> assertThat(candidato.id()).isEqualTo(3));
    }

    // Leitura real observada na foto de medida: o OCR devolveu SI7OR14E. Não casar é o comportamento
    // correto — corrigir esses caracteres seria inventar a medida.
    @Test
    void leituraDegradadaDeMedidaNaoProduzCandidato() {
        when(medidas.findAll()).thenReturn(List.of(medida(5, "175/70 R14")));
        assertThat(resolvedor.resolver("MEDIDA", List.of(linha("SI7OR14E", 0.72)), null, null)).isEmpty();
    }

    @Test
    void medidasQueSoDiferemNaConstrucaoFicamAmbiguas() {
        when(medidas.findAll()).thenReturn(List.of(medida(49, "255/35 ZR18"), medida(50, "255/35 R18")));
        assertThat(resolvedor.resolver("MEDIDA", List.of(linha("255/35ZR18", 0.95)), null, null))
                .extracting("id").containsExactlyInAnyOrder(49, 50);
    }

    @Test
    void medidaCadastradaSemPerfilFicaForaDoCasamentoPorPadrao() {
        when(medidas.findAll()).thenReturn(List.of(medida(51, "185/R14 C")));
        assertThat(resolvedor.resolver("MEDIDA", List.of(linha("185/R14 C", 0.95)), null, null)).isEmpty();
    }

    // O catálogo cadastra o modelo com a marca no início; o pneu imprime só o modelo.
    @Test
    void modeloCasaComADescricaoSemOPrefixoDaMarca() {
        MarcaModel hifly = marca(1, "HIFLY");
        when(marcas.existsById(1)).thenReturn(true);
        when(marcas.findById(1)).thenReturn(Optional.of(hifly));
        when(modelos.findAll()).thenReturn(List.of(modelo(2, "HIFLY HF-805 CHALLENGER DSRT", hifly)));
        assertThat(resolvedor.resolver("MODELO", List.of(linha("HF-805 CHALLENGER DSRT", 0.91)), 1, null))
                .singleElement().satisfies(candidato -> assertThat(candidato.id()).isEqualTo(2));
        assertThat(resolvedor.resolver("MODELO", List.of(linha("HF-805", 0.91)), 1, null))
                .singleElement().satisfies(candidato -> assertThat(candidato.id()).isEqualTo(2));
    }

    // Caso real: pneu HIFLY SUPER 2000, foto tirada na fábrica. O catálogo cadastra "HIFLY SUPER
    // 2000" com espaço; o molde do pneu grava "SUPER2000" colado. Comparar por token exato nunca
    // casaria isso -- é a fronteira de espaço de quem digitou o catálogo, não informação da foto.
    @Test
    void modeloSemEspacoNoMoldeCasaComDescricaoComEspacoNoCatalogo() {
        MarcaModel hifly = marca(1, "HIFLY");
        when(marcas.existsById(1)).thenReturn(true);
        when(marcas.findById(1)).thenReturn(Optional.of(hifly));
        when(modelos.findAll()).thenReturn(List.of(modelo(274, "HIFLY SUPER 2000", hifly)));
        assertThat(resolvedor.resolver("MODELO", List.of(linha("SUPER2000", 0.9)), 1, null))
                .singleElement().satisfies(candidato -> assertThat(candidato.id()).isEqualTo(274));
    }

    // A comparação compacta ainda respeita ordem e contiguidade: "SUPER" sozinho só casa com o
    // modelo que de fato tem esse token, mesmo havendo outro modelo da marca com "2000" também.
    @Test
    void comparacaoCompactaRespeitaOrdemENaoCasaComModeloSemOToken() {
        MarcaModel hifly = marca(1, "HIFLY");
        when(marcas.existsById(1)).thenReturn(true);
        when(marcas.findById(1)).thenReturn(Optional.of(hifly));
        when(modelos.findAll()).thenReturn(List.of(modelo(274, "HIFLY SUPER 2000", hifly),
                modelo(300, "HIFLY 2000 TURBO", hifly)));
        assertThat(resolvedor.resolver("MODELO", List.of(linha("2000", 0.9)), 1, null))
                .extracting("id").containsExactlyInAnyOrder(274, 300);
        assertThat(resolvedor.resolver("MODELO", List.of(linha("SUPER", 0.9)), 1, null))
                .extracting("id").containsExactly(274);
    }

    // Caso real da fábrica: a foto de país saiu "MADEANCHENA" (dois caracteres trocados) e o
    // casamento exato não devolvia nada. País é vocabulário fechado de 42 itens — tolerar dois
    // caracteres aqui é seguro e resolve; a escolha continua sendo do operador.
    @Test
    void paisComDoisCaracteresTrocadosViraCandidatoAproximado() {
        when(paises.findAll()).thenReturn(List.of(pais(4, "CHINA"), pais(1, "BRASIL"), pais(5, "JAPAO")));
        var catalogo = resolvedor.snapshot("PAIS", null, null);
        assertThat(resolvedor.resolverSnapshot("PAIS", List.of(linha("MADEANCHENA", 0.87)), catalogo))
                .as("casamento exato não deve inventar nada").isEmpty();
        assertThat(resolvedor.resolverAproximado("PAIS", List.of(linha("MADEANCHENA", 0.87)), catalogo, null))
                .singleElement().satisfies(candidato -> assertThat(candidato.id()).isEqualTo(4));
    }

    // Leituras reais das fotos do pneu HIFLY SUPER 2000 tiradas na fábrica. O reconhecedor troca
    // glifos que ele não distingue no relevo (F por P, 0 por D/G) -- é o mesmo texto, desenhado de
    // um jeito que a imagem não permite separar. Tratar isso como equivalente destrava o modelo.
    @Test
    void modeloComGlifosConfundidosViraCandidatoAproximado() {
        MarcaModel hifly = marca(25, "HIFLY");
        when(marcas.existsById(25)).thenReturn(true);
        when(marcas.findById(25)).thenReturn(Optional.of(hifly));
        when(modelos.findAll()).thenReturn(List.of(modelo(274, "HIFLY SUPER 2000", hifly)));
        var catalogo = resolvedor.snapshot("MODELO", 25, null);
        for (String leitura : List.of("SUFER2DDG", "SUFERGOOG", "SUPER2DE")) {
            assertThat(resolvedor.resolverSnapshot("MODELO", List.of(linha(leitura, 0.72)), catalogo, 25))
                    .as("casamento exato não pode aceitar leitura trocada: " + leitura).isEmpty();
            assertThat(resolvedor.resolverAproximado("MODELO", List.of(linha(leitura, 0.72)), catalogo, 25))
                    .as(leitura).singleElement()
                    .satisfies(candidato -> assertThat(candidato.id()).isEqualTo(274));
        }
    }

    // Limite do que a classe de glifo cobre: leitura degradada demais continua sem candidato.
    @Test
    void leituraMuitoDegradadaDeModeloNaoViraCandidato() {
        MarcaModel hifly = marca(25, "HIFLY");
        when(marcas.existsById(25)).thenReturn(true);
        when(marcas.findById(25)).thenReturn(Optional.of(hifly));
        when(modelos.findAll()).thenReturn(List.of(modelo(274, "HIFLY SUPER 2000", hifly)));
        var catalogo = resolvedor.snapshot("MODELO", 25, null);
        assertThat(resolvedor.resolverAproximado("MODELO", List.of(linha("SURENEGEE", 0.61)),
                catalogo, 25)).isEmpty();
    }

    // Leitura real da fábrica: o marcador saiu com dois erros ("MADGAN") e o país com um
    // ("CHENA"). Somados dão três e estouram a tolerância; separados, o país casa com folga.
    @Test
    void paisCasaMesmoComOMarcadorCorrompido() {
        when(paises.findAll()).thenReturn(
                List.of(pais(4, "CHINA"), pais(1, "BRASIL"), pais(12, "THAILAND"), pais(32, "CHILE")));
        var catalogo = resolvedor.snapshot("PAIS", null, null);
        for (String leitura : List.of("MADGANCHENA", "MADEANCHENA")) {
            assertThat(resolvedor.resolverAproximado("PAIS", List.of(linha(leitura, 0.85)), catalogo, null))
                    .as(leitura).extracting("id").containsExactly(4);
        }
    }

    // O prefixo precisa parecer o marcador: sem isso, qualquer texto terminado parecido com um
    // país viraria candidato.
    @Test
    void sufixoParecidoComPaisSemMarcadorNaoCasa() {
        when(paises.findAll()).thenReturn(List.of(pais(4, "CHINA"), pais(32, "CHILE")));
        var catalogo = resolvedor.snapshot("PAIS", null, null);
        assertThat(resolvedor.resolverAproximado("PAIS", List.of(linha("POLYESTERCHENA", 0.9)),
                catalogo, null)).isEmpty();
    }

    @Test
    void paisMuitoDiferenteNaoViraCandidatoAproximado() {
        when(paises.findAll()).thenReturn(List.of(pais(4, "CHINA"), pais(1, "BRASIL")));
        var catalogo = resolvedor.snapshot("PAIS", null, null);
        assertThat(resolvedor.resolverAproximado("PAIS", List.of(linha("PLIES TREAD POLYESTER", 0.99)),
                catalogo, null)).isEmpty();
    }

    // A spec proíbe aproximar dígito: medida e DOT não entram nessa lista em hipótese nenhuma.
    @Test
    void medidaEDotNuncaRecebemAproximacao() {
        when(medidas.findAll()).thenReturn(List.of(medida(5, "175/70 R14")));
        var catalogoMedida = resolvedor.snapshot("MEDIDA", null, null);
        assertThat(resolvedor.resolverAproximado("MEDIDA", List.of(linha("175/70R15", 0.99)),
                catalogoMedida, null)).isEmpty();
        assertThat(resolvedor.resolverAproximado("DOT", List.of(linha("3924", 0.99)), Map.of(), null))
                .isEmpty();
    }

    @Test
    void leituraCurtaDemaisNaoAproxima() {
        when(marcas.findAll()).thenReturn(List.of(marca(25, "HIFLY")));
        var catalogo = resolvedor.snapshot("MARCA", null, null);
        assertThat(resolvedor.resolverAproximado("MARCA", List.of(linha("HI", 0.99)), catalogo, null))
                .isEmpty();
    }

    @Test
    void aproximadosSaemOrdenadosPelaDistanciaELimitadosACinco() {
        when(marcas.findAll()).thenReturn(List.of(marca(1, "GOODYEAR"), marca(2, "GOODYEAB"),
                marca(3, "BRIDGESTONE")));
        var catalogo = resolvedor.snapshot("MARCA", null, null);
        var aproximados = resolvedor.resolverAproximado("MARCA", List.of(linha("GOODYEAR", 0.9)),
                catalogo, null);
        assertThat(aproximados).hasSizeLessThanOrEqualTo(5);
        assertThat(aproximados.get(0).id()).as("o mais próximo vem primeiro").isEqualTo(1);
        assertThat(aproximados).extracting("id").doesNotContain(3);
    }

    @Test
    void modeloComDescricaoVaziaNuncaCasa() {
        MarcaModel hifly = marca(1, "HIFLY");
        when(marcas.existsById(1)).thenReturn(true);
        when(marcas.findById(1)).thenReturn(Optional.of(hifly));
        when(modelos.findAll()).thenReturn(List.of(modelo(829, "", hifly)));
        assertThat(resolvedor.resolver("MODELO", List.of(linha("HF-805", 0.99)), 1, null)).isEmpty();
    }

    @Test
    void trechoPresenteEmDoisModelosDaMarcaFicaAmbiguo() {
        MarcaModel aptany = marca(1, "APTANY");
        when(marcas.existsById(1)).thenReturn(true);
        when(marcas.findById(1)).thenReturn(Optional.of(aptany));
        when(modelos.findAll()).thenReturn(List.of(modelo(844, "APTANY TRACFORCE RL108", aptany),
                modelo(845, "APTANY TRACFORCE RL108", aptany)));
        assertThat(resolvedor.resolver("MODELO", List.of(linha("TRACFORCE RL108", 0.9)), 1, null))
                .extracting("id").containsExactlyInAnyOrder(844, 845);
    }

    // Há 14 pares de marcas em que um nome está contido no outro; casar por substring trocaria uma
    // marca pela outra sem que ninguém percebesse.
    @Test
    void marcaNaoCasaPorSubstringDeOutraMarca() {
        when(marcas.findAll()).thenReturn(List.of(marca(1, "LANDSAIL"), marca(2, "ANDSAIL")));
        assertThat(resolvedor.resolver("MARCA", List.of(linha("LANDSAIL", 0.97)), null, null))
                .singleElement().satisfies(candidato -> assertThat(candidato.id()).isEqualTo(1));
        assertThat(resolvedor.resolver("MARCA", List.of(linha("ANDSAIL", 0.97)), null, null))
                .singleElement().satisfies(candidato -> assertThat(candidato.id()).isEqualTo(2));
    }

    @Test
    void marcaDuplicadaNoCatalogoFicaAmbigua() {
        when(marcas.findAll()).thenReturn(List.of(marca(123, "ACCELERA"), marca(213, "ACCELERA")));
        assertThat(resolvedor.resolver("MARCA", List.of(linha("ACCELERA", 0.98)), null, null))
                .extracting("id").containsExactlyInAnyOrder(123, 213);
    }

    @Test
    void dotAmbiguoPreservaTodosOsCandidatosSemEscolher() {
        var candidatos = resolvedor.resolver("DOT", List.of(linha("3625 0126", 0.99)), null, null);
        assertThat(candidatos).extracting("texto").containsExactlyInAnyOrder("3625", "0126");
    }
}
