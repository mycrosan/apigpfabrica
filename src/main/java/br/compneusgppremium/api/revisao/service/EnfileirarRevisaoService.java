package br.compneusgppremium.api.revisao.service;
import br.compneusgppremium.api.exception.CadastroPneuException;
import br.compneusgppremium.api.leitura.dto.ExecucaoLeituraDTO;
import br.compneusgppremium.api.leitura.model.*;
import br.compneusgppremium.api.leitura.repository.*;
import br.compneusgppremium.api.leitura.service.*;
import br.compneusgppremium.api.ocr.OcrResposta;
import br.compneusgppremium.api.revisao.dto.*;
import br.compneusgppremium.api.revisao.model.*;
import br.compneusgppremium.api.revisao.repository.*;
import br.compneusgppremium.api.service.ImagemLeituraService;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
@RequiredArgsConstructor
@Slf4j
public class EnfileirarRevisaoService {
    private final ItemRevisaoRepository itens;
    private final ImagemRevisaoRepository imagens;
    private final ImagemLeituraRepository imagensLeitura;
    private final SessaoLeituraRepository sessoes;
    private final TentativaLeituraRepository tentativas;
    private final ExecucaoLeituraRepository execucoes;
    private final ConfirmacaoLeituraRepository confirmacoes;
    private final ArquivosLeituraService arquivosLeitura;
    private final ArquivoRevisaoService arquivos;
    private final PreviaRevisaoService previas;
    private final ImagemLeituraService validacao;
    private final AcessoRevisaoService acesso;
    private final LeituraJson json;
    private final Clock relogio;
    @Transactional
    public List<String> importar(final ImportarRevisaoDTO entrada) {
        acesso.importar();
        var bytes = validacao.validar(entrada.fotoBase64());
        if (!ArquivosLeituraService.hash(bytes).equals(entrada.imagemSha256())) {
            throw new CadastroPneuException("HASH_DIVERGENTE", "Foto diferente da evidência do manifesto.");
        }
        var imagem = arquivar(bytes);
        validarExtracao(entrada.extracao(), imagem);
        return enfileirar(imagem, entrada.extracao(), null, null, null, entrada.manifestoSha256());
    }
    @Transactional
    public void tentativa(final String identificador) {
        var tentativa = tentativas.findById(identificador).orElseThrow();
        var sessao = sessoes.bloquear(tentativa.getSessaoId()).orElseThrow();
        var execucao = execucoes.findById(identificador).map(e -> json.ler(e.getSnapshotJson(), ExecucaoLeituraDTO.class)).orElse(null);
        var captura = imagensLeitura.findById(tentativa.getImagemId()).orElseThrow();
        var bytes = arquivosLeitura.ler(captura.getArquivo());
        if (!captura.getSha256().equals(ArquivosLeituraService.hash(bytes))) {
            throw new CadastroPneuException("HASH_DIVERGENTE", "Evidência de captura inconsistente.");
        }
        var imagem = arquivar(bytes);
        var extracao = execucao == null ? null : execucao.extracao();
        enfileirar(imagem, extracao, execucao, tentativa, sessao, null);
    }
    private ImagemRevisao arquivar(final byte[] bytes) {
        String hash = ArquivosLeituraService.hash(bytes);
        var existente = imagens.findBySha256(hash);
        if (existente.isPresent()) { return existente.get(); }
        var previa = previas.preparar(bytes);
        var imagem = new ImagemRevisao(); imagem.setId(UUID.randomUUID().toString()); imagem.setSha256(hash);
        imagem.setArquivo(arquivos.salvar(bytes)); imagem.setPrevia(arquivos.salvar(previa.bytes()));
        imagem.setHashPrevia(ArquivosLeituraService.hash(previa.bytes()));
        imagem.setLargura(previa.largura()); imagem.setAltura(previa.altura()); imagem.setCriadaEm(relogio.instant());
        return imagens.saveAndFlush(imagem);
    }
    private List<String> enfileirar(final ImagemRevisao imagem, final OcrResposta extracao,
            final ExecucaoLeituraDTO execucao, final TentativaLeitura tentativa, final SessaoLeitura sessao,
            final String manifesto) {
        int quantidade = extracao == null ? 0 : extracao.linhas().size();
        var identificadores = new ArrayList<String>();
        for (int indice = 0; indice < Math.max(1, quantidade); indice++) {
            String fonte = tentativa == null ? manifesto + ":" + imagem.getSha256() : tentativa.getId();
            String chave = ArquivosLeituraService.hash((fonte + ":" + indice).getBytes(StandardCharsets.UTF_8));
            var existente = itens.findByChaveOrigem(chave);
            if (existente.isPresent()) { identificadores.add(existente.get().getId()); continue; }
            var item = new ItemRevisao(); item.setId(UUID.randomUUID().toString()); item.setChaveOrigem(chave);
            item.setImagemId(imagem.getId()); item.setOrigem(tentativa == null ? "LEGADO_NAO_REVISADO" : "CAPTURA_APLICATIVO");
            item.setEstado("PENDENTE"); item.setCriadaEm(relogio.instant());
            item.setRegiaoJson(json.escrever(quantidade == 0 ? List.of() : extracao.linhas().get(indice).regiao()));
            item.setEvidenciaJson(json.escrever(new EvidenciaRevisaoDTO(extracao, execucao, indice, manifesto)));
            if (tentativa != null) {
                item.setSessaoId(sessao.getId()); item.setTentativaId(tentativa.getId());
                item.setOperadorId(sessao.getOperadorId()); item.setCampoSolicitado(tentativa.getCampo());
                item.setAmbiguidade("AMBIGUA".equals(tentativa.getEstado()));
                Integer[] contexto = json.ler(tentativa.getContextoJson(), Integer[].class);
                item.setMarcaId(contexto[0]); item.setModeloId(contexto[1]);
                confirmacoes.findFirstByTentativaIdOrderByCriadaEmDesc(tentativa.getId()).ifPresent(confirmacao -> {
                    item.setConfirmacaoId(confirmacao.getId()); item.setValorCadastro(confirmacao.getValorFinal());
                    item.setCorrecao(!"IA_CONFIRMADA".equals(confirmacao.getOrigem()));
                });
            }
            identificadores.add(itens.save(item).getId());
        }
        log.info("Evidências disponíveis para revisão quantidade={}", identificadores.size());
        return List.copyOf(identificadores);
    }
    private void validarExtracao(final OcrResposta extracao, final ImagemRevisao imagem) {
        if (extracao.linhas().size() > 1000 || extracao.versaoModelo() == null || extracao.versaoModelo().isBlank()) {
            throw new CadastroPneuException("EXTRACAO_INVALIDA", "Extração sem versão ou acima do limite.");
        }
        for (var linha : extracao.linhas()) {
            if (linha.texto() == null || linha.texto().length() > 4096) {
                throw new CadastroPneuException("EXTRACAO_INVALIDA", "Linha de OCR inválida.");
            }
            RegrasRevisaoService.validarRegiao(linha.regiao(), imagem.getLargura(), imagem.getAltura());
        }
    }
}
