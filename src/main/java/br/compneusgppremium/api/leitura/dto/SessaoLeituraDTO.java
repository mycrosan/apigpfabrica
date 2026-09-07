package br.compneusgppremium.api.leitura.dto;
import java.time.Instant;
import java.util.List;
import java.util.Map;
public record SessaoLeituraDTO(String id, Integer carcacaId, String grupoFisico, String status,
        Instant criadaEm, Long versao, Map<String, CampoConfirmadoDTO> confirmacoes,
        List<TentativaLeituraDTO> tentativas, String motivoAbandono) {
    public SessaoLeituraDTO {
        confirmacoes = Map.copyOf(confirmacoes); tentativas = List.copyOf(tentativas);
    }
}
