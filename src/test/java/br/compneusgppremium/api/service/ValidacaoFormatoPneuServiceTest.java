package br.compneusgppremium.api.service;

import br.compneusgppremium.api.config.PoliticaPneuProperties;
import br.compneusgppremium.api.exception.CadastroPneuException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidacaoFormatoPneuServiceTest {
    private ValidacaoFormatoPneuService servico(final String data, final boolean validarSemana) {
        ZoneId fuso = ZoneId.of("America/Sao_Paulo");
        return new ValidacaoFormatoPneuService(new PoliticaPneuProperties(fuso, 2000, validarSemana, 0,
                Set.of("PASSEIO"), Set.of("METRICA_RADIAL", "POLEGADAS_RADIAL", "FLUTUACAO")),
                Clock.fixed(Instant.parse(data), fuso));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"0026", "5426", "0127", "232", "23233", "DOT2323", "ABCD", "２３２３"})
    void rejeitaDotInvalido(final String dot) {
        assertThrows(CadastroPneuException.class, () -> servico("2026-09-05T12:00:00Z", false).validarDot(dot));
    }

    @Test
    void controlaBloqueioDeSemanaFuturaPorPolitica() {
        assertDoesNotThrow(() -> servico("2026-09-05T12:00:00Z", false).validarDot("5326"));
        assertThrows(CadastroPneuException.class, () -> servico("2026-09-05T12:00:00Z", true).validarDot("5326"));
    }

    @Test
    void trataViradaDeAnoSemAceitarSemana53DoNovoAno() {
        assertThrows(CadastroPneuException.class, () -> servico("2021-01-01T12:00:00Z", true).validarDot("5321"));
        assertDoesNotThrow(() -> servico("2021-01-01T12:00:00Z", true).validarDot("5320"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"205/55R16", "295/80R22.5", "7.50R16", "31X10.50R15LT", "205 / 55 r16"})
    void aceitaFamiliasConfiguradas(final String medida) {
        assertDoesNotThrow(() -> servico("2026-09-05T12:00:00Z", false).validarMedida(medida));
    }

    @ParameterizedTest
    @ValueSource(strings = {"205/?5R16", "205/55R16 OU 205/65R16", "999", "205/55"})
    void naoCompletaMedidaParcial(final String medida) {
        assertThrows(CadastroPneuException.class, () -> servico("2026-09-05T12:00:00Z", false).validarMedida(medida));
    }

    @Test
    void preservaDigitosNaNormalizacao() {
        assertEquals("205/65R16", servico("2026-09-05T12:00:00Z", false).normalizarMedida("205 / 65 r16"));
    }
}
