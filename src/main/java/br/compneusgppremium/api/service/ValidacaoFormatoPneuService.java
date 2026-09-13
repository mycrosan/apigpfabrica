package br.compneusgppremium.api.service;

import br.compneusgppremium.api.config.PoliticaPneuProperties;
import br.compneusgppremium.api.exception.CadastroPneuException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
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
    // Padrões de BUSCA da medida dentro da linha lida. O flanco do pneu traz índice de carga,
    // sufixo C e outros atributos coladas à medida, então exigir que a linha inteira seja a medida
    // (validarMedida) não serve para leitura — é o mesmo motivo de resolverDot buscar dentro da linha.
    // Os grupos capturam só a identidade que distingue um item do catálogo: construção (Z), uso (C),
    // prefixo P/LT e índice de carga/velocidade ficam de fora porque não diferenciam medida.
    private static final Map<String, Pattern> BUSCA_MEDIDA = Map.of(
            "METRICA_RADIAL", Pattern.compile(
                    "(?<![0-9./])(?:P|LT)?\\s*([1-9][0-9]{2})\\s*/\\s*([1-9][0-9])\\s*Z?\\s*R"
                    + "\\s*([1-9][0-9](?:\\.5)?)(?![0-9.])"),
            "POLEGADAS_RADIAL", Pattern.compile(
                    "(?<![0-9./A-Z])([1-9][0-9]?(?:\\.[0-9]{1,2})?)\\s*R\\s*([1-9][0-9](?:\\.5)?)(?![0-9.])"),
            "FLUTUACAO", Pattern.compile(
                    "(?<![0-9./A-Z])([1-9][0-9])\\s*X\\s*([1-9][0-9]?(?:\\.[0-9]{1,2})?)\\s*R"
                    + "\\s*([1-9][0-9])(?![0-9.])"));
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

    /**
     * Medidas encontradas dentro do texto, na forma canônica usada para casar com o catálogo.
     * Serve aos dois lados da comparação: a linha lida na foto e a descrição cadastrada.
     * Nenhum dígito é corrigido nem completado — o que não casa o padrão simplesmente não é devolvido.
     */
    public Set<String> extrairMedidasCanonicas(final String texto) {
        String normalizado = normalizarParaBusca(texto);
        Set<String> encontradas = new LinkedHashSet<>();
        for (String familia : politica.familiasMedida()) {
            Pattern busca = BUSCA_MEDIDA.get(familia);
            if (busca == null) {
                continue;
            }
            var achado = busca.matcher(normalizado);
            while (achado.find()) {
                encontradas.add(canonizar(familia, achado));
            }
        }
        return encontradas;
    }

    // Diferente de normalizarMedida: preserva um espaço entre os grupos. O espaço é a fronteira que
    // separa a medida do índice de carga ("205/55R16 91V"); removê-lo antes da busca colaria o 91
    // no aro e faria a leitura inteira ser descartada.
    private String normalizarParaBusca(final String texto) {
        if (texto == null) {
            return "";
        }
        return texto.toUpperCase(Locale.ROOT).replace(',', '.').replace('×', 'X')
                .replaceAll("\\s+", " ").trim();
    }

    private String canonizar(final String familia, final Matcher achado) {
        return switch (familia) {
            case "METRICA_RADIAL" -> achado.group(1) + "/" + achado.group(2) + "R" + achado.group(3);
            case "POLEGADAS_RADIAL" -> achado.group(1) + "R" + achado.group(2);
            case "FLUTUACAO" -> achado.group(1) + "X" + achado.group(2) + "R" + achado.group(3);
            default -> achado.group();
        };
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
