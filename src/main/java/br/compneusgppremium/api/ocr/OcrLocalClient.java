package br.compneusgppremium.api.ocr;
import br.compneusgppremium.api.config.OcrLocalProperties;
import br.compneusgppremium.api.exception.CadastroPneuException;
import java.time.Duration;
import java.util.Map;
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
    public OcrLocalClient(final OcrLocalProperties configuracao, final CircuitoOcr circuito) {
        this.configuracao = configuracao;
        this.circuito = circuito;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(configuracao.timeoutSegundos()));
        factory.setReadTimeout(Duration.ofSeconds(configuracao.timeoutSegundos()));
        this.cliente = RestClient.builder().baseUrl(configuracao.url().toString()).requestFactory(factory).build();
    }
    public OcrResposta ler(final String campo, final String base64) {
        if (!configuracao.habilitado() || configuracao.token() == null || configuracao.token().isBlank()) {
            throw indisponivel();
        }
        // Serviço reconhecidamente fora do ar: recusar agora evita somar o timeout à espera do operador.
        if (!circuito.permitirChamada()) {
            log.warn("Leitura do campo {} recusada sem chamada: circuito do OCR local aberto.", campo);
            throw indisponivel();
        }
        OcrResposta resposta = chamar(campo, base64);
        if (!contratoAtendido(resposta, campo)) {
            circuito.registrarFalha();
            log.warn("Resposta do OCR local fora do contrato para o campo {}; leitura descartada.", campo);
            throw indisponivel();
        }
        circuito.registrarSucesso();
        return resposta;
    }
    private OcrResposta chamar(final String campo, final String base64) {
        try {
            return cliente.post().uri("/v1/reconhecer")
                    .headers(headers -> headers.setBearerAuth(configuracao.token()))
                    .body(Map.of("campo", campo, "foto_base64", base64)).retrieve().body(OcrResposta.class);
        } catch (RestClientException erro) {
            circuito.registrarFalha();
            log.warn("Falha de transporte ao chamar o OCR local para o campo {}: {}", campo,
                    erro.getClass().getSimpleName(), erro);
            throw indisponivel();
        }
    }
    private boolean contratoAtendido(final OcrResposta resposta, final String campo) {
        return resposta != null && "PADDLEOCR".equals(resposta.motor()) && campo.equals(resposta.campo())
                && resposta.versaoModelo() != null && !resposta.versaoModelo().isBlank();
    }
    private CadastroPneuException indisponivel() {
        return new CadastroPneuException("OCR_INDISPONIVEL", "OCR indisponível; informe o campo manualmente.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }
}
