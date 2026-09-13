package br.compneusgppremium.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Nomes de país como aparecem no flanco do pneu, mapeados para a descrição cadastrada no catálogo.
 *
 * <p>O catálogo tem 42 países e já está quase todo em inglês; a lista existe para os poucos casos em
 * português ({@code BRASIL}, {@code JAPAO}), para um erro de grafia herdado ({@code COLONBIA}) e para
 * apelidos do Reino Unido, que o catálogo guarda duas vezes.
 *
 * <p>Regra que o arquivo precisa respeitar, verificada na carga: o sinônimo mapeia <b>leitura completa
 * para item completo</b>. Nenhuma chave é fragmento de nome — {@code BRAZ} não vira {@code BRASIL} —
 * porque completar caracteres ausentes da imagem é proibido pela política de leitura.
 */
@Service
public class SinonimosPaisService {

    private static final String RECURSO = "catalogo/sinonimos-pais.json";
    // Indicação explícita de fabricação. O prefixo é opcional na leitura: em foto de perto o marcador
    // costuma ficar fora do quadro, e quem protege contra ruído é o vocabulário fechado do catálogo.
    // Sem exigir fronteira de palavra ao final: em foto real o OCR devolveu "MADEINCHINA" numa única
    // palavra, e exigir a fronteira deixava o marcador colado ao nome do país.
    private static final Pattern MARCADOR = Pattern.compile(
            "^(?:MADE\\s*IN|MFG\\s*IN|MANUFACTURED\\s*IN|FABRICADO\\s*N[OA])[\\s:-]*");

    private Map<String, List<String>> sinonimos = Map.of();
    private String versao = RECURSO;

    @PostConstruct
    void carregar() {
        try (InputStream entrada = new ClassPathResource(RECURSO).getInputStream()) {
            var conteudo = new ObjectMapper().readTree(entrada);
            var lidos = new LinkedHashMap<String, List<String>>();
            conteudo.path("sinonimos").fields().forEachRemaining(item -> {
                var alvos = new java.util.ArrayList<String>();
                item.getValue().forEach(alvo -> alvos.add(normalizar(alvo.asText())));
                lidos.put(normalizar(item.getKey()), List.copyOf(alvos));
            });
            validarChaves(lidos);
            sinonimos = Map.copyOf(lidos);
            versao = conteudo.path("versao").asText(RECURSO);
        } catch (IOException erro) {
            throw new IllegalStateException("Não foi possível carregar " + RECURSO, erro);
        }
    }

    /**
     * Descrições de catálogo que a leitura pode designar. Devolve o próprio texto quando não há
     * sinônimo — a correspondência direta continua sendo o caminho normal.
     */
    public Set<String> alvos(final String textoLido) {
        String lido = semMarcador(normalizar(textoLido));
        if (lido.isEmpty()) {
            return Set.of();
        }
        var encontrados = new java.util.LinkedHashSet<String>();
        encontrados.add(lido);
        encontrados.addAll(sinonimos.getOrDefault(lido, List.of()));
        return Set.copyOf(encontrados);
    }

    /** Caixa, acento e pontuação fora do caminho: {@code U.S.A.} e {@code USA} são o mesmo nome. */
    public String normalizar(final String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT).replaceAll("[.,'`]", "")
                .replaceAll("\\s+", " ").trim();
    }

    public String versao() {
        return versao;
    }

    private String semMarcador(final String texto) {
        return MARCADOR.matcher(texto).replaceFirst("").trim();
    }

    // Uma chave que é prefixo de outra só é aceitável quando ambas designam o mesmo item — caso de
    // "UNITED STATES" e "UNITED STATES OF AMERICA", dois nomes completos do mesmo país. Prefixo com
    // destino diferente faria uma leitura truncada valer como nome completo de OUTRO país.
    private void validarChaves(final Map<String, List<String>> lidos) {
        for (var chave : lidos.entrySet()) {
            if (chave.getKey().isBlank()) {
                throw new IllegalStateException(RECURSO + ": sinônimo com chave vazia.");
            }
            for (var outra : lidos.entrySet()) {
                boolean prefixoDeToken = !chave.getKey().equals(outra.getKey())
                        && outra.getKey().startsWith(chave.getKey() + " ");
                if (prefixoDeToken && !Set.copyOf(chave.getValue()).equals(Set.copyOf(outra.getValue()))) {
                    throw new IllegalStateException(
                            RECURSO + ": '" + chave.getKey() + "' é prefixo de '" + outra.getKey()
                            + "' e aponta para item diferente; leitura truncada viraria outro país.");
                }
            }
        }
    }
}
