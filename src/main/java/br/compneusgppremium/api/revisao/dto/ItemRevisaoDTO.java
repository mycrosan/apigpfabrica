package br.compneusgppremium.api.revisao.dto;
import java.time.Instant;
import java.util.List;
public record ItemRevisaoDTO(String id, String campoSolicitado, String origem, String estado,
        Long versao, Instant criadaEm, boolean reavaliar, boolean pendenciaCadastro,
        boolean podeRevisar, String etapa, int largura, int altura, List<List<Integer>> regiao,
        RespostaRevisaoDTO minhaResposta, ComparacaoDTO comparacao) {
    public ItemRevisaoDTO { regiao = regiao.stream().map(List::copyOf).toList(); }
    public record ComparacaoDTO(String leituraOriginal, String sugestao, String cadastro,
            String versaoModelo, List<RespostaRevisaoDTO> revisoes) {
        public ComparacaoDTO { revisoes = List.copyOf(revisoes); }
    }
}
