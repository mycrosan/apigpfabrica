package br.compneusgppremium.api.revisao.service;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.leitura.service.*;
import br.compneusgppremium.api.revisao.dto.*;
import br.compneusgppremium.api.revisao.mapper.RevisaoMapper;
import br.compneusgppremium.api.revisao.model.*;
import br.compneusgppremium.api.revisao.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultaRevisaoService {
    private final ItemRevisaoRepository itens;
    private final ImagemRevisaoRepository imagens;
    private final RespostaRevisaoRepository respostas;
    private final AcessoRevisaoService acesso;
    private final ArquivoRevisaoService arquivos;
    private final RevisaoMapper mapper;
    private final LeituraJson json;
    public FilaRevisaoDTO fila(final int pagina, final String campo, final String estado, final Boolean correcao,
            final Boolean ambiguidade, final Integer marca, final Integer modelo, final Instant desde,
            final Instant ate, final boolean disponiveis) {
        int revisor = acesso.revisor();
        if (pagina < 0 || pagina > 100000) { throw new CadastroPneuException("PAGINA_INVALIDA", "Página inválida."); }
        Specification<ItemRevisao> filtros = (raiz, consulta, construtor) -> {
            var criterios = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (campo != null) { criterios.add(construtor.equal(raiz.get("campoSolicitado"), campo)); }
            if (estado != null) { criterios.add(construtor.equal(raiz.get("estado"), estado)); }
            if (correcao != null) { criterios.add(construtor.equal(raiz.get("correcao"), correcao)); }
            if (ambiguidade != null) { criterios.add(construtor.equal(raiz.get("ambiguidade"), ambiguidade)); }
            if (marca != null) { criterios.add(construtor.equal(raiz.get("marcaId"), marca)); }
            if (modelo != null) { criterios.add(construtor.equal(raiz.get("modeloId"), modelo)); }
            if (desde != null) { criterios.add(construtor.greaterThanOrEqualTo(raiz.get("criadaEm"), desde)); }
            if (ate != null) { criterios.add(construtor.lessThanOrEqualTo(raiz.get("criadaEm"), ate)); }
            if (disponiveis) {
                criterios.add(raiz.get("estado").in("PENDENTE", "DIVERGENTE"));
                criterios.add(construtor.isFalse(raiz.get("reavaliar")));
                criterios.add(construtor.or(construtor.isNull(raiz.get("operadorId")),
                        construtor.notEqual(raiz.get("operadorId"), revisor)));
                var anteriores = consulta.subquery(String.class); var resposta = anteriores.from(RespostaRevisao.class);
                anteriores.select(resposta.get("id")).where(construtor.equal(resposta.get("itemId"), raiz.get("id")),
                        construtor.equal(resposta.get("revisorId"), revisor));
                criterios.add(construtor.not(construtor.exists(anteriores)));
            }
            return construtor.and(criterios.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var encontrados = itens.findAll(filtros, PageRequest.of(pagina, 20, Sort.by("criadaEm", "id")));
        return new FilaRevisaoDTO(encontrados.stream().map(item -> projetar(item, revisor)).toList(), pagina,
                encontrados.getTotalElements(), encontrados.hasNext());
    }
    public ItemRevisaoDTO consultar(final String id) {
        int revisor = acesso.revisor(); return projetar(buscar(id), revisor);
    }
    public byte[] imagem(final String id) {
        acesso.revisor(); var imagem = imagens.findById(buscar(id).getImagemId()).orElseThrow(ConsultaRevisaoService::ausente);
        byte[] bytes;
        try {
            bytes = arquivos.ler(imagem.getPrevia());
            if (!imagem.getSha256().equals(ArquivosLeituraService.hash(arquivos.ler(imagem.getArquivo())))) {
                throw new IllegalStateException("Hash da evidência original divergente");
            }
        } catch (IllegalStateException erro) {
            throw new CadastroPneuException("EVIDENCIA_INCONSISTENTE", "Foto indisponível para revisão.", HttpStatus.CONFLICT);
        }
        if (!imagem.getHashPrevia().equals(ArquivosLeituraService.hash(bytes))) {
            throw new CadastroPneuException("EVIDENCIA_INCONSISTENTE", "Foto indisponível para revisão.", HttpStatus.CONFLICT);
        }
        return bytes;
    }
    ItemRevisaoDTO projetar(final ItemRevisao item, final int revisor) {
        var imagem = imagens.findById(item.getImagemId()).orElseThrow(ConsultaRevisaoService::ausente);
        var historico = respostas.findByItemIdAndCicloOrderByCriadaEmAsc(item.getId(), item.getCiclo());
        var minha = historico.stream().filter(resposta -> resposta.getRevisorId().equals(revisor)).findFirst();
        // A API omite a evidência textual, inclusive a resposta de outros revisores, antes do envio independente.
        ItemRevisaoDTO.ComparacaoDTO comparacao = null;
        if (minha.isPresent()) {
            var evidencia = json.ler(item.getEvidenciaJson(), EvidenciaRevisaoDTO.class);
            String original = evidencia.extracao() == null || evidencia.extracao().linhas().isEmpty() ? null
                    : evidencia.extracao().linhas().get(evidencia.indiceLinha()).texto();
            String sugestao = evidencia.execucao() == null ? null : evidencia.execucao().resultado().valorSugerido();
            String versao = evidencia.extracao() == null ? null : evidencia.extracao().versaoModelo();
            comparacao = new ItemRevisaoDTO.ComparacaoDTO(original, sugestao, item.getValorCadastro(), versao,
                    historico.stream().map(mapper::resposta).toList());
        }
        boolean pode = !Objects.equals(item.getOperadorId(), revisor) && !item.isReavaliar()
                && Set.of("PENDENTE", "DIVERGENTE").contains(item.getEstado())
                && !respostas.existsByItemIdAndRevisorId(item.getId(), revisor);
        String etapa = "DIVERGENTE".equals(item.getEstado()) ? "ADJUDICACAO" : historico.isEmpty() ? "PRIMEIRA" : "SEGUNDA";
        return new ItemRevisaoDTO(item.getId(), item.getCampoSolicitado(), item.getOrigem(), item.getEstado(),
                item.getVersao(), item.getCriadaEm(), item.isReavaliar(), item.isPendenciaCadastro(), pode, etapa,
                imagem.getLargura(), imagem.getAltura(), json.ler(item.getRegiaoJson(), new TypeReference<>() {}),
                minha.map(mapper::resposta).orElse(null), comparacao);
    }
    private ItemRevisao buscar(final String id) { return itens.findById(id).orElseThrow(ConsultaRevisaoService::ausente); }
    static CadastroPneuException ausente() {
        return new CadastroPneuException("REVISAO_AUSENTE", "Item de revisão não encontrado.", HttpStatus.NOT_FOUND);
    }
}
