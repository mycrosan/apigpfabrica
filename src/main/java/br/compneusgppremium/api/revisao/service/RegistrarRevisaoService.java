package br.compneusgppremium.api.revisao.service;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.leitura.service.*;
import br.compneusgppremium.api.revisao.dto.*;
import br.compneusgppremium.api.revisao.mapper.RevisaoMapper;
import br.compneusgppremium.api.revisao.repository.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrarRevisaoService {
    private final ItemRevisaoRepository itens;
    private final ImagemRevisaoRepository imagens;
    private final RespostaRevisaoRepository respostas;
    private final AcessoRevisaoService acesso;
    private final ConsultaRevisaoService consulta;
    private final RegrasRevisaoService regras;
    private final RevisaoMapper mapper;
    private final LeituraJson json;
    private final Clock relogio;
    @Transactional
    public ItemRevisaoDTO registrar(final String id, final UUID chave, final RevisarEntradaDTO entrada) {
        int revisor = acesso.revisor();
        var item = itens.bloquear(id).orElseThrow(ConsultaRevisaoService::ausente);
        String hash = ArquivosLeituraService.hash((id + json.escrever(entrada)).getBytes(StandardCharsets.UTF_8));
        var existente = respostas.findByRevisorIdAndChave(revisor, chave.toString());
        if (existente.isPresent()) {
            if (!hash.equals(existente.get().getHashRequisicao())) { throw conflito("Chave já usada com outra resposta."); }
            return consulta.projetar(item, revisor);
        }
        if (Objects.equals(item.getOperadorId(), revisor)) {
            throw new CadastroPneuException("AUTORREVISAO", "Outra pessoa deve revisar a sua captura ou confirmação.", HttpStatus.FORBIDDEN);
        }
        if (!Objects.equals(item.getVersao(), entrada.versao())) { throw conflito("A revisão mudou. Atualize a foto antes de continuar."); }
        if (item.isReavaliar() || !Set.of("PENDENTE", "DIVERGENTE").contains(item.getEstado())) {
            throw conflito("Este item não está disponível para nova resposta.");
        }
        if (respostas.existsByItemIdAndRevisorId(id, revisor)) { throw conflito("Sua resposta já foi salva. A conferência exige outra pessoa."); }
        var imagem = imagens.findById(item.getImagemId()).orElseThrow(ConsultaRevisaoService::ausente);
        boolean adjudicacao = "DIVERGENTE".equals(item.getEstado());
        regras.validar(entrada, imagem.getLargura(), imagem.getAltura(), adjudicacao);
        consulta.imagem(id); // Não aprovar evidência ausente ou cujo conteúdo foi alterado.
        var resposta = mapper.entrada(entrada); resposta.setId(UUID.randomUUID().toString());
        resposta.setItemId(id); resposta.setRevisorId(revisor); resposta.setChave(chave.toString());
        resposta.setHashRequisicao(hash); resposta.setCiclo(item.getCiclo()); resposta.setAdjudicacao(adjudicacao);
        resposta.setTranscricao(entrada.transcricao() == null || entrada.transcricao().isBlank() ? null : entrada.transcricao().strip());
        resposta.setRegiaoJson(json.escrever(entrada.regiao())); resposta.setCriadaEm(relogio.instant());
        respostas.saveAndFlush(resposta);
        var historico = respostas.findByItemIdAndCicloOrderByCriadaEmAsc(id, item.getCiclo());
        item.setEstado(regras.decidir(historico));
        if ("APROVADA".equals(item.getEstado()) && item.getValorCadastro() != null
                && Objects.equals(item.getCampoSolicitado(), resposta.getCampo())
                && !item.getValorCadastro().equalsIgnoreCase(resposta.getTranscricao())) { item.setPendenciaCadastro(true); }
        // A primeira resposta também precisa alterar a versão, mesmo permanecendo PENDENTE.
        item.setUltimaRespostaId(resposta.getId());
        itens.flush();
        log.info("Revisão registrada item={} resposta={} estado={}", id, resposta.getId(), item.getEstado());
        return consulta.projetar(item, revisor);
    }
    private CadastroPneuException conflito(final String mensagem) {
        return new CadastroPneuException("REVISAO_CONFLITO", mensagem, HttpStatus.CONFLICT);
    }
}
