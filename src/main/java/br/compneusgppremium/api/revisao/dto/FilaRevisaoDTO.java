package br.compneusgppremium.api.revisao.dto;
import java.util.List;
public record FilaRevisaoDTO(List<ItemRevisaoDTO> itens, int pagina, long total, boolean temProxima) {
    public FilaRevisaoDTO { itens = List.copyOf(itens); }
}
