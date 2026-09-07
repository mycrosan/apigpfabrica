package br.compneusgppremium.api.revisao.service;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.revisao.dto.RevisarEntradaDTO;
import br.compneusgppremium.api.revisao.model.RespostaRevisao;
import java.util.*;
import org.springframework.stereotype.Service;
@Service
public class RegrasRevisaoService {
    public void validar(final RevisarEntradaDTO entrada, final int largura, final int altura, final boolean adjudicacao) {
        if (adjudicacao && (entrada.motivo() == null || entrada.motivo().isBlank())) {
            throw invalida("Explique a decisão de desempate.");
        }
        if (entrada.legibilidade() == RevisarEntradaDTO.Legibilidade.LEGIVEL) {
            if (entrada.campo() == null || entrada.transcricao() == null || entrada.transcricao().isBlank()) {
                throw invalida("Escolha o campo e transcreva somente o texto visível na região.");
            }
            validarRegiao(entrada.regiao(), largura, altura); return;
        }
        if (entrada.transcricao() != null && !entrada.transcricao().isBlank()) {
            throw invalida("Foto ilegível ou campo ausente não pode receber transcrição.");
        }
        if (!entrada.regiao().isEmpty()) { validarRegiao(entrada.regiao(), largura, altura); }
    }
    public String decidir(final List<RespostaRevisao> respostas) {
        var ultima = respostas.get(respostas.size() - 1);
        if (ultima.isAdjudicacao()) { return estado(ultima); }
        if (respostas.size() < 2) { return "PENDENTE"; }
        var primeira = respostas.get(0);
        if (!Objects.equals(primeira.getCampo(), ultima.getCampo())
                || !Objects.equals(primeira.getTranscricao(), ultima.getTranscricao())
                || !Objects.equals(primeira.getLegibilidade(), ultima.getLegibilidade())
                || !Objects.equals(primeira.getRegiaoJson(), ultima.getRegiaoJson())) { return "DIVERGENTE"; }
        return estado(ultima);
    }
    private String estado(final RespostaRevisao resposta) {
        return switch (resposta.getLegibilidade()) {
            case "LEGIVEL" -> "APROVADA";
            case "ILEGIVEL" -> "ILEGIVEL";
            default -> "DESCARTADA";
        };
    }
    public static void validarRegiao(final List<List<Integer>> regiao, final int largura, final int altura) {
        if (regiao == null || regiao.size() != 4) { throw invalida("Selecione uma região de texto na foto."); }
        for (var ponto : regiao) {
            if (ponto == null || ponto.size() != 2 || ponto.get(0) == null || ponto.get(1) == null
                    || ponto.get(0) < 0 || ponto.get(0) > largura || ponto.get(1) < 0 || ponto.get(1) > altura) {
                throw invalida("Região fora dos limites da foto.");
            }
        }
        long area = 0;
        for (int indice = 0; indice < 4; indice++) {
            var atual = regiao.get(indice); var proximo = regiao.get((indice + 1) % 4);
            area += (long) atual.get(0) * proximo.get(1) - (long) proximo.get(0) * atual.get(1);
        }
        if (Math.abs(area) < 2) { throw invalida("A região deve conter uma área de texto."); }
    }
    private static CadastroPneuException invalida(final String mensagem) {
        return new CadastroPneuException("REVISAO_INVALIDA", mensagem);
    }
}
