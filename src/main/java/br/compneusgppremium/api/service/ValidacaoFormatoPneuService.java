package br.compneusgppremium.api.service;

import br.compneusgppremium.api.config.PoliticaPneuProperties;
import br.compneusgppremium.api.exception.CadastroPneuException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidacaoFormatoPneuService {
    private static final int PRIMEIRO_ANO_DOT = 2000;
    private static final int MAXIMA_SEMANA = 53;
    private static final Map<String, Pattern> FORMATOS = Map.of(
            "METRICA_RADIAL", Pattern.compile("(?:P|LT)?[1-9][0-9]{2}/[1-9][0-9]R[1-9][0-9](?:\\.5)?C?"),
            "POLEGADAS_RADIAL", Pattern.compile("[1-9][0-9]?(?:\\.[0-9]{1,2})?R[1-9][0-9](?:\\.5)?C?"),
            "FLUTUACAO", Pattern.compile("[1-9][0-9]X[1-9][0-9]?(?:\\.[0-9]{1,2})?R[1-9][0-9](?:LT)?"));
    private final PoliticaPneuProperties politica;
    private final Clock relogio;

    public void validarDot(final String dot) {
        if (dot == null || !dot.matches("[0-9]{4}")) {
            throw new CadastroPneuException("DOT_INVALIDO", "DOT deve conter quatro dígitos: semana e ano.");
        }
        int semana = Integer.parseInt(dot.substring(0, 2));
        int ano = PRIMEIRO_ANO_DOT + Integer.parseInt(dot.substring(2));
        LocalDate hoje = LocalDate.now(relogio.withZone(politica.fuso()));
        if (semana < 1 || semana > MAXIMA_SEMANA || ano < politica.anoMinimo() || ano > hoje.getYear()) {
            throw new CadastroPneuException("DOT_INVALIDO", "Semana ou ano do DOT fora da faixa permitida.");
        }
        // Calendário ISO é uma convenção operacional; o bloqueio exige ativação explícita na política.
        if (politica.validarSemanaFutura() && ano == hoje.getYear()) {
            int semanaAtual = hoje.get(WeekFields.ISO.weekOfWeekBasedYear());
            int anoSemanal = hoje.get(WeekFields.ISO.weekBasedYear());
            int limite = semanaAtual + politica.toleranciaSemanas();
            if (anoSemanal < ano) {
                limite = politica.toleranciaSemanas();
            }
            if (anoSemanal > ano) {
                limite = MAXIMA_SEMANA;
            }
            if (semana > limite) {
                throw new CadastroPneuException("DOT_FUTURO", "Semana de fabricação futura; confira a foto do DOT.");
            }
        }
    }

    public String normalizarMedida(final String texto) {
        if (texto == null) {
            return "";
        }
        return texto.toUpperCase(Locale.ROOT).replaceAll("\\s", "").replace(',', '.').replace('×', 'X');
    }

    public void validarMedida(final String texto) {
        String medida = normalizarMedida(texto);
        boolean suportada = politica.familiasMedida().stream().map(FORMATOS::get)
                .filter(java.util.Objects::nonNull).anyMatch(formato -> formato.matcher(medida).matches());
        if (!suportada) {
            throw new CadastroPneuException("MEDIDA_NAO_SUPORTADA", "Formato de medida exige revisão do catálogo.");
        }
    }
}
