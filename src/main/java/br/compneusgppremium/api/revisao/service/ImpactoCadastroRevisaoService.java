package br.compneusgppremium.api.revisao.service;
import br.compneusgppremium.api.leitura.model.ConfirmacaoLeitura;
import br.compneusgppremium.api.revisao.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
public class ImpactoCadastroRevisaoService {
    private final ItemRevisaoRepository itens;
    @Transactional
    public void confirmacao(final ConfirmacaoLeitura confirmacao) {
        var dependentes = switch (confirmacao.getCampo()) {
            case "MARCA" -> java.util.Set.of("MODELO", "MEDIDA", "PAIS");
            case "MODELO" -> java.util.Set.of("MEDIDA", "PAIS");
            case "MEDIDA" -> java.util.Set.of("PAIS");
            default -> java.util.Set.<String>of();
        };
        for (var item : itens.findBySessaoIdOrderById(confirmacao.getSessaoId())) {
            if (dependentes.contains(item.getCampoSolicitado())) { item.setReavaliar(true); continue; }
            if (!confirmacao.getCampo().equals(item.getCampoSolicitado())) { continue; }
            if (item.getUltimaRespostaId() != null || item.getConfirmacaoId() != null) {
                item.setReavaliar(true); continue;
            }
            if (!java.util.Objects.equals(confirmacao.getTentativaId(), item.getTentativaId())) { continue; }
            item.setConfirmacaoId(confirmacao.getId()); item.setValorCadastro(confirmacao.getValorFinal());
            item.setCorrecao(!"IA_CONFIRMADA".equals(confirmacao.getOrigem()));
        }
    }
    @Transactional
    public void cadastroAlterado(final Integer carcacaId) {
        for (var item : itens.porCarcaca(carcacaId)) { item.setReavaliar(true); }
    }
}
