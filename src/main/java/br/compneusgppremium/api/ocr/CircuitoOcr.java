package br.compneusgppremium.api.ocr;

import br.compneusgppremium.api.config.OcrLocalProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Protege o serviço local de OCR quando ele está indisponível ou respondendo fora do contrato.
 *
 * <p>Sem essa proteção cada tentativa do operador aguarda o timeout completo da requisição antes de
 * cair no cadastro manual. Numa fila de fábrica isso transforma uma indisponibilidade do serviço em
 * atraso proporcional ao número de operadores, além de manter carga sobre um serviço já em falha.</p>
 *
 * <p>O estado vive em memória, por instância da API: é uma proteção de latência local, não um acordo
 * distribuído entre réplicas. O circuito nunca decide o valor de um campo; ele apenas antecipa a
 * mesma recusa que a chamada real produziria, mantendo o caminho manual como saída.</p>
 */
@Component
@Slf4j
public class CircuitoOcr {
    private final OcrLocalProperties configuracao;
    private final Clock relogio;
    private int falhasConsecutivas;
    private Instant liberadoEm;
    private boolean sondaEmCurso;

    public CircuitoOcr(final OcrLocalProperties configuracao, final Clock relogio) {
        this.configuracao = configuracao;
        this.relogio = relogio;
    }

    /**
     * Informa se a chamada ao serviço pode seguir agora.
     *
     * <p>Com o circuito aberto a resposta é negativa até o fim da espera. Passada a espera, apenas
     * uma sonda por vez é liberada; o resultado dessa sonda fecha ou reabre o circuito.</p>
     *
     * @return {@code true} quando a chamada deve ser executada.
     */
    public synchronized boolean permitirChamada() {
        if (liberadoEm == null) {
            return true;
        }
        if (relogio.instant().isBefore(liberadoEm)) {
            return false;
        }
        if (sondaEmCurso) {
            return false;
        }
        sondaEmCurso = true;
        return true;
    }

    /** Registra uma leitura bem-sucedida, fechando o circuito e zerando as falhas acumuladas. */
    public synchronized void registrarSucesso() {
        falhasConsecutivas = 0;
        sondaEmCurso = false;
        if (liberadoEm != null) {
            liberadoEm = null;
            log.info("Circuito do OCR local fechado após leitura bem-sucedida.");
        }
    }

    /** Registra uma falha do serviço, abrindo o circuito quando o limite configurado é atingido. */
    public synchronized void registrarFalha() {
        sondaEmCurso = false;
        falhasConsecutivas++;
        if (falhasConsecutivas < configuracao.falhasParaAbrir()) {
            return;
        }
        liberadoEm = relogio.instant().plus(Duration.ofSeconds(configuracao.aberturaSegundos()));
        log.warn("Circuito do OCR local aberto após {} falhas consecutivas; nova sonda a partir de {}.",
                falhasConsecutivas, liberadoEm);
    }

    /**
     * Indica se o circuito está recusando chamadas neste instante.
     *
     * @return {@code true} enquanto durar a espera após a abertura.
     */
    public synchronized boolean aberto() {
        return liberadoEm != null && relogio.instant().isBefore(liberadoEm);
    }
}
