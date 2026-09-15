package br.compneusgppremium.api.ocr;

import br.compneusgppremium.api.config.OcrLocalProperties;
import br.compneusgppremium.api.exception.CadastroPneuException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OcrLocalClientTest {
    private final ObjectMapper json = new ObjectMapper();
    private final CircuitoOcr circuito = mock(CircuitoOcr.class);
    private HttpServer servidor;
    private OcrLocalClient cliente;

    @BeforeEach
    void iniciarServicoLocal() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.start();
        var configuracao = new OcrLocalProperties(
                URI.create("http://127.0.0.1:" + servidor.getAddress().getPort()), "token-teste", 2,
                true, Set.of("MODELO"), 0.9, 8388608, 20000000, 3, 30);
        when(circuito.permitirChamada()).thenReturn(true);
        cliente = new OcrLocalClient(configuracao, circuito, mock(DiagnosticoOcr.class));
    }

    @AfterEach
    void encerrarServicoLocal() {
        servidor.stop(0);
    }

    @Test
    void desserializaTextoDoOllamaSemFabricarEscoreOuRegiao() throws Exception {
        var resposta = json.readValue("""
                {"motor":"OLLAMA_LOCAL","versaoBiblioteca":"0.17.1","versaoModelo":"sha256-config",
                 "campo":"MODELO","linhas":[{"texto":"SUPER 2000","escore":null,"regiao":[]}],
                 "duracaoMs":1234,"versaoPreprocessamento":"ollama-vision-rgb-v1"}
                """, OcrResposta.class);
        assertThat(resposta.linhas().get(0).texto()).isEqualTo("SUPER 2000");
        assertThat(resposta.linhas().get(0).escore()).isNull();
        assertThat(resposta.linhas().get(0).regiao()).isEmpty();
        assertThat(json.readTree(json.writeValueAsBytes(resposta)).at("/linhas/0/escore").isNull()).isTrue();
    }

    @Test
    void aceitaOllamaLocalComVersoesESemConfiancaInventada() throws Exception {
        responder(resposta("OLLAMA_LOCAL", null, List.of()));
        var leitura = cliente.ler("MODELO", "Zm90bw==");
        assertThat(leitura.motor()).isEqualTo("OLLAMA_LOCAL");
        assertThat(leitura.linhas().get(0).escore()).isNull();
        verify(circuito).registrarSucesso();
        verify(circuito, never()).registrarFalha();
    }

    @Test
    void preservaContratoPaddleComEscoreNumerico() throws Exception {
        responder(resposta("PADDLEOCR", 0.98, List.of()));
        assertThat(cliente.ler("MODELO", "Zm90bw==").linhas().get(0).escore()).isEqualTo(0.98);
        verify(circuito).registrarSucesso();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"OLLAMA", "OLLAMA_CLOUD", "PROVEDOR_EXTERNO", ""})
    void recusaMotorForaDaListaConhecida(final String motor) throws Exception {
        responder(resposta(motor, null, List.of()));
        verificarRecusa();
    }

    @Test
    void recusaOllamaComConfiancaOuCaixaInventada() throws Exception {
        responder(resposta("OLLAMA_LOCAL", 0.99, List.of()));
        verificarRecusa();
        servidor.removeContext("/v1/reconhecer");
        responder(resposta("OLLAMA_LOCAL", null, List.of(List.of(0, 0), List.of(10, 10))));
        assertThatThrownBy(() -> cliente.ler("MODELO", "Zm90bw==")).isInstanceOf(CadastroPneuException.class);
    }

    @Test
    void recusaOllamaSemPreprocessamentoVersionado() throws Exception {
        responder(new OcrResposta("OLLAMA_LOCAL", "runtime-teste", "sha256-config", "MODELO",
                List.of(), 10, null));
        verificarRecusa();
    }

    @Test
    void recusaOllamaSemVersaoDoRuntime() throws Exception {
        responder(new OcrResposta("OLLAMA_LOCAL", "", "sha256-config", "MODELO", List.of(), 10,
                "ollama-vision-rgb-v1"));
        verificarRecusa();
    }

    @Test
    void recusaPaddleSemEscoreEmVezDeConverterNuloEmZero() throws Exception {
        responder(resposta("PADDLEOCR", null, List.of()));
        verificarRecusa();
    }

    private OcrResposta resposta(final String motor, final Double escore, final List<List<Integer>> regiao) {
        return new OcrResposta(motor, "runtime-teste", "sha256-config", "MODELO",
                List.of(new OcrResposta.Linha("SUPER 2000", escore, regiao)), 10, "ollama-vision-rgb-v1");
    }

    private void responder(final OcrResposta resposta) throws Exception {
        byte[] corpo = json.writeValueAsBytes(resposta);
        servidor.createContext("/v1/reconhecer", requisicao -> {
            try (requisicao) {
                requisicao.getRequestBody().readAllBytes();
                requisicao.getResponseHeaders().set("Content-Type", "application/json");
                requisicao.sendResponseHeaders(200, corpo.length);
                requisicao.getResponseBody().write(corpo);
            }
        });
    }

    private void verificarRecusa() {
        assertThatThrownBy(() -> cliente.ler("MODELO", "Zm90bw=="))
                .isInstanceOfSatisfying(CadastroPneuException.class, erro -> {
                    assertThat(erro.getCodigo()).isEqualTo("OCR_INDISPONIVEL");
                    assertThat(erro.getStatus().value()).isEqualTo(503);
                });
        verify(circuito).registrarFalha();
        verify(circuito, never()).registrarSucesso();
    }
}
