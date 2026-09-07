package br.compneusgppremium.api.ocr;

import br.compneusgppremium.api.config.OcrLocalProperties;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CircuitoOcrTest {
    private static final Instant INICIO = Instant.parse("2026-09-05T12:00:00Z");
    private final RelogioAjustavel relogio = new RelogioAjustavel(INICIO);
    private final CircuitoOcr circuito = new CircuitoOcr(configuracao(2, 30), relogio);

    private OcrLocalProperties configuracao(final int falhasParaAbrir, final int aberturaSegundos) {
        return new OcrLocalProperties(URI.create("http://localhost:8091"), "token-interno-teste", 15, true,
                Set.of(), 0.9, 8388608, 20000000, falhasParaAbrir, aberturaSegundos);
    }

    @Test
    void permaneceFechadoAntesDoLimiteDeFalhas() {
        assertThat(circuito.permitirChamada()).isTrue();
        circuito.registrarFalha();
        assertThat(circuito.aberto()).isFalse();
        assertThat(circuito.permitirChamada()).isTrue();
    }

    @Test
    void abreDepoisDasFalhasConsecutivasERecusaSemChamar() {
        circuito.registrarFalha();
        circuito.registrarFalha();
        assertThat(circuito.aberto()).isTrue();
        assertThat(circuito.permitirChamada()).isFalse();
    }

    @Test
    void sucessoIntercaladoNaoDeixaFalhasSeAcumularem() {
        circuito.registrarFalha();
        circuito.registrarSucesso();
        circuito.registrarFalha();
        assertThat(circuito.aberto()).isFalse();
        assertThat(circuito.permitirChamada()).isTrue();
    }

    @Test
    void liberaUmaSondaPorVezDepoisDaEspera() {
        circuito.registrarFalha();
        circuito.registrarFalha();
        relogio.avancar(Duration.ofSeconds(31));
        assertThat(circuito.permitirChamada()).isTrue();
        assertThat(circuito.permitirChamada()).isFalse();
    }

    @Test
    void sondaBemSucedidaFechaOCircuito() {
        circuito.registrarFalha();
        circuito.registrarFalha();
        relogio.avancar(Duration.ofSeconds(31));
        circuito.permitirChamada();
        circuito.registrarSucesso();
        assertThat(circuito.aberto()).isFalse();
        assertThat(circuito.permitirChamada()).isTrue();
    }

    @Test
    void sondaFalhaReabreOCircuitoPorNovaEspera() {
        circuito.registrarFalha();
        circuito.registrarFalha();
        relogio.avancar(Duration.ofSeconds(31));
        circuito.permitirChamada();
        circuito.registrarFalha();
        assertThat(circuito.aberto()).isTrue();
        assertThat(circuito.permitirChamada()).isFalse();
        relogio.avancar(Duration.ofSeconds(31));
        assertThat(circuito.permitirChamada()).isTrue();
    }

    /** Relógio controlado pelo teste: a espera do circuito não pode depender do tempo real da suíte. */
    private static final class RelogioAjustavel extends Clock {
        private Instant agora;

        private RelogioAjustavel(final Instant inicio) {
            this.agora = inicio;
        }

        private void avancar(final Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
