package br.compneusgppremium.api.ocr;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public record OcrResposta(String motor, String versaoBiblioteca, String versaoModelo,
        String campo, List<Linha> linhas, long duracaoMs, String versaoPreprocessamento) {
    public OcrResposta(String motor, String versaoBiblioteca, String versaoModelo,
            String campo, List<Linha> linhas, long duracaoMs) {
        this(motor, versaoBiblioteca, versaoModelo, campo, linhas, duracaoMs, null);
    }
    public OcrResposta {
        linhas = List.copyOf(linhas);
    }
    public record Linha(String texto, double escore, List<List<Integer>> regiao) {
        public Linha {
            regiao = regiao.stream().map(List::copyOf).toList();
        }
    }
}
