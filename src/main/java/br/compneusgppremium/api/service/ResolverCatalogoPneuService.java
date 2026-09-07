package br.compneusgppremium.api.service;
import br.compneusgppremium.api.controller.dto.ResultadoLeituraDTO.Candidato;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.ocr.OcrResposta;
import br.compneusgppremium.api.repository.MarcaRepository;
import br.compneusgppremium.api.repository.MedidaRepository;
import br.compneusgppremium.api.repository.ModeloRepository;
import br.compneusgppremium.api.repository.PaisRepository;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResolverCatalogoPneuService {
    private final MarcaRepository marcas;
    private final ModeloRepository modelos;
    private final MedidaRepository medidas;
    private final PaisRepository paises;
    private final ValidacaoFormatoPneuService formatos;
    public List<Candidato> resolver(final String campo, final List<OcrResposta.Linha> linhas,
            final Integer marcaId, final Integer modeloId) {
        Map<Integer, String> catalogo = snapshot(campo, marcaId, modeloId);
        return resolverSnapshot(campo, linhas, catalogo);
    }
    public Map<Integer, String> snapshot(final String campo, final Integer marcaId, final Integer modeloId) {
        if (marcaId != null && !marcas.existsById(marcaId)) {
            throw contexto();
        }
        if (modeloId != null) {
            var modelo = modelos.findById(modeloId).orElseThrow(this::contexto);
            if (marcaId != null && (modelo.getMarca() == null || !marcaId.equals(modelo.getMarca().getId()))) {
                throw contexto();
            }
        }
        Map<Integer, String> catalogo = new LinkedHashMap<>();
        switch (campo) {
            case "MARCA" -> marcas.findAll().forEach(item -> catalogo.put(item.getId(), item.getDescricao()));
            case "MODELO" -> {
                if (marcaId == null) {
                    throw contexto();
                }
                modelos.findAll().forEach(item -> {
                    if (item.getMarca() != null && marcaId.equals(item.getMarca().getId())) {
                        catalogo.put(item.getId(), item.getDescricao());
                    }
                });
            }
            case "MEDIDA" -> medidas.findAll().forEach(item -> catalogo.put(item.getId(), item.getDescricao()));
            case "PAIS" -> paises.findAll().forEach(item -> catalogo.put(item.getId(), item.getDescricao()));
            case "DOT" -> { }
            default -> throw new CadastroPneuException("CAMPO_INVALIDO", "Campo de leitura inválido.");
        }
        return java.util.Collections.unmodifiableMap(new java.util.TreeMap<>(catalogo));
    }
    public List<Candidato> resolverSnapshot(final String campo, final List<OcrResposta.Linha> linhas,
            final Map<Integer, String> catalogo) {
        if ("DOT".equals(campo)) { return resolverDot(linhas); }
        Map<Integer, Candidato> candidatos = new LinkedHashMap<>();
        for (OcrResposta.Linha linha : linhas) {
            if (linha.texto() == null || !Double.isFinite(linha.escore())) {
                continue;
            }
            String texto = linha.texto();
            if ("PAIS".equals(campo)) {
                if (!texto.toUpperCase(Locale.ROOT).startsWith("MADE IN ")) {
                    continue;
                }
                texto = texto.substring("MADE IN ".length());
            }
            String normalizado = normalizar(campo, texto);
            for (Map.Entry<Integer, String> item : catalogo.entrySet()) {
                if (!normalizado.isEmpty() && normalizado.equals(normalizar(campo, item.getValue()))) {
                    candidatos.merge(item.getKey(), new Candidato(item.getKey(), item.getValue(), linha.escore()),
                            (anterior, atual) -> anterior.escore() >= atual.escore() ? anterior : atual);
                }
            }
        }
        return List.copyOf(candidatos.values());
    }
    private List<Candidato> resolverDot(final List<OcrResposta.Linha> linhas) {
        Map<String, Candidato> candidatos = new LinkedHashMap<>();
        Pattern formato = Pattern.compile("(?<![A-Z0-9])([0-9]{4})(?![A-Z0-9])");
        for (OcrResposta.Linha linha : linhas) {
            if (linha.texto() == null || !Double.isFinite(linha.escore())) {
                continue;
            }
            var matcher = formato.matcher(linha.texto().toUpperCase(Locale.ROOT));
            while (matcher.find()) {
                String dot = matcher.group(1);
                try {
                    formatos.validarDot(dot);
                    candidatos.putIfAbsent(dot, new Candidato(null, dot, linha.escore()));
                } catch (CadastroPneuException erro) {
                    // Candidato inválido é rejeitado; o texto original permanece na evidência da tentativa.
                    if (!erro.getCodigo().startsWith("DOT_")) {
                        throw erro;
                    }
                }
            }
        }
        return List.copyOf(candidatos.values());
    }
    private String normalizar(final String campo, final String valor) {
        if (valor == null) {
            return "";
        }
        if ("MEDIDA".equals(campo)) {
            return formatos.normalizarMedida(valor);
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
    private CadastroPneuException contexto() {
        return new CadastroPneuException("CONTEXTO_INVALIDO", "Confira a marca e o modelo antes da leitura.");
    }
}
