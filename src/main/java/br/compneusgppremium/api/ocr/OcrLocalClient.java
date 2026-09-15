package br.compneusgppremium.api.ocr;
import br.compneusgppremium.api.config.OcrLocalProperties;
import br.compneusgppremium.api.exception.CadastroPneuException;
import java.time.Duration;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.web.client.RestClientResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
@Component
@Slf4j
public class OcrLocalClient {
    private final OcrLocalProperties configuracao;
    private final CircuitoOcr circuito;
    private final RestClient cliente;
    private final DiagnosticoOcr diagnostico;
    public OcrLocalClient(final OcrLocalProperties configuracao, final CircuitoOcr circuito, final DiagnosticoOcr diagnostico) {
        this.configuracao = configuracao;
        this.circuito = circuito;
        this.diagnostico = diagnostico;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(configuracao.timeoutSegundos()));
        factory.setReadTimeout(Duration.ofSeconds(configuracao.timeoutSegundos()));
        this.cliente = RestClient.builder().baseUrl(configuracao.url().toString()).requestFactory(factory).build();
    }
    public OcrResposta ler(final String campo, final String base64) {
        if (!configuracao.habilitado() || configuracao.token() == null || configuracao.token().isBlank()) {
            log.warn("ocr nao_enviado id={} campo={} habilitado={} tokenConfigurado={}", MDC.get("leituraId"),
                    campo, configuracao.habilitado(), configuracao.token() != null && !configuracao.token().isBlank());
            throw indisponivel();
        }
        // Serviço reconhecidamente fora do ar: recusar agora evita somar o timeout à espera do operador.
        if (!circuito.permitirChamada()) {
            log.warn("ocr circuito_aberto id={} campo={}", MDC.get("leituraId"), campo);
            throw indisponivel();
        }
        OcrResposta resposta = chamar(campo, base64);
        if (log.isInfoEnabled()) {
            // Mantém o retorno anterior à validação em uma linha, sem incluir a foto ou o token da requisição.
            log.info("ocr retorno_recebido id={} campo={} resposta={}", MDC.get("leituraId"), campo,
                    String.valueOf(resposta).replace("\r", "\\r").replace("\n", "\\n"));
        }
        if (!contratoAtendido(resposta, campo)) {
            circuito.registrarFalha();
            log.warn("ocr contrato_invalido id={} campo={}", MDC.get("leituraId"), campo);
            throw indisponivel();
        }
        log.info("ocr retorno_validado id={} campo={} linhas={} duracaoMotorMs={} versaoModelo={}",
                MDC.get("leituraId"), campo, resposta.linhas().size(), resposta.duracaoMs(), resposta.versaoModelo());
        circuito.registrarSucesso();
        return resposta;
    }
    private OcrResposta chamar(final String campo, final String base64) {
        var corpo = Map.of("campo", campo, "foto_base64", base64);
        diagnostico.registrar(corpo);
        long inicio = System.nanoTime();
        log.info("ocr envio id={} campo={} host={} porta={} rota=/v1/reconhecer timeoutSegundos={}",
                MDC.get("leituraId"), campo, configuracao.url().getHost(), configuracao.url().getPort(),
                configuracao.timeoutSegundos());
        try {
            return cliente.post().uri("/v1/reconhecer")
                    .headers(headers -> {
                        headers.setBearerAuth(configuracao.token());
                        if (MDC.get("leituraId") != null) headers.set("X-Leitura-Id", MDC.get("leituraId"));
                    })
                    .body(corpo).retrieve().body(OcrResposta.class);
        } catch (RestClientException erro) {
            circuito.registrarFalha();
            log.warn("ocr falha id={} campo={} tipo={} http={} duracaoMs={}", MDC.get("leituraId"), campo,
                    erro.getClass().getSimpleName(),
                    erro instanceof RestClientResponseException http ? http.getStatusCode().value() : null,
                    (System.nanoTime() - inicio) / 1_000_000);
            throw indisponivel();
        } finally {
            log.info("ocr chamada_encerrada id={} campo={} duracaoMs={}", MDC.get("leituraId"), campo,
                    (System.nanoTime() - inicio) / 1_000_000);
        }
    }
    private boolean contratoAtendido(final OcrResposta resposta, final String campo) {
        if (resposta == null || !campo.equals(resposta.campo()) || !preenchido(resposta.versaoModelo())) {
            return false;
        }
        return switch (resposta.motor() == null ? "" : resposta.motor()) {
            case "PADDLEOCR" -> resposta.linhas().stream().allMatch(linha -> linha.escore() != null
                    && Double.isFinite(linha.escore()) && linha.escore() >= 0 && linha.escore() <= 1);
            case "OLLAMA_LOCAL" -> preenchido(resposta.versaoBiblioteca())
                    && preenchido(resposta.versaoPreprocessamento())
                    && resposta.linhas().stream().allMatch(linha -> linha.escore() == null && linha.regiao().isEmpty());
            default -> false;
        };
    }
    private boolean preenchido(final String valor) {
        return valor != null && !valor.isBlank();
    }
    private CadastroPneuException indisponivel() {
        return new CadastroPneuException("OCR_INDISPONIVEL", "OCR indisponível; informe o campo manualmente.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
