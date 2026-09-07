package br.compneusgppremium.api.controller.dto;
import java.util.List;
public record ResultadoLeituraDTO(String campo, String estado, List<String> motivos, String textoOriginal,
        Integer itemSugeridoId, String valorSugerido, List<Candidato> candidatos, Double escoreOcrBruto,
        Double probabilidadeCalibrada, String mensagem, String versaoModelo) {
    public ResultadoLeituraDTO {
        motivos = List.copyOf(motivos);
        candidatos = List.copyOf(candidatos);
    }
    public record Candidato(Integer id, String texto, double escore) {}
}
